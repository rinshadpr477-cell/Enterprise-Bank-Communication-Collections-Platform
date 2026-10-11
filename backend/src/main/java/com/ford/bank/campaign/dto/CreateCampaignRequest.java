package com.ford.bank.campaign.dto;

import com.ford.bank.campaign.domain.CampaignType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public record CreateCampaignRequest(

        @NotBlank(message = "Campaign name is required")
        @Size(max = 255, message = "Campaign name must not exceed 255 characters")
        String name,

        @NotNull(message = "Campaign type is required")
        CampaignType campaignType,

        @Size(max = 1000)
        String description,

        @Future(message = "Scheduled time must be in the future")
        OffsetDateTime scheduledAt
) {}
