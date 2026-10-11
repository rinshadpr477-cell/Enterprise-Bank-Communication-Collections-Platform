package com.ford.bank.communication.domain;

import com.ford.bank.customer.domain.CommunicationChannel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "communication_jobs")
@Getter
@Setter
@NoArgsConstructor
public class CommunicationJob {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private CommunicationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private JobStatus status = JobStatus.PENDING;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "claimed_by", length = 100)
    private String claimedBy;

    @Column(name = "claimed_at")
    private OffsetDateTime claimedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public boolean canRetry() {
        return attemptCount < maxAttempts && status == JobStatus.FAILED;
    }

    public boolean isTerminal() {
        return status == JobStatus.COMPLETED
                || status == JobStatus.CANCELLED
                || status == JobStatus.SUPPRESSED;
    }

    /**
     * Calculate next retry time using exponential backoff with jitter.
     * Base: 60s, multiplier: 2^attempt, max: 1 hour.
     * Jitter: ±10% to prevent thundering herd.
     */
    public OffsetDateTime calculateNextRetryAt() {
        long baseSeconds = 60L;
        long backoffSeconds = Math.min(baseSeconds * (1L << attemptCount), 3600L);
        double jitter = 1.0 + (Math.random() * 0.2 - 0.1);
        long delaySeconds = (long) (backoffSeconds * jitter);
        return OffsetDateTime.now().plusSeconds(delaySeconds);
    }
}
