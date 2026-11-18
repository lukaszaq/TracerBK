package pl.tracer.client;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/** Asynchroniczne, nieblokujące wysyłanie zdarzeń do kolektora (przy przepełnieniu kolejki zdarzenia są gubione). */
public class EventSender {
    private static final Logger log = LoggerFactory.getLogger(EventSender.class);

    public record Header(String name, String value) {}
    public record Event(String trackingUuid, String spanId, String side, String timestamp, String type, String from, String to,
                        String method, String uri, Integer status, List<Header> headers, String payload) {}

    private final RestClient http; // celowo "gołego" buildera: bez naszego interceptora
    private final ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10_000), r -> {
                Thread t = new Thread(r, "tracer-sender");
                t.setDaemon(true);
                return t;
            }, new ThreadPoolExecutor.DiscardPolicy());

    public EventSender(String collectorUrl) {
        this.http = RestClient.builder().baseUrl(collectorUrl).build();
    }

    public void send(Event e) {
        pool.execute(() -> {
            try {
                http.post().uri("/api/events").contentType(MediaType.APPLICATION_JSON)
                        .body(List.of(e)).retrieve().toBodilessEntity();
            } catch (Exception ex) {
                log.debug("Tracer: nie udało się wysłać zdarzenia: {}", ex.toString());
            }
        });
    }
}
