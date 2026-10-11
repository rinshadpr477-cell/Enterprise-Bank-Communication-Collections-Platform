package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.CollectionCase;
import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.domain.Priority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CollectionCaseResponse(
        UUID id,
        UUID customerId,
        String externalAccountReference,
        BigDecimal outstandingAmount,
        String currency,
        LocalDate dueDate,
        CollectionCaseStatus status,
        String assignedAgentId,
        Priority priority,
        String notes,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CollectionCaseResponse fromEntity(CollectionCase entity) {
        return new CollectionCaseResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getExternalAccountReference(),
                entity.getOutstandingAmount(),
                entity.getCurrency(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getAssignedAgentId(),
                entity.getPriority(),
                entity.getNotes(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
