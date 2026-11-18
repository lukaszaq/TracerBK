package pl.tracer;

import jakarta.validation.Valid;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final EventRepository events;
    private final TraceService traces;
    private final ServiceConfigRepository configs;

    public ApiController(EventRepository events, TraceService traces, ServiceConfigRepository configs) {
        this.events = events;
        this.traces = traces;
        this.configs = configs;
    }

    /** Kolektor: mikroserwisy wysyłają tu zdarzenia (asynchronicznie, fire-and-forget). */
    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@RequestBody List<@Valid TrackingEvent> batch) {
        events.saveAll(batch.stream().map(TrackingEvent::normalized).toList());
    }

    @GetMapping("/traces/{trackingUuid}")
    public TraceService.TraceView trace(@PathVariable String trackingUuid) {
        return traces.build(trackingUuid);
    }

    @GetMapping("/events/{id}")
    public TrackingEvent event(@PathVariable String id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/services")
    public List<ServiceConfig> services() { return configs.findAll(); }

    @PutMapping("/services/{name}")
    public ServiceConfig saveService(@PathVariable String name, @RequestBody ServiceConfig body) {
        body.setName(name);
        return configs.save(body);
    }
}
