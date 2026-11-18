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
    /** peerId = zdarzenie z drugiej strony calla (serwer), jeśli oba końce raportowały. */
    public record MessageView(String id, String peerId, Instant timestamp, TrackingEvent.EventType type,
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

        // Ten sam call raportuje klient i serwer (wspólny spanId) -> na diagramie jedna strzałka.
        Set<String> clientKeys = list.stream().filter(e -> !isServer(e) && key(e) != null)
                .map(TraceService::key).collect(Collectors.toSet());
        Map<String, TrackingEvent> serverByKey = new HashMap<>();
        list.stream().filter(e -> isServer(e) && key(e) != null).forEach(e -> serverByKey.putIfAbsent(key(e), e));

        List<MessageView> messages = new ArrayList<>();
        for (TrackingEvent e : list) {
            String k = key(e);
            if (isServer(e) && k != null && clientKeys.contains(k)) continue; // pokryte przez zdarzenie klienta
            TrackingEvent peer = (!isServer(e) && k != null) ? serverByKey.get(k) : null;
            String from = e.from(), to = e.to();
            // prawdziwa nazwa wywoływanego serwisu pochodzi z jego własnego raportu (nie trzeba mapować hostów)
            if (peer != null) {
                if (e.type() == TrackingEvent.EventType.REST_REQUEST) to = peer.to();
                else if (e.type() == TrackingEvent.EventType.REST_RESPONSE) from = peer.from();
            }
            messages.add(new MessageView(e.id(), peer != null ? peer.id() : null, e.timestamp(), e.type(),
                    from, to, label(e), e.status()));
        }

        Set<String> names = new LinkedHashSet<>(); // kolejność pierwszego pojawienia się
        for (MessageView m : messages) { names.add(m.from()); names.add(m.to()); }
        Map<String, ServiceConfig> cfg = configs.findAllById(names).stream()
                .collect(Collectors.toMap(ServiceConfig::getName, c -> c));
        List<Participant> participants = names.stream().map(n -> {
            ServiceConfig c = cfg.get(n);
            return new Participant(n,
                    c != null && c.getDisplayName() != null ? c.getDisplayName() : n,
                    c != null ? c.getColor() : null);
        }).toList();
        return new TraceView(trackingUuid, participants, messages);
    }

    private static boolean isServer(TrackingEvent e) { return "SERVER".equals(e.side()); }

    private static String key(TrackingEvent e) { return e.spanId() == null ? null : e.spanId() + "|" + e.type(); }

    private static String label(TrackingEvent e) {
        return switch (e.type()) {
            case REST_REQUEST -> e.method() + " " + e.uri();
            case REST_RESPONSE -> String.valueOf(e.status());
            case KAFKA_PRODUCE -> "produce" + (e.messageKey() != null ? " [" + e.messageKey() + "]" : "");
            case KAFKA_CONSUME -> "consume" + (e.messageKey() != null ? " [" + e.messageKey() + "]" : "");
        };
    }
}
