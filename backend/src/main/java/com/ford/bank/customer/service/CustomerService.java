package com.ford.bank.customer.service;

import com.ford.bank.audit.service.AuditService;
import com.ford.bank.common.exception.ConflictException;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.domain.CustomerStatus;
import com.ford.bank.customer.domain.CommunicationChannel;
import com.ford.bank.customer.dto.CreateCustomerRequest;
import com.ford.bank.customer.dto.CustomerResponse;
import com.ford.bank.customer.dto.UpdateCustomerRequest;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.customer.repository.SuppressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Customer application service.
 * Keeps business rules out of controllers.
 * Enforces data invariants: no duplicate customer references or emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SuppressionRepository suppressionRepository;
    private final AuditService auditService;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, String actorId) {
        if (customerRepository.existsByCustomerReference(request.customerReference())) {
            throw new ConflictException("Customer with reference " + request.customerReference() + " already exists");
        }

        if (request.email() != null && customerRepository.existsByEmail(request.email())) {
            throw new ConflictException("Customer with email " + request.email() + " already exists");
        }

        Customer customer = new Customer(
                request.customerReference(),
                request.fullName(),
                request.email(),
                request.phoneNumber(),
                request.preferredLanguage()
        );

        Customer saved = customerRepository.save(customer);
        log.info("Created customer id={} ref={}", saved.getId(), saved.getCustomerReference());
        auditService.record(actorId, "CUSTOMER_CREATED", "Customer", saved.getId().toString(), null);

        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        return customerMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(CustomerStatus status, String search, Pageable pageable) {
        return customerRepository.findWithFilters(status, search, pageable)
                .map(customerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request, String actorId) {
        Customer customer = findOrThrow(id);

        if (request.fullName() != null) {
            customer.setFullName(request.fullName());
        }
        if (request.email() != null) {
            if (!request.email().equals(customer.getEmail()) && customerRepository.existsByEmail(request.email())) {
                throw new ConflictException("Email already used by another customer");
            }
            customer.setEmail(request.email());
        }
        if (request.phoneNumber() != null) {
            customer.setPhoneNumber(request.phoneNumber());
        }
        if (request.preferredLanguage() != null) {
            customer.setPreferredLanguage(request.preferredLanguage());
        }
        if (request.status() != null) {
            customer.setStatus(request.status());
        }

        Customer saved = customerRepository.save(customer);
        auditService.record(actorId, "CUSTOMER_UPDATED", "Customer", id.toString(), null);
        return customerMapper.toResponse(saved);
    }

    /**
     * Check if a customer is eligible for communication on a given channel.
     * This is called immediately before dispatching - NOT cached.
     */
    @Transactional(readOnly = true)
    public boolean isEligibleForCommunication(UUID customerId, CommunicationChannel channel, String normalizedContact) {
        Customer customer = findOrThrow(customerId);

        // Deceased customers are never contacted
        if (customer.getStatus() == CustomerStatus.DECEASED) {
            log.info("Suppressed: customer {} is DECEASED", customerId);
            return false;
        }

        if (!customer.isActive()) {
            log.info("Suppressed: customer {} status={}", customerId, customer.getStatus());
            return false;
        }

        // Check suppression list
        var suppression = suppressionRepository.findActiveSuppressionForContact(
                normalizedContact, channel.name(), OffsetDateTime.now()
        );

        if (suppression.isPresent()) {
            log.info("Suppressed: contact {} on channel {} reason={}",
                    normalizedContact, channel, suppression.get().getReason());
            return false;
        }

        return true;
    }

    private Customer findOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id.toString()));
    }
}
