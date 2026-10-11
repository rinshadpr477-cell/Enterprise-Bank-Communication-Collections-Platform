package com.ford.bank.campaign.repository;

import com.ford.bank.campaign.domain.Campaign;
import com.ford.bank.campaign.domain.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {

    Page<Campaign> findByStatus(CampaignStatus status, Pageable pageable);

    @Query("""
        SELECT c FROM Campaign c
        WHERE c.status = 'SCHEDULED'
        AND c.scheduledAt <= :now
        ORDER BY c.scheduledAt ASC
        """)
    List<Campaign> findDueScheduledCampaigns(@Param("now") OffsetDateTime now);

    @Query("""
        SELECT c FROM Campaign c
        WHERE (:status IS NULL OR c.status = :status)
        AND (:type IS NULL OR c.campaignType = :type)
        """)
    Page<Campaign> findWithFilters(
            @Param("status") CampaignStatus status,
            @Param("type") String type,
            Pageable pageable
    );
}
