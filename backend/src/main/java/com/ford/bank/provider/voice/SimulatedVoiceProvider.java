package com.ford.bank.provider.voice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Simulated voice provider for local development and testing.
 * No real phone calls are made. No external credentials required.
 *
 * Configurable outcome rates:
 * - success-rate: fraction of calls that are ACCEPTED (default 85%)
 * - timeout-rate: fraction that timeout (default 5%)
 * - failure-rate: remainder are FAILED (default 10%)
 *
 * The simulated provider generates realistic provider references
 * and delayed callback scenarios without real network calls.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.provider.mode", havingValue = "SIMULATED", matchIfMissing = true)
public class SimulatedVoiceProvider implements VoiceProvider {

    private final Random random = new Random();

    @Value("${app.provider.simulated.success-rate:0.85}")
    private double successRate;

    @Value("${app.provider.simulated.timeout-rate:0.05}")
    private double timeoutRate;

    @Override
    public VoiceProviderResult initiateCall(VoiceCallRequest request) {
        log.info("[SIMULATED] Voice call to customerId={} phone={}",
                request.customerId(), maskPhone(request.phoneNumber()));

        double roll = random.nextDouble();

        if (roll < timeoutRate) {
            log.warn("[SIMULATED] Voice call TIMEOUT for customerId={}", request.customerId());
            return VoiceProviderResult.timeout();
        }

        if (roll < timeoutRate + (1.0 - successRate - timeoutRate)) {
            String failCode = randomFailureCode();
            log.info("[SIMULATED] Voice call FAILED code={} customerId={}", failCode, request.customerId());
            return VoiceProviderResult.failed(failCode, "Simulated failure: " + failCode);
        }

        String ref = "SIM-VOICE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[SIMULATED] Voice call ACCEPTED ref={} customerId={}", ref, request.customerId());
        return VoiceProviderResult.accepted(ref);
    }

    @Override
    public Optional<VoiceProviderResult> getCallStatus(String providerReference) {
        // Simulated status lookup: 80% delivered, 20% still unknown
        if (random.nextDouble() < 0.8) {
            return Optional.of(VoiceProviderResult.accepted(providerReference));
        }
        return Optional.empty();
    }

    private String randomFailureCode() {
        String[] codes = {"NO_ANSWER", "BUSY", "INVALID_NUMBER", "CARRIER_ERROR", "NETWORK_ERROR"};
        return codes[random.nextInt(codes.length)];
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}
