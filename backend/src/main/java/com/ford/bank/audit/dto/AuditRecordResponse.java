package com.ford.bank.audit.dto;

import com.ford.bank.audit.domain.AuditRecord;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AuditRecordResponse(
        UUID id,
        String actorId,
        String action,
        String entityType,
        String entityId,
        String correlationId,
        String ipAddress,
        OffsetDateTime timestamp,
        Map<String, Object> metadata,
        String outcome
) {
    public static AuditRecordResponse fromEntity(AuditRecord entity) {
        return new AuditRecordResponse(
                entity.getId(),
                entity.getActorId(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getCorrelationId(),
                entity.getIpAddress(),
                entity.getTimestamp(),
                entity.getMetadata(),
                entity.getOutcome()
        );
    }
}
