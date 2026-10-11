package com.ford.bank.campaign.repository;

import com.ford.bank.campaign.domain.CampaignTarget;
import com.ford.bank.campaign.domain.CampaignTargetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignTargetRepository extends JpaRepository<CampaignTarget, UUID> {

    boolean existsByCampaignIdAndCustomerId(UUID campaignId, UUID customerId);

    Optional<CampaignTarget> findByCampaignIdAndCustomerId(UUID campaignId, UUID customerId);

    long countByCampaignIdAndStatus(UUID campaignId, CampaignTargetStatus status);

    Page<CampaignTarget> findByCampaignId(UUID campaignId, Pageable pageable);

    /**
     * Fetch a bounded batch of pending targets for a campaign.
     * Ordered by createdAt for deterministic processing order.
     */
    @Query(value = """
        SELECT * FROM campaign_targets
        WHERE campaign_id = :campaignId
        AND status = 'PENDING'
        ORDER BY created_at ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<CampaignTarget> findPendingBatch(@Param("campaignId") UUID campaignId, @Param("limit") int limit);
}
