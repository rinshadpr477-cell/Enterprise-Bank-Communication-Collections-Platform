package com.ford.bank.provider.sms;

import java.util.Optional;

/**
 * Provider interface for SMS dispatch.
 * Isolated behind this interface to allow provider switching without
 * changing business logic.
 */
public interface SmsProvider {
    SmsProviderResult sendSms(SmsRequest request);
    Optional<SmsProviderResult> getDeliveryStatus(String providerReference);
}
