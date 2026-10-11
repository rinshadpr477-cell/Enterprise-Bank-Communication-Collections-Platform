package com.ford.bank.collections.dto;

import com.ford.bank.collections.domain.Priority;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateCollectionCaseRequest(
        @NotNull UUID customerId,
        @NotBlank @Size(max = 100) String externalAccountReference,
        @NotNull @DecimalMin("0.00") BigDecimal outstandingAmount,
        @Size(min = 3, max = 3) String currency,
        @NotNull LocalDate dueDate,
        Priority priority,
        String notes
) {}
