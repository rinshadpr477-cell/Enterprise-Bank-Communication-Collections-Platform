package com.ford.bank.campaign.service;

import com.ford.bank.audit.service.AuditService;
import com.ford.bank.campaign.domain.*;
import com.ford.bank.campaign.dto.CampaignResponse;
import com.ford.bank.campaign.dto.CreateCampaignRequest;
import com.ford.bank.campaign.repository.CampaignRepository;
import com.ford.bank.campaign.repository.CampaignTargetRepository;
import com.ford.bank.common.exception.ConflictException;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.kafka.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignTargetRepository campaignTargetRepository;
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;
    private final AuditService auditService;

    @Transactional
    public CampaignResponse createCampaign(CreateCampaignRequest request, String actorId) {
        Campaign campaign = new Campaign();
        campaign.setName(request.name());
        campaign.setCampaignType(request.campaignType());
        campaign.setDescription(request.description());
        campaign.setScheduledAt(request.scheduledAt());
        campaign.setCreatedBy(actorId);

        Campaign saved = campaignRepository.save(campaign);
        auditService.record(actorId, "CAMPAIGN_CREATED", "Campaign", saved.getId().toString(), null);
        log.info("Created campaign id={} name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> listCampaigns(CampaignStatus status, Pageable pageable) {
        if (status != null) {
            return campaignRepository.findByStatus(status, pageable).map(this::toResponse);
        }
        return campaignRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public CampaignResponse scheduleCampaign(UUID id, OffsetDateTime scheduledAt, String actorId) {
        Campaign campaign = findOrThrow(id);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        if (scheduledAt != null) {
            campaign.setScheduledAt(scheduledAt);
        }
        Campaign saved = campaignRepository.save(campaign);

        // Publish outbox event - same transaction as status change
        outboxService.publishEvent("Campaign", id.toString(), "CampaignScheduled",
                java.util.Map.of("campaignId", id.toString(), "scheduledAt", saved.getScheduledAt().toString()));

        auditService.record(actorId, "CAMPAIGN_SCHEDULED", "Campaign", id.toString(), null);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse pauseCampaign(UUID id, String actorId) {
        Campaign campaign = findOrThrow(id);
        campaign.transitionTo(CampaignStatus.PAUSED);
        Campaign saved = campaignRepository.save(campaign);
        auditService.record(actorId, "CAMPAIGN_PAUSED", "Campaign", id.toString(), null);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse resumeCampaign(UUID id, String actorId) {
        Campaign campaign = findOrThrow(id);
        campaign.transitionTo(CampaignStatus.RUNNING);
        Campaign saved = campaignRepository.save(campaign);
        auditService.record(actorId, "CAMPAIGN_RESUMED", "Campaign", id.toString(), null);
        return toResponse(saved);
    }

    @Transactional
    public CampaignResponse cancelCampaign(UUID id, String actorId) {
        Campaign campaign = findOrThrow(id);
        campaign.transitionTo(CampaignStatus.CANCELLED);
        Campaign saved = campaignRepository.save(campaign);
        auditService.record(actorId, "CAMPAIGN_CANCELLED", "Campaign", id.toString(), null);
        return toResponse(saved);
    }

    @Transactional
    public void addTarget(UUID campaignId, UUID customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", customerId.toString());
        }

        Campaign campaign = findOrThrow(campaignId);
        if (campaign.getStatus() == CampaignStatus.COMPLETED || campaign.getStatus() == CampaignStatus.CANCELLED) {
            throw new com.ford.bank.common.exception.InvalidStateTransitionException(
                    "Cannot add targets to a " + campaign.getStatus() + " campaign");
        }

        try {
            campaignTargetRepository.save(new CampaignTarget(campaignId, customerId));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Customer " + customerId + " is already targeted in this campaign");
        }
    }

    @Transactional
    public void addTargetsBatch(UUID campaignId, List<UUID> customerIds) {
        Campaign campaign = findOrThrow(campaignId);
        if (!campaign.getStatus().canTransitionTo(CampaignStatus.RUNNING)
                && campaign.getStatus() != CampaignStatus.DRAFT
                && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            throw new com.ford.bank.common.exception.InvalidStateTransitionException(
                    "Cannot add targets to campaign in state: " + campaign.getStatus());
        }

        int added = 0;
        int skipped = 0;
        for (UUID customerId : customerIds) {
            if (customerRepository.existsById(customerId)
                    && !campaignTargetRepository.existsByCampaignIdAndCustomerId(campaignId, customerId)) {
                campaignTargetRepository.save(new CampaignTarget(campaignId, customerId));
                added++;
            } else {
                skipped++;
            }
        }
        log.info("Batch add targets campaignId={} added={} skipped={}", campaignId, added, skipped);
    }

    private Campaign findOrThrow(UUID id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id.toString()));
    }

    private CampaignResponse toResponse(Campaign c) {
        long pending = campaignTargetRepository.countByCampaignIdAndStatus(c.getId(), CampaignTargetStatus.PENDING);
        long completed = campaignTargetRepository.countByCampaignIdAndStatus(c.getId(), CampaignTargetStatus.COMPLETED);
        long total = pending + completed;

        return new CampaignResponse(
                c.getId(), c.getName(), c.getCampaignType(), c.getStatus(),
                c.getDescription(), c.getScheduledAt(), c.getStartedAt(), c.getCompletedAt(),
                c.getCreatedBy(), c.getCreatedAt(), c.getUpdatedAt(),
                pending, completed, total
        );
    }
}
