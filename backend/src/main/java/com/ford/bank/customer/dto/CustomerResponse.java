package com.ford.bank.customer.dto;

import com.ford.bank.customer.domain.CustomerStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API response for customer data.
 * Phone numbers are partially masked (last 4 digits visible only).
 * Full phone access requires elevated permissions checked at service layer.
 */
public record CustomerResponse(
        UUID id,
        String customerReference,
        String fullName,
        String email,
        String maskedPhoneNumber,
        String preferredLanguage,
        CustomerStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
