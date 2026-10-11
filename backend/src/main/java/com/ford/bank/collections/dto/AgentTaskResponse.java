package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.AgentTask;
import com.ford.bank.collections.domain.AgentTaskStatus;
import com.ford.bank.collections.domain.AgentTaskType;
import com.ford.bank.collections.domain.Priority;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgentTaskResponse(
        UUID id,
        UUID collectionCaseId,
        AgentTaskType taskType,
        AgentTaskStatus status,
        String assignedAgentId,
        Priority priority,
        OffsetDateTime dueAt,
        String notes,
        OffsetDateTime completedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AgentTaskResponse fromEntity(AgentTask entity) {
        return new AgentTaskResponse(
                entity.getId(),
                entity.getCollectionCaseId(),
                entity.getTaskType(),
                entity.getStatus(),
                entity.getAssignedAgentId(),
                entity.getPriority(),
                entity.getDueAt(),
                entity.getNotes(),
                entity.getCompletedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
