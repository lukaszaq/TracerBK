package pl.tracer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Jedno zdarzenie raportowane przez mikroserwis.
 * REST:  REST_REQUEST (from=caller,to=callee) i opcjonalnie REST_RESPONSE (from=callee,to=caller).
 * Kafka: KAFKA_PRODUCE (from=serwis,to="topic:NAZWA") oraz KAFKA_CONSUME (from="topic:NAZWA",to=serwis).
 */
@Document(indexName = "tracking-events")
public record TrackingEvent(
        @Id String id,
        @NotBlank @Field(type = FieldType.Keyword) String trackingUuid,
        @NotNull @Field(type = FieldType.Date, format = DateFormat.date_time) Instant timestamp,
        @NotNull @Field(type = FieldType.Keyword) EventType type,
        @NotBlank @Field(type = FieldType.Keyword) String from,
        @NotBlank @Field(type = FieldType.Keyword) String to,
        @Field(type = FieldType.Keyword) String method,      // REST
        @Field(type = FieldType.Keyword) String uri,         // REST
        @Field(type = FieldType.Integer) Integer status,     // REST_RESPONSE
        @Field(type = FieldType.Keyword) String topic,       // Kafka
        @Field(type = FieldType.Keyword) String messageKey,  // Kafka
        List<Header> headers,
        @Field(type = FieldType.Text, index = false) String payload) {

    public enum EventType { REST_REQUEST, REST_RESPONSE, KAFKA_PRODUCE, KAFKA_CONSUME }

    public record Header(String name, String value) {}

    TrackingEvent withId(String newId) {
        return new TrackingEvent(newId, trackingUuid, timestamp, type, from, to, method, uri, status,
                topic, messageKey, headers, payload);
    }
}
