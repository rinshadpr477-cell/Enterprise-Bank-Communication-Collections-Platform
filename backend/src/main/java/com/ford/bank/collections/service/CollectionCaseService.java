package com.ford.bank.collections.service;

import com.ford.bank.collections.domain.CollectionCase;
import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.domain.Priority;
import com.ford.bank.collections.dto.CollectionCaseResponse;
import com.ford.bank.collections.dto.CreateCollectionCaseRequest;
import com.ford.bank.collections.dto.UpdateCollectionCaseRequest;
import com.ford.bank.collections.repository.CollectionCaseRepository;
import com.ford.bank.common.dto.PageResponse;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.kafka.outbox.OutboxService;
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
public class CollectionCaseService {

    private final CollectionCaseRepository collectionCaseRepository;
    private final CustomerRepository customerRepository;
    private final OutboxService outboxService;

    @Transactional
    public CollectionCaseResponse createCase(CreateCollectionCaseRequest request) {
        if (!customerRepository.existsById(request.customerId())) {
            throw new ResourceNotFoundException("Customer not found: " + request.customerId());
        }

        CollectionCase c = new CollectionCase();
        c.setCustomerId(request.customerId());
        c.setExternalAccountReference(request.externalAccountReference());
        c.setOutstandingAmount(request.outstandingAmount());
        c.setCurrency(request.currency() != null ? request.currency() : "INR");
        c.setDueDate(request.dueDate());
        c.setStatus(CollectionCaseStatus.OPEN);
        c.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        c.setNotes(request.notes());

        CollectionCase saved = collectionCaseRepository.save(c);

        Map<String, Object> payload = new HashMap<>();
        payload.put("caseId", saved.getId().toString());
        payload.put("customerId", saved.getCustomerId().toString());
        payload.put("outstandingAmount", saved.getOutstandingAmount());
        payload.put("status", saved.getStatus().name());

        outboxService.publishEvent("CollectionCase", saved.getId().toString(), "COLLECTION_CASE_CREATED", payload);
        log.info("Created collection case id={} customerId={}", saved.getId(), saved.getCustomerId());
        return CollectionCaseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CollectionCaseResponse getCase(UUID id) {
        CollectionCase c = collectionCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CollectionCase not found: " + id));
        return CollectionCaseResponse.fromEntity(c);
    }

    @Transactional(readOnly = true)
    public List<CollectionCaseResponse> getCasesByCustomer(UUID customerId) {
        return collectionCaseRepository.findByCustomerId(customerId).stream()
                .map(CollectionCaseResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CollectionCaseResponse> listCases(CollectionCaseStatus status, String agentId, Pageable pageable) {
        Page<CollectionCase> page;
        if (status != null && agentId != null) {
            page = collectionCaseRepository.findByAssignedAgentIdAndStatus(agentId, status, pageable);
        } else if (status != null) {
            page = collectionCaseRepository.findByStatus(status, pageable);
        } else if (agentId != null) {
            page = collectionCaseRepository.findByAssignedAgentId(agentId, pageable);
        } else {
            page = collectionCaseRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(CollectionCaseResponse::fromEntity));
    }

    @Transactional
    public CollectionCaseResponse updateCase(UUID id, UpdateCollectionCaseRequest request) {
        CollectionCase c = collectionCaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CollectionCase not found: " + id));

        boolean statusChanged = false;
        if (request.status() != null && request.status() != c.getStatus()) {
            c.setStatus(request.status());
            statusChanged = true;
            if (request.status() == CollectionCaseStatus.RESOLVED || request.status() == CollectionCaseStatus.WRITTEN_OFF) {
                c.setResolvedAt(OffsetDateTime.now());
            }
        }
        if (request.assignedAgentId() != null) {
            c.setAssignedAgentId(request.assignedAgentId());
        }
        if (request.outstandingAmount() != null) {
            c.setOutstandingAmount(request.outstandingAmount());
        }
        if (request.priority() != null) {
            c.setPriority(request.priority());
        }
        if (request.notes() != null) {
            c.setNotes(request.notes());
        }

        CollectionCase updated = collectionCaseRepository.save(c);

        if (statusChanged) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("caseId", updated.getId().toString());
            payload.put("newStatus", updated.getStatus().name());
            payload.put("outstandingAmount", updated.getOutstandingAmount());
            outboxService.publishEvent("CollectionCase", updated.getId().toString(), "COLLECTION_CASE_UPDATED", payload);
        }

        return CollectionCaseResponse.fromEntity(updated);
    }
}
