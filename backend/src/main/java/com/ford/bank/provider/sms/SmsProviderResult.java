package com.ford.bank.provider.sms;

public record SmsProviderResult(
        ResultStatus status,
        String providerReference,
        String failureCode,
        String failureReason
) {
    public enum ResultStatus { ACCEPTED, FAILED, TIMEOUT, DUPLICATE }

    public static SmsProviderResult accepted(String ref) {
        return new SmsProviderResult(ResultStatus.ACCEPTED, ref, null, null);
    }

    public static SmsProviderResult failed(String code, String reason) {
        return new SmsProviderResult(ResultStatus.FAILED, null, code, reason);
    }

    public static SmsProviderResult timeout() {
        return new SmsProviderResult(ResultStatus.TIMEOUT, null, "TIMEOUT", "No response within timeout");
    }
}
