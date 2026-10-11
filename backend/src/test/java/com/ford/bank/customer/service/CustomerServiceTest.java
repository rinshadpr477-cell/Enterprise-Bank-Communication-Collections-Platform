package com.ford.bank.customer.service;

import com.ford.bank.audit.service.AuditService;
import com.ford.bank.common.exception.ConflictException;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.customer.domain.CommunicationChannel;
import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.domain.CustomerStatus;
import com.ford.bank.customer.domain.SuppressionEntry;
import com.ford.bank.customer.dto.CreateCustomerRequest;
import com.ford.bank.customer.dto.CustomerResponse;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.customer.repository.SuppressionRepository;
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
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SuppressionRepository suppressionRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        sampleCustomer = new Customer("CUST-1001", "Alice Smith", "alice@example.com", "+919876543210", "en");
        sampleCustomer.setId(customerId);
        sampleCustomer.setStatus(CustomerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create customer successfully when valid request is provided")
    void createCustomer_Success() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-1001", "Alice Smith", "alice@example.com", "+919876543210", "en"
        );

        when(customerRepository.existsByCustomerReference(request.customerReference())).thenReturn(false);
        when(customerRepository.existsByEmail(request.email())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);
        when(customerMapper.toResponse(sampleCustomer)).thenReturn(new CustomerResponse(
                customerId, "CUST-1001", "Alice Smith", "alice@example.com", "****3210", "en",
                CustomerStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()
        ));

        CustomerResponse response = customerService.createCustomer(request, "admin-user");

        assertThat(response).isNotNull();
        assertThat(response.customerReference()).isEqualTo("CUST-1001");
        verify(customerRepository).save(any(Customer.class));
        verify(auditService).record(eq("admin-user"), eq("CUSTOMER_CREATED"), eq("Customer"), eq(customerId.toString()), any());
    }

    @Test
    @DisplayName("Should reject create customer when reference already exists")
    void createCustomer_DuplicateReference() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "CUST-1001", "Alice Smith", "alice@example.com", "+919876543210", "en"
        );

        when(customerRepository.existsByCustomerReference(request.customerReference())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request, "admin-user"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve customer by ID")
    void getCustomer_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(customerMapper.toResponse(sampleCustomer)).thenReturn(new CustomerResponse(
                customerId, "CUST-1001", "Alice Smith", "alice@example.com", "****3210", "en",
                CustomerStatus.ACTIVE, OffsetDateTime.now(), OffsetDateTime.now()
        ));

        CustomerResponse response = customerService.getCustomer(customerId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found")
    void getCustomer_NotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(customerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should return false for communication when customer is deceased")
    void isEligibleForCommunication_Deceased() {
        sampleCustomer.setStatus(CustomerStatus.DECEASED);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));

        boolean eligible = customerService.isEligibleForCommunication(customerId, CommunicationChannel.SMS, "+919876543210");

        assertThat(eligible).isFalse();
    }

    @Test
    @DisplayName("Should return false for communication when contact is suppressed")
    void isEligibleForCommunication_Suppressed() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        SuppressionEntry suppression = new SuppressionEntry("+919876543210", "SMS", "Customer opted out");
        when(suppressionRepository.findActiveSuppressionForContact(eq("+919876543210"), eq("SMS"), any()))
                .thenReturn(Optional.of(suppression));

        boolean eligible = customerService.isEligibleForCommunication(customerId, CommunicationChannel.SMS, "+919876543210");

        assertThat(eligible).isFalse();
    }

    @Test
    @DisplayName("Should return true for communication when customer is active and not suppressed")
    void isEligibleForCommunication_Eligible() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(sampleCustomer));
        when(suppressionRepository.findActiveSuppressionForContact(eq("+919876543210"), eq("SMS"), any()))
                .thenReturn(Optional.empty());

        boolean eligible = customerService.isEligibleForCommunication(customerId, CommunicationChannel.SMS, "+919876543210");

        assertThat(eligible).isTrue();
    }
}
