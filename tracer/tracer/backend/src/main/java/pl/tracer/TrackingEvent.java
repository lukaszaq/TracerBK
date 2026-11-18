package pl.tracer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Jedno zdarzenie raportowane przez mikroserwis.
 * REST: ten sam call jest raportowany przez klienta (side=CLIENT) i serwer (side=SERVER) z tym samym spanId;
 *       REST_REQUEST (from=caller,to=callee) i REST_RESPONSE (from=callee,to=caller).
 * Kafka: KAFKA_PRODUCE (from=serwis,to="topic:NAZWA") oraz KAFKA_CONSUME (from="topic:NAZWA",to=serwis).
 */
@Document(indexName = "tracking-events")
public record TrackingEvent(
        @Id String id,
        @NotBlank @Field(type = FieldType.Keyword) String trackingUuid,
        @Field(type = FieldType.Keyword) String spanId,
        @Field(type = FieldType.Keyword) String side,        // CLIENT | SERVER (null = CLIENT)
        @NotNull @Field(type = FieldType.Date, format = DateFormat.date_time) Instant timestamp,
        @NotNull @Field(type = FieldType.Keyword) EventType type,
        @NotBlank @Field(type = FieldType.Keyword) String from,
        @NotBlank @Field(type = FieldType.Keyword) String to,
        @Field(type = FieldType.Keyword) String method,
        @Field(type = FieldType.Keyword) String uri,
        @Field(type = FieldType.Integer) Integer status,
        @Field(type = FieldType.Keyword) String topic,
        @Field(type = FieldType.Keyword) String messageKey,
        List<Header> headers,
        @Field(type = FieldType.Text, index = false) String payload) {

    public enum EventType { REST_REQUEST, REST_RESPONSE, KAFKA_PRODUCE, KAFKA_CONSUME }

    public record Header(String name, String value) {}

    TrackingEvent normalized() {
        return new TrackingEvent(UUID.randomUUID().toString(), trackingUuid, spanId, side,
                timestamp.truncatedTo(ChronoUnit.MILLIS), type, from, to, method, uri, status,
                topic, messageKey, headers, payload);
    }
}
