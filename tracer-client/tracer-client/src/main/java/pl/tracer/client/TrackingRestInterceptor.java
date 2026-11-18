package pl.tracer.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import pl.tracer.client.EventSender.Event;

/** Wychodzące calle (RestClient / RestTemplate): dopina nagłówki śledzenia i raportuje request + response (side=CLIENT). */
public class TrackingRestInterceptor implements ClientHttpRequestInterceptor {

    private final TracerProperties props;
    private final EventSender sender;
    private final String self;

    public TrackingRestInterceptor(TracerProperties props, EventSender sender, String self) {
        this.props = props;
        this.sender = sender;
        this.self = self;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        String uuid = MDC.get(TrackingFilter.MDC_KEY);
        if (uuid == null) uuid = UUID.randomUUID().toString(); // call spoza requestu (np. scheduler)
        String span = UUID.randomUUID().toString();            // łączy zdarzenia klienta i serwera
        request.getHeaders().set(TrackingFilter.HEADER, uuid);
        request.getHeaders().set(TrackingFilter.SPAN_HEADER, span);
        request.getHeaders().set(TrackingFilter.CALLER_HEADER, self);

        URI uri = request.getURI();
        String to = resolve(uri);
        String path = uri.getRawPath() + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
        int max = props.getMaxPayloadBytes();

        sender.send(new Event(uuid, span, "CLIENT", Instant.now().toString(), "REST_REQUEST", self, to,
                request.getMethod().name(), path, null, headers(request.getHeaders()),
                Payloads.text(str(request.getHeaders()), body, max)));

        ClientHttpResponse response;
        try {
            response = execution.execute(request, body);
        } catch (IOException e) {
            sender.send(new Event(uuid, span, "CLIENT", Instant.now().toString(), "REST_RESPONSE", to, self,
                    null, null, 0, List.of(), e.toString()));
            throw e;
        }
        byte[] respBody = response.getBody().readAllBytes(); // buforujemy, żeby klient dalej mógł czytać
        sender.send(new Event(uuid, span, "CLIENT", Instant.now().toString(), "REST_RESPONSE", to, self, null, null,
                response.getStatusCode().value(), headers(response.getHeaders()),
                Payloads.text(str(response.getHeaders()), respBody, max)));
        return new BufferedResponse(response, respBody);
    }

    private String resolve(URI uri) {
        String host = uri.getHost();
        String withPort = uri.getPort() > 0 ? host + ":" + uri.getPort() : host;
        return props.getHosts().getOrDefault(withPort, props.getHosts().getOrDefault(host, withPort));
    }

    private List<EventSender.Header> headers(HttpHeaders h) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        h.forEach(map::put);
        return Payloads.headers(map, props.getMaskHeaders());
    }

    private static String str(HttpHeaders h) {
        return h.getContentType() != null ? h.getContentType().toString() : null;
    }

    private static final class BufferedResponse implements ClientHttpResponse {
        private final ClientHttpResponse delegate;
        private final byte[] body;

        BufferedResponse(ClientHttpResponse delegate, byte[] body) {
            this.delegate = delegate;
            this.body = body;
        }

        @Override public HttpStatusCode getStatusCode() throws IOException { return delegate.getStatusCode(); }
        @Override public String getStatusText() throws IOException { return delegate.getStatusText(); }
        @Override public HttpHeaders getHeaders() { return delegate.getHeaders(); }
        @Override public InputStream getBody() { return new ByteArrayInputStream(body); }
        @Override public void close() { delegate.close(); }
    }
}
