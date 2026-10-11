package com.ford.bank.campaign.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "campaign_type", nullable = false, length = 50)
    private CampaignType campaignType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Column(name = "description")
    private String description;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

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

    /**
     * Enforce state machine transitions.
     * Valid transitions:
     *   DRAFT -> SCHEDULED
     *   SCHEDULED -> RUNNING | CANCELLED
     *   RUNNING -> PAUSED | COMPLETED | CANCELLED
     *   PAUSED -> RUNNING | CANCELLED
     *   COMPLETED -> (terminal)
     *   CANCELLED -> (terminal)
     */
    public void transitionTo(CampaignStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new com.ford.bank.common.exception.InvalidStateTransitionException(
                    "Cannot transition campaign from " + status + " to " + newStatus
            );
        }
        this.status = newStatus;
        if (newStatus == CampaignStatus.RUNNING && startedAt == null) {
            this.startedAt = OffsetDateTime.now();
        }
        if (newStatus == CampaignStatus.COMPLETED) {
            this.completedAt = OffsetDateTime.now();
        }
    }

    public boolean isActive() {
        return status == CampaignStatus.RUNNING || status == CampaignStatus.SCHEDULED;
    }
}
