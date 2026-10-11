package com.ford.bank.customer.controller;

import com.ford.bank.common.dto.PageResponse;
import com.ford.bank.customer.domain.CustomerStatus;
import com.ford.bank.customer.dto.CreateCustomerRequest;
import com.ford.bank.customer.dto.CustomerResponse;
import com.ford.bank.customer.dto.UpdateCustomerRequest;
import com.ford.bank.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@Tag(name = "Customers", description = "Customer management endpoints")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create a new customer")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        CustomerResponse response = customerService.createCustomer(request, extractActorId(jwt));
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Get customer by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable UUID id) {
        return ResponseEntity.ok(customerService.getCustomer(id));
    }

    @Operation(summary = "List customers with optional filtering and pagination")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER', 'COLLECTION_AGENT', 'AUDITOR')")
    public ResponseEntity<PageResponse<CustomerResponse>> listCustomers(
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        size = Math.min(size, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<CustomerResponse> result = customerService.listCustomers(status, search, pageable);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @Operation(summary = "Partially update a customer")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMPAIGN_MANAGER')")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        return ResponseEntity.ok(customerService.updateCustomer(id, request, extractActorId(jwt)));
    }

    private String extractActorId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "system";
    }
}
