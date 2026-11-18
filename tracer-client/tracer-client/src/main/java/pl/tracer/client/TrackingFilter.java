package pl.tracer.client;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import pl.tracer.client.EventSender.Event;

/**
 * Przychodzące calle: odbiera/generuje X-Tracking-UUID (do MDC, żeby wychodzące calle go propagowały)
 * i raportuje request + response (side=SERVER). Nazwę wołającego bierze z X-Tracking-Caller ("external" gdy brak).
 */
public class TrackingFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Tracking-UUID";
    public static final String SPAN_HEADER = "X-Tracking-Span";
    public static final String CALLER_HEADER = "X-Tracking-Caller";
    public static final String MDC_KEY = "trackingUuid";

    private final TracerProperties props;
    private final EventSender sender;
    private final String self;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public TrackingFilter(TracerProperties props, EventSender sender, String self) {
        this.props = props;
        this.sender = sender;
        this.self = self;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String uuid = req.getHeader(HEADER);
        if (uuid == null || uuid.isBlank()) uuid = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, uuid);
        res.setHeader(HEADER, uuid);

        String path = req.getRequestURI();
        boolean skip = props.getExcludePaths().stream().anyMatch(p -> matcher.match(p, path))
                || "text/event-stream".equals(req.getHeader("Accept"));
        if (skip) {
            try { chain.doFilter(req, res); } finally { MDC.remove(MDC_KEY); }
            return;
        }

        String caller = req.getHeader(CALLER_HEADER);
        if (caller == null || caller.isBlank()) caller = "external";
        String span = req.getHeader(SPAN_HEADER);
        if (span == null || span.isBlank()) span = UUID.randomUUID().toString();
        int max = props.getMaxPayloadBytes();
        Instant start = Instant.now();

        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(req, max);
        ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(res);
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            failed = true;
            throw e;
        } finally {
            Instant end = Instant.now();
            int status = failed ? 500 : response.getStatus();
            String uri = req.getRequestURI() + (req.getQueryString() != null ? "?" + req.getQueryString() : "");
            sender.send(new Event(uuid, span, "SERVER", start.toString(), "REST_REQUEST", caller, self,
                    req.getMethod(), uri, null, Payloads.headers(requestHeaders(req), props.getMaskHeaders()),
                    Payloads.text(req.getContentType(), request.getContentAsByteArray(), max)));
            sender.send(new Event(uuid, span, "SERVER", end.toString(), "REST_RESPONSE", self, caller,
                    null, null, status, Payloads.headers(responseHeaders(response), props.getMaskHeaders()),
                    Payloads.text(response.getContentType(), response.getContentAsByteArray(), max)));
            response.copyBodyToResponse();
            MDC.remove(MDC_KEY);
        }
    }

    private static Map<String, List<String>> requestHeaders(HttpServletRequest r) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String n : Collections.list(r.getHeaderNames())) m.put(n, Collections.list(r.getHeaders(n)));
        return m;
    }

    private static Map<String, List<String>> responseHeaders(HttpServletResponse r) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String n : r.getHeaderNames()) m.put(n, List.copyOf(r.getHeaders(n)));
        return m;
    }
}
