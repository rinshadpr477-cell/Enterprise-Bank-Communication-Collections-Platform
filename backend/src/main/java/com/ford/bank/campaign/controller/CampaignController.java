package com.ford.bank.campaign.controller;

import com.ford.bank.campaign.domain.CampaignStatus;
import com.ford.bank.campaign.dto.CampaignResponse;
import com.ford.bank.campaign.dto.CreateCampaignRequest;
import com.ford.bank.campaign.service.CampaignService;
import com.ford.bank.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Campaigns", description = "Campaign management and lifecycle control")
@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @Operation(summary = "Create a new campaign")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CampaignResponse> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        CampaignResponse response = campaignService.createCampaign(request, extractActorId(jwt));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get campaign by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'AUDITOR')")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable UUID id) {
        return ResponseEntity.ok(campaignService.getCampaign(id));
    }

    @Operation(summary = "List campaigns with pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'AUDITOR')")
    public ResponseEntity<PageResponse<CampaignResponse>> listCampaigns(
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        size = Math.min(size, 100);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(PageResponse.from(campaignService.listCampaigns(status, pageable)));
    }

    @Operation(summary = "Schedule a campaign")
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CampaignResponse> scheduleCampaign(
            @PathVariable UUID id,
            @RequestParam(required = false) OffsetDateTime scheduledAt,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(campaignService.scheduleCampaign(id, scheduledAt, extractActorId(jwt)));
    }

    @Operation(summary = "Pause a running campaign")
    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CampaignResponse> pauseCampaign(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(campaignService.pauseCampaign(id, extractActorId(jwt)));
    }

    @Operation(summary = "Resume a paused campaign")
    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CampaignResponse> resumeCampaign(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(campaignService.resumeCampaign(id, extractActorId(jwt)));
    }

    @Operation(summary = "Cancel a campaign")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CampaignResponse> cancelCampaign(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(campaignService.cancelCampaign(id, extractActorId(jwt)));
    }

    @Operation(summary = "Add customer targets to a campaign (batch)")
    @PostMapping("/{id}/targets")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<Void> addTargets(
            @PathVariable UUID id,
            @RequestBody List<UUID> customerIds,
            @AuthenticationPrincipal Jwt jwt) {

        if (customerIds == null || customerIds.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        campaignService.addTargetsBatch(id, customerIds);
        return ResponseEntity.noContent().build();
    }

    private String extractActorId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "system";
    }
}
