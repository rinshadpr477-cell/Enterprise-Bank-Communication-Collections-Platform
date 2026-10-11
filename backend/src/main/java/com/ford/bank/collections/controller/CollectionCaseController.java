package com.ford.bank.collections.controller;

import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.dto.CollectionCaseResponse;
import com.ford.bank.collections.dto.CreateCollectionCaseRequest;
import com.ford.bank.collections.dto.UpdateCollectionCaseRequest;
import com.ford.bank.collections.service.CollectionCaseService;
import com.ford.bank.common.dto.PageResponse;
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

@Tag(name = "Collections", description = "Collection case and account recovery endpoints")
@RestController
@RequestMapping("/api/v1/collections/cases")
@RequiredArgsConstructor
public class CollectionCaseController {

    private final CollectionCaseService collectionCaseService;

    @Operation(summary = "Create a new collection case")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT')")
    public ResponseEntity<CollectionCaseResponse> createCase(@Valid @RequestBody CreateCollectionCaseRequest request) {
        CollectionCaseResponse response = collectionCaseService.createCase(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get collection case by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<CollectionCaseResponse> getCase(@PathVariable UUID id) {
        return ResponseEntity.ok(collectionCaseService.getCase(id));
    }

    @Operation(summary = "Get collection cases for a specific customer")
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<List<CollectionCaseResponse>> getCasesByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(collectionCaseService.getCasesByCustomer(customerId));
    }

    @Operation(summary = "List collection cases with filtering and pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<PageResponse<CollectionCaseResponse>> listCases(
            @RequestParam(required = false) CollectionCaseStatus status,
            @RequestParam(required = false) String agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return ResponseEntity.ok(collectionCaseService.listCases(status, agentId, pageable));
    }

    @Operation(summary = "Update a collection case status or details")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT')")
    public ResponseEntity<CollectionCaseResponse> updateCase(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCollectionCaseRequest request) {
        return ResponseEntity.ok(collectionCaseService.updateCase(id, request));
    }
}
