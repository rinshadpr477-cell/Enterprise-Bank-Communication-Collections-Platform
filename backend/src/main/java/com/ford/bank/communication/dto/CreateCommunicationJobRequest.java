package com.ford.bank.communication.dto;

import com.ford.bank.customer.domain.CommunicationChannel;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateCommunicationJobRequest(
        @NotNull UUID customerId,
        UUID campaignId,
        @NotNull CommunicationChannel channel,
        OffsetDateTime scheduledAt,
        Integer maxAttempts,
        String idempotencyKey
) {}
