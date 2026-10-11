package com.ford.bank.kafka.consumer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Tracks events processed by each consumer to prevent duplicate processing.
 * Composite primary key (event_id, consumer_name) allows the same event
 * to be processed by different consumer groups independently.
 *
 * Usage: Before processing any Kafka event, check if (eventId, consumerName)
 * already exists. If so, skip processing (idempotent handling).
 * Insert this record in the same transaction as the business processing.
 */
@Entity
@Table(name = "processed_events")
@Getter
@Setter
@NoArgsConstructor
@IdClass(ProcessedEventId.class)
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Id
    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.processedAt = OffsetDateTime.now();
    }

    public ProcessedEvent(String eventId, String consumerName) {
        this.eventId = eventId;
        this.consumerName = consumerName;
    }
}
