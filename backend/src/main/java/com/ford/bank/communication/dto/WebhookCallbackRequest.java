package com.ford.bank.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookCallbackRequest(
        @NotBlank String provider,
        @NotBlank String providerReference,
        @NotBlank String eventType,
        @NotBlank String status,
        String failureCode,
        String failureReason,
        @NotBlank String nonce,
        @NotNull Long timestamp,
        String signature
) {}
