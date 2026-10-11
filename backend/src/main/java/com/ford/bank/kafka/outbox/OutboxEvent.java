package com.ford.bank.kafka.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Outbox event record.
 *
 * Design notes on the crash window:
 * After Kafka broker acknowledges the message, we update published_at.
 * If the JVM crashes between Kafka ack and the DB update, the event will
 * be republished on the next poll. Consumers MUST be idempotent via
 * the processed_events table.
 *
 * Locking strategy:
 * The publisher locks records by setting locked_by = instanceId and locked_at = now().
 * If locked_at is older than lock_timeout, another instance can reclaim the record.
 * Multiple publisher instances thus cannot claim the same record concurrently.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "publish_attempts", nullable = false)
    private int publishAttempts = 0;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public void markPublished() {
        this.publishedAt = OffsetDateTime.now();
        this.lockedBy = null;
        this.lockedAt = null;
    }

    public void incrementAttempts() {
        this.publishAttempts++;
    }
}
