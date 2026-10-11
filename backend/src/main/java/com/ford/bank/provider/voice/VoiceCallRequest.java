package com.ford.bank.provider.voice;

public record VoiceCallRequest(
        String phoneNumber,
        String language,
        String messageTemplate,
        String idempotencyKey,
        String customerId
) {}
