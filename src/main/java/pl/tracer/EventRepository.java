package pl.tracer;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EventRepository extends ElasticsearchRepository<TrackingEvent, String> {
    List<TrackingEvent> findByTrackingUuid(String trackingUuid, Pageable pageable);
}
