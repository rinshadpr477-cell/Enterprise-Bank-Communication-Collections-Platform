package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.AgentTaskStatus;
import com.ford.bank.collections.domain.Priority;

import java.time.OffsetDateTime;

public record UpdateAgentTaskRequest(
        AgentTaskStatus status,
        String assignedAgentId,
        Priority priority,
        OffsetDateTime dueAt,
        String notes
) {}
