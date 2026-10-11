package com.ford.bank.communication.service;

import com.ford.bank.common.exception.ConflictException;
import com.ford.bank.communication.domain.*;
import com.ford.bank.communication.dto.CreateCommunicationJobRequest;
import com.ford.bank.communication.dto.WebhookCallbackRequest;
import com.ford.bank.communication.repository.CommunicationAttemptRepository;
import com.ford.bank.communication.repository.CommunicationJobRepository;
import com.ford.bank.communication.repository.WebhookNonceRepository;
import com.ford.bank.customer.domain.CommunicationChannel;
import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.customer.repository.SuppressionRepository;
import com.ford.bank.kafka.outbox.OutboxService;
import com.ford.bank.provider.sms.SmsProvider;
import com.ford.bank.provider.sms.SmsProviderResult;
import com.ford.bank.provider.voice.VoiceCallRequest;
import com.ford.bank.provider.voice.VoiceProvider;
import com.ford.bank.provider.voice.VoiceProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommunicationJobServiceTest {

    @Mock
    private CommunicationJobRepository jobRepository;

    @Mock
    private CommunicationAttemptRepository attemptRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SuppressionRepository suppressionRepository;

    @Mock
    private WebhookNonceRepository webhookNonceRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private VoiceProvider voiceProvider;

    @Mock
    private SmsProvider smsProvider;

    @InjectMocks
    private CommunicationJobService jobService;

    private Customer customer;
    private UUID customerId;
    private CommunicationJob job;
    private UUID jobId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        customer = new Customer("CUST-1", "Bob Smith", "bob@example.com", "+919876543210", "en");
        customer.setId(customerId);

        jobId = UUID.randomUUID();
        job = new CommunicationJob();
        job.setId(jobId);
        job.setCustomerId(customerId);
        job.setChannel(CommunicationChannel.VOICE);
        job.setStatus(JobStatus.PENDING);
        job.setIdempotencyKey("KEY-123");
        job.setMaxAttempts(3);
    }

    @Test
    @DisplayName("Should dispatch voice call successfully and transition to COMPLETED")
    void dispatchJob_VoiceSuccess() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(voiceProvider.initiateCall(any(VoiceCallRequest.class)))
                .thenReturn(VoiceProviderResult.accepted("VOICE-REF-001"));
        when(jobRepository.save(any(CommunicationJob.class))).thenReturn(job);

        jobService.dispatchJob(jobId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        verify(attemptRepository).save(any(CommunicationAttempt.class));
        verify(outboxService).publishEvent(eq("CommunicationJob"), eq(jobId.toString()), eq("COMMUNICATION_JOB_COMPLETED"), any());
    }

    @Test
    @DisplayName("Should transition job to RETRYING when voice call fails and retry limit not reached")
    void dispatchJob_VoiceFailed_RetryScheduled() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(voiceProvider.initiateCall(any(VoiceCallRequest.class)))
                .thenReturn(VoiceProviderResult.failed("BUSY", "Line busy"));
        when(jobRepository.save(any(CommunicationJob.class))).thenReturn(job);

        jobService.dispatchJob(jobId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.RETRYING);
        assertThat(job.getNextRetryAt()).isNotNull();
        assertThat(job.getLastError()).isEqualTo("Line busy");
    }

    @Test
    @DisplayName("Should transition job to UNCERTAIN when provider times out")
    void dispatchJob_VoiceTimeout_UncertainStatus() {
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(voiceProvider.initiateCall(any(VoiceCallRequest.class)))
                .thenReturn(VoiceProviderResult.timeout());
        when(jobRepository.save(any(CommunicationJob.class))).thenReturn(job);

        jobService.dispatchJob(jobId);

        assertThat(job.getStatus()).isEqualTo(JobStatus.UNCERTAIN);
    }

    @Test
    @DisplayName("Should reject webhook callback with replay exception when nonce already exists")
    void processWebhookCallback_ReplayAttackPrevented() {
        WebhookCallbackRequest req = new WebhookCallbackRequest(
                "PROVIDER", "REF-123", "STATUS_UPDATE", "DELIVERED", null, null,
                "NONCE-999", System.currentTimeMillis(), "signature"
        );

        when(webhookNonceRepository.existsById("NONCE-999")).thenReturn(true);

        assertThatThrownBy(() -> jobService.processWebhookCallback(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Duplicate webhook nonce");

        verify(attemptRepository, never()).save(any());
    }
}
