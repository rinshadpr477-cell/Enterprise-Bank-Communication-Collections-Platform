package com.ford.bank.kafka.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Standard event envelope for all Kafka events.
 * This envelope is published with every event and should NOT contain customer PII.
 *
 * eventId: globally unique ID for this event (used for consumer deduplication)
 * eventType: discriminator for routing/filtering
 * schemaVersion: schema version for forward compatibility
 * occurredAt: when the business event occurred (not when it was published)
 * correlationId: request correlation ID for distributed tracing
 * causationId: the event ID that caused this event (event sourcing chain)
 * aggregateId: the domain aggregate this event belongs to
 * aggregateType: type name of the aggregate
 * data: event-specific payload
 */
public record EventEnvelope(
        String eventId,
        String eventType,
        int schemaVersion,
        OffsetDateTime occurredAt,
        String correlationId,
        String causationId,
        String aggregateId,
        String aggregateType,
        Object data
) {
    public static EventEnvelope of(String eventType, String aggregateType, String aggregateId,
                                    String correlationId, Object data) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                1,
                OffsetDateTime.now(),
                correlationId,
                null,
                aggregateId,
                aggregateType,
                data
        );
    }

    public static EventEnvelope withCausation(String eventType, String aggregateType, String aggregateId,
                                               String correlationId, String causationId, Object data) {
        return new EventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                1,
                OffsetDateTime.now(),
                correlationId,
                causationId,
                aggregateId,
                aggregateType,
                data
        );
    }
}
