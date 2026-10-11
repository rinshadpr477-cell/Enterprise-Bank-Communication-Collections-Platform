package com.ford.bank.campaign.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Campaign target represents a single customer targeted by a campaign.
 * Unique constraint (campaign_id, customer_id) prevents duplicates.
 */
@Entity
@Table(name = "campaign_targets",
       uniqueConstraints = @UniqueConstraint(name = "uq_campaign_customer",
               columnNames = {"campaign_id", "customer_id"}))
@Getter
@Setter
@NoArgsConstructor
public class CampaignTarget {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CampaignTargetStatus status = CampaignTargetStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public CampaignTarget(UUID campaignId, UUID customerId) {
        this.campaignId = campaignId;
        this.customerId = customerId;
    }
}
