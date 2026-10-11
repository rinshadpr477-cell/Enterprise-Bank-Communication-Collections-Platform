package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.domain.Priority;

import java.math.BigDecimal;

public record UpdateCollectionCaseRequest(
        CollectionCaseStatus status,
        String assignedAgentId,
        BigDecimal outstandingAmount,
        Priority priority,
        String notes
) {}
