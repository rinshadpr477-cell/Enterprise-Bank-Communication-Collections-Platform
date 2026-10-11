package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.AgentTaskType;
import com.ford.bank.collections.domain.Priority;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAgentTaskRequest(
        @NotNull UUID collectionCaseId,
        @NotNull AgentTaskType taskType,
        String assignedAgentId,
        Priority priority,
        OffsetDateTime dueAt,
        String notes
) {}
