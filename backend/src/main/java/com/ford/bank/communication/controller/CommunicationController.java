package com.ford.bank.communication.controller;

import com.ford.bank.common.dto.PageResponse;
import com.ford.bank.communication.domain.JobStatus;
import com.ford.bank.communication.dto.*;
import com.ford.bank.communication.service.CommunicationJobService;
import com.ford.bank.customer.domain.CommunicationChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Communication", description = "Communication job orchestration, dispatch, and callback handling")
@RestController
@RequestMapping("/api/v1/communication")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationJobService communicationJobService;

    @Operation(summary = "Create a communication job")
    @PostMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CommunicationJobResponse> createJob(@Valid @RequestBody CreateCommunicationJobRequest request) {
        CommunicationJobResponse response = communicationJobService.createJob(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get communication job by ID")
    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<CommunicationJobResponse> getJob(@PathVariable UUID id) {
        return ResponseEntity.ok(communicationJobService.getJob(id));
    }

    @Operation(summary = "Get all attempt history for a communication job")
    @GetMapping("/jobs/{id}/attempts")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<List<CommunicationAttemptResponse>> getJobAttempts(@PathVariable UUID id) {
        return ResponseEntity.ok(communicationJobService.getJobAttempts(id));
    }

    @Operation(summary = "List communication jobs with filtering and pagination")
    @GetMapping("/jobs")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<PageResponse<CommunicationJobResponse>> listJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) CommunicationChannel channel,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(communicationJobService.listJobs(status, channel, customerId, campaignId, pageable));
    }

    @Operation(summary = "Trigger immediate dispatch of a communication job")
    @PostMapping("/jobs/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CommunicationJobResponse> dispatchJob(@PathVariable UUID id) {
        return ResponseEntity.ok(communicationJobService.dispatchJob(id));
    }

    @Operation(summary = "Handle provider callback webhook with replay protection")
    @PostMapping("/webhooks/callback")
    public ResponseEntity<Void> handleWebhookCallback(@Valid @RequestBody WebhookCallbackRequest request) {
        communicationJobService.processWebhookCallback(request);
        return ResponseEntity.ok().build();
    }
}
