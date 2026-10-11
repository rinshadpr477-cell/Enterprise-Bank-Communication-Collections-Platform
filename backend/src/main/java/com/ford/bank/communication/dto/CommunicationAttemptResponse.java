package com.ford.bank.communication.dto;

import com.ford.bank.communication.domain.AttemptStatus;
import com.ford.bank.communication.domain.CommunicationAttempt;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommunicationAttemptResponse(
        UUID id,
        UUID jobId,
        String provider,
        String providerReference,
        int attemptNumber,
        AttemptStatus status,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String failureCode,
        String failureReason,
        String rawResponse
) {
    public static CommunicationAttemptResponse fromEntity(CommunicationAttempt entity) {
        return new CommunicationAttemptResponse(
                entity.getId(),
                entity.getJobId(),
                entity.getProvider(),
                entity.getProviderReference(),
                entity.getAttemptNumber(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getFailureCode(),
                entity.getFailureReason(),
                entity.getRawResponse()
        );
    }
}
