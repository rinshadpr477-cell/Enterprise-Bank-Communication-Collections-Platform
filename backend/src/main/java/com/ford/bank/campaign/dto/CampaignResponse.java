package com.ford.bank.campaign.dto;

import com.ford.bank.campaign.domain.CampaignStatus;
import com.ford.bank.campaign.domain.CampaignType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String name,
        CampaignType campaignType,
        CampaignStatus status,
        String description,
        OffsetDateTime scheduledAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        long pendingTargets,
        long completedTargets,
        long totalTargets
) {}
