package pl.tracer;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class TraceService {

    public record Participant(String name, String displayName, String color) {}
    public record MessageView(String id, Instant timestamp, TrackingEvent.EventType type,
                              String from, String to, String label, Integer status) {}
    public record TraceView(String trackingUuid, List<Participant> participants, List<MessageView> messages) {}

    private final EventRepository events;
    private final ServiceConfigRepository configs;

    public TraceService(EventRepository events, ServiceConfigRepository configs) {
        this.events = events;
        this.configs = configs;
    }

    public TraceView build(String trackingUuid) {
        // kolejność = czas zdarzenia; uwaga na rozjazd zegarów między hostami (NTP)
        List<TrackingEvent> list = events.findByTrackingUuid(trackingUuid,
                PageRequest.of(0, 2000, Sort.by("timestamp").ascending()));

        // uczestnicy w kolejności pierwszego pojawienia się w wywołaniach
        Set<String> names = new LinkedHashSet<>();
        for (TrackingEvent e : list) { names.add(e.from()); names.add(e.to()); }

        Map<String, ServiceConfig> cfg = configs.findAllById(names).stream()
                .collect(Collectors.toMap(ServiceConfig::getName, c -> c));
        List<Participant> participants = names.stream().map(n -> {
            ServiceConfig c = cfg.get(n);
            return new Participant(n,
                    c != null && c.getDisplayName() != null ? c.getDisplayName() : n,
                    c != null ? c.getColor() : null);
        }).toList();

        List<MessageView> messages = list.stream().map(e -> new MessageView(
                e.id(), e.timestamp(), e.type(), e.from(), e.to(), label(e), e.status())).toList();
        return new TraceView(trackingUuid, participants, messages);
    }

    private static String label(TrackingEvent e) {
        return switch (e.type()) {
            case REST_REQUEST -> e.method() + " " + e.uri();
            case REST_RESPONSE -> String.valueOf(e.status());
            case KAFKA_PRODUCE -> "produce" + (e.messageKey() != null ? " [" + e.messageKey() + "]" : "");
            case KAFKA_CONSUME -> "consume" + (e.messageKey() != null ? " [" + e.messageKey() + "]" : "");
        };
    }
}
