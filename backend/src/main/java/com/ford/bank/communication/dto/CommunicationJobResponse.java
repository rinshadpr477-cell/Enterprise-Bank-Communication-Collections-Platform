package com.ford.bank.communication.dto;

import com.ford.bank.communication.domain.CommunicationJob;
import com.ford.bank.communication.domain.JobStatus;
import com.ford.bank.customer.domain.CommunicationChannel;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunicationJobResponse(
        UUID id,
        UUID customerId,
        UUID campaignId,
        CommunicationChannel channel,
        JobStatus status,
        OffsetDateTime scheduledAt,
        int attemptCount,
        int maxAttempts,
        String idempotencyKey,
        String lastError,
        OffsetDateTime nextRetryAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommunicationJobResponse fromEntity(CommunicationJob entity) {
        return new CommunicationJobResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getCampaignId(),
                entity.getChannel(),
                entity.getStatus(),
                entity.getScheduledAt(),
                entity.getAttemptCount(),
                entity.getMaxAttempts(),
                entity.getIdempotencyKey(),
                entity.getLastError(),
                entity.getNextRetryAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
