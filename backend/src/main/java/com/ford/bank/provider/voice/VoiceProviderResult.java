package com.ford.bank.provider.voice;

public record VoiceProviderResult(
        ResultStatus status,
        String providerReference,
        String failureCode,
        String failureReason,
        String rawResponse
) {
    public enum ResultStatus {
        ACCEPTED,    // Call accepted by provider; awaiting delivery callback
        FAILED,      // Provider definitively rejected the call
        TIMEOUT,     // No response within timeout; outcome unknown - treat as UNCERTAIN
        DUPLICATE    // Provider recognized this as a duplicate (idempotent retry)
    }

    public static VoiceProviderResult accepted(String ref) {
        return new VoiceProviderResult(ResultStatus.ACCEPTED, ref, null, null, null);
    }

    public static VoiceProviderResult failed(String code, String reason) {
        return new VoiceProviderResult(ResultStatus.FAILED, null, code, reason, null);
    }

    public static VoiceProviderResult timeout() {
        return new VoiceProviderResult(ResultStatus.TIMEOUT, null, "TIMEOUT", "No response within timeout", null);
    }
}
