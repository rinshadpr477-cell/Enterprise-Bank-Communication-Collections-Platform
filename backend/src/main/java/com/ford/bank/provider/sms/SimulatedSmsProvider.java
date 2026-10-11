package com.ford.bank.provider.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.provider.mode", havingValue = "SIMULATED", matchIfMissing = true)
public class SimulatedSmsProvider implements SmsProvider {

    private final Random random = new Random();

    @Value("${app.provider.simulated.success-rate:0.85}")
    private double successRate;

    @Value("${app.provider.simulated.timeout-rate:0.05}")
    private double timeoutRate;

    @Override
    public SmsProviderResult sendSms(SmsRequest request) {
        log.info("[SIMULATED] SMS to customerId={}", request.customerId());

        double roll = random.nextDouble();

        if (roll < timeoutRate) return SmsProviderResult.timeout();

        if (roll < timeoutRate + (1.0 - successRate - timeoutRate)) {
            return SmsProviderResult.failed("DELIVERY_FAILED", "Simulated SMS delivery failure");
        }

        String ref = "SIM-SMS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[SIMULATED] SMS ACCEPTED ref={}", ref);
        return SmsProviderResult.accepted(ref);
    }

    @Override
    public Optional<SmsProviderResult> getDeliveryStatus(String providerReference) {
        return random.nextDouble() < 0.8
                ? Optional.of(SmsProviderResult.accepted(providerReference))
                : Optional.empty();
    }
}
