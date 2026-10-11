package com.ford.bank.communication.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Records each individual communication attempt separately.
 * Historical records are never overwritten - this is an append-only log.
 */
@Entity
@Table(name = "communication_attempts")
@Getter
@Setter
@NoArgsConstructor
public class CommunicationAttempt {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_reference", length = 255)
    private String providerReference;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AttemptStatus status;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "raw_response")
    private String rawResponse;

    @PrePersist
    protected void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = OffsetDateTime.now();
        }
    }

    public CommunicationAttempt(UUID jobId, String provider, int attemptNumber) {
        this.jobId = jobId;
        this.provider = provider;
        this.attemptNumber = attemptNumber;
        this.status = AttemptStatus.IN_PROGRESS;
        this.startedAt = OffsetDateTime.now();
    }
}
