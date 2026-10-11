package com.ford.bank.communication.service;

import com.ford.bank.common.dto.PageResponse;
import com.ford.bank.common.exception.ConflictException;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.communication.domain.*;
import com.ford.bank.communication.dto.*;
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
import com.ford.bank.provider.sms.SmsRequest;
import com.ford.bank.provider.voice.VoiceCallRequest;
import com.ford.bank.provider.voice.VoiceProvider;
import com.ford.bank.provider.voice.VoiceProviderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunicationJobService {

    private final CommunicationJobRepository jobRepository;
    private final CommunicationAttemptRepository attemptRepository;
    private final CustomerRepository customerRepository;
    private final SuppressionRepository suppressionRepository;
    private final WebhookNonceRepository webhookNonceRepository;
    private final OutboxService outboxService;
    private final VoiceProvider voiceProvider;
    private final SmsProvider smsProvider;

    @Transactional
    public CommunicationJobResponse createJob(CreateCommunicationJobRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        String idempotencyKey = request.idempotencyKey();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = "JOB-" + UUID.randomUUID();
        }

        var existing = jobRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent request: returning existing job id={}", existing.get().getId());
            return CommunicationJobResponse.fromEntity(existing.get());
        }

        CommunicationJob job = new CommunicationJob();
        job.setCustomerId(customer.getId());
        job.setCampaignId(request.campaignId());
        job.setChannel(request.channel());
        job.setIdempotencyKey(idempotencyKey);
        job.setMaxAttempts(request.maxAttempts() != null && request.maxAttempts() > 0 ? request.maxAttempts() : 3);
        job.setScheduledAt(request.scheduledAt() != null ? request.scheduledAt() : OffsetDateTime.now());

        // Check contact suppression rule before queueing
        String contact = request.channel() == CommunicationChannel.EMAIL ? customer.getEmail() : customer.getPhoneNumber();
        boolean isSuppressed = false;
        if (contact != null) {
            isSuppressed = suppressionRepository
                    .findActiveSuppressionForContact(contact, request.channel().name(), OffsetDateTime.now())
                    .isPresent();
        }

        if (isSuppressed) {
            job.setStatus(JobStatus.SUPPRESSED);
            job.setLastError("Contact is in suppression list for channel " + request.channel());
            log.warn("Customer {} contact is suppressed for channel {}", customer.getId(), request.channel());
        } else {
            job.setStatus(JobStatus.PENDING);
        }

        CommunicationJob saved = jobRepository.save(job);

        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", saved.getId().toString());
        payload.put("customerId", saved.getCustomerId().toString());
        payload.put("channel", saved.getChannel().name());
        payload.put("status", saved.getStatus().name());
        if (saved.getCampaignId() != null) {
            payload.put("campaignId", saved.getCampaignId().toString());
        }

        outboxService.publishEvent("CommunicationJob", saved.getId().toString(), "COMMUNICATION_JOB_CREATED", payload);
        log.info("Communication job created id={} status={}", saved.getId(), saved.getStatus());
        return CommunicationJobResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CommunicationJobResponse getJob(UUID id) {
        CommunicationJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + id));
        return CommunicationJobResponse.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public List<CommunicationAttemptResponse> getJobAttempts(UUID jobId) {
        return attemptRepository.findByJobIdOrderByAttemptNumberDesc(jobId).stream()
                .map(CommunicationAttemptResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CommunicationJobResponse> listJobs(
            JobStatus status, CommunicationChannel channel, UUID customerId, UUID campaignId, Pageable pageable) {
        Page<CommunicationJob> page = jobRepository.findWithFilters(status, channel, customerId, campaignId, pageable);
        return PageResponse.from(page.map(CommunicationJobResponse::fromEntity));
    }

    @Transactional
    public CommunicationJobResponse dispatchJob(UUID jobId) {
        CommunicationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

        if (job.isTerminal()) {
            throw new ConflictException("Job is already in terminal state: " + job.getStatus());
        }

        Customer customer = customerRepository.findById(job.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + job.getCustomerId()));

        job.setStatus(JobStatus.IN_PROGRESS);
        job.setAttemptCount(job.getAttemptCount() + 1);
        jobRepository.save(job);

        int currentAttemptNumber = job.getAttemptCount();
        String providerName = job.getChannel() == CommunicationChannel.VOICE ? "SIMULATED_VOICE" : "SIMULATED_SMS";
        CommunicationAttempt attempt = new CommunicationAttempt(job.getId(), providerName, currentAttemptNumber);

        if (job.getChannel() == CommunicationChannel.VOICE) {
            VoiceCallRequest callReq = new VoiceCallRequest(
                    customer.getPhoneNumber(),
                    customer.getPreferredLanguage(),
                    "Dear customer, reminder for your bank account update.",
                    job.getIdempotencyKey() + "-" + currentAttemptNumber,
                    customer.getId().toString()
            );
            VoiceProviderResult res = voiceProvider.initiateCall(callReq);
            attempt.setProviderReference(res.providerReference());

            switch (res.status()) {
                case ACCEPTED -> {
                    attempt.setStatus(AttemptStatus.DELIVERED);
                    attempt.setCompletedAt(OffsetDateTime.now());
                    job.setStatus(JobStatus.COMPLETED);
                    recordOutbox(job, "COMMUNICATION_JOB_COMPLETED");
                }
                case TIMEOUT -> {
                    attempt.setStatus(AttemptStatus.TIMEOUT);
                    attempt.setFailureReason("Provider timeout");
                    job.setStatus(JobStatus.UNCERTAIN);
                    job.setLastError("Provider timed out");
                }
                default -> {
                    attempt.setStatus(AttemptStatus.FAILED);
                    attempt.setFailureCode(res.failureCode());
                    attempt.setFailureReason(res.failureReason());
                    attempt.setCompletedAt(OffsetDateTime.now());
                    handleFailure(job, res.failureReason());
                }
            }
        } else {
            SmsRequest smsReq = new SmsRequest(
                    customer.getPhoneNumber(),
                    "Dear customer, urgent payment reminder for your bank account.",
                    "TEMPLATE_REMINDER",
                    job.getIdempotencyKey() + "-" + currentAttemptNumber,
                    customer.getId().toString()
            );
            SmsProviderResult res = smsProvider.sendSms(smsReq);
            attempt.setProviderReference(res.providerReference());

            switch (res.status()) {
                case ACCEPTED -> {
                    attempt.setStatus(AttemptStatus.DELIVERED);
                    attempt.setCompletedAt(OffsetDateTime.now());
                    job.setStatus(JobStatus.COMPLETED);
                    recordOutbox(job, "COMMUNICATION_JOB_COMPLETED");
                }
                case TIMEOUT -> {
                    attempt.setStatus(AttemptStatus.TIMEOUT);
                    attempt.setFailureReason("Provider timeout");
                    job.setStatus(JobStatus.UNCERTAIN);
                    job.setLastError("Provider timed out");
                }
                default -> {
                    attempt.setStatus(AttemptStatus.FAILED);
                    attempt.setFailureCode(res.failureCode());
                    attempt.setFailureReason(res.failureReason());
                    attempt.setCompletedAt(OffsetDateTime.now());
                    handleFailure(job, res.failureReason());
                }
            }
        }

        attemptRepository.save(attempt);
        CommunicationJob saved = jobRepository.save(job);
        return CommunicationJobResponse.fromEntity(saved);
    }

    @Transactional
    public void processWebhookCallback(WebhookCallbackRequest request) {
        // Replay attack protection: verify nonce
        if (webhookNonceRepository.existsById(request.nonce())) {
            log.warn("Duplicate webhook callback detected for nonce: {}", request.nonce());
            throw new ConflictException("Duplicate webhook nonce: replay detected");
        }

        WebhookNonce nonce = new WebhookNonce(request.nonce(), request.provider(), OffsetDateTime.now().plusDays(1));
        webhookNonceRepository.save(nonce);

        var attemptOpt = attemptRepository.findByProviderReference(request.providerReference());
        if (attemptOpt.isEmpty()) {
            log.warn("Received webhook for unknown provider reference: {}", request.providerReference());
            return;
        }

        CommunicationAttempt attempt = attemptOpt.get();
        attempt.setCompletedAt(OffsetDateTime.now());

        CommunicationJob job = jobRepository.findById(attempt.getJobId()).orElse(null);
        if (job == null) return;

        if ("DELIVERED".equalsIgnoreCase(request.status()) || "SUCCESS".equalsIgnoreCase(request.status())) {
            attempt.setStatus(AttemptStatus.DELIVERED);
            job.setStatus(JobStatus.COMPLETED);
            recordOutbox(job, "COMMUNICATION_JOB_COMPLETED");
        } else {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setFailureCode(request.failureCode());
            attempt.setFailureReason(request.failureReason());
            handleFailure(job, request.failureReason());
        }

        attemptRepository.save(attempt);
        jobRepository.save(job);
        log.info("Webhook processed for attempt id={} new status={}", attempt.getId(), attempt.getStatus());
    }

    private void handleFailure(CommunicationJob job, String reason) {
        job.setStatus(JobStatus.FAILED);
        job.setLastError(reason);
        if (job.canRetry()) {
            job.setStatus(JobStatus.RETRYING);
            job.setNextRetryAt(job.calculateNextRetryAt());
            log.info("Job {} eligible for retry. Scheduled for {}", job.getId(), job.getNextRetryAt());
        } else {
            recordOutbox(job, "COMMUNICATION_JOB_FAILED");
        }
    }

    private void recordOutbox(CommunicationJob job, String eventType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("jobId", job.getId().toString());
        payload.put("customerId", job.getCustomerId().toString());
        payload.put("channel", job.getChannel().name());
        payload.put("status", job.getStatus().name());
        outboxService.publishEvent("CommunicationJob", job.getId().toString(), eventType, payload);
    }
}
