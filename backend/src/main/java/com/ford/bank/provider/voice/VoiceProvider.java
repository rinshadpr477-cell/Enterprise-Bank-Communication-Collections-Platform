package com.ford.bank.provider.voice;

import java.util.Optional;

/**
 * Provider interface for voice call dispatch.
 * Implementations must be isolated behind this interface so switching
 * providers does not affect business logic.
 *
 * Design note: Provider calls are NOT made within open database transactions.
 * The transaction boundary ends before the HTTP call, and the result is
 * persisted afterward. This prevents long-held DB connections during network calls.
 */
public interface VoiceProvider {

    /**
     * Initiate an outbound voice call.
     * Returns a ProviderResult containing the provider's call reference.
     *
     * A TIMEOUT result means the call may or may not have been accepted by the provider.
     * Do NOT blindly retry a TIMEOUT - treat the job as UNCERTAIN and reconcile.
     */
    VoiceProviderResult initiateCall(VoiceCallRequest request);

    /**
     * Check the current status of a call by provider reference.
     * Used to reconcile UNCERTAIN outcomes.
     */
    Optional<VoiceProviderResult> getCallStatus(String providerReference);
}
