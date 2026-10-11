package com.ford.bank.collections.service;

import com.ford.bank.collections.domain.CollectionCase;
import com.ford.bank.collections.domain.CollectionCaseStatus;
import com.ford.bank.collections.domain.Priority;
import com.ford.bank.collections.dto.CollectionCaseResponse;
import com.ford.bank.collections.dto.CreateCollectionCaseRequest;
import com.ford.bank.collections.dto.UpdateCollectionCaseRequest;
import com.ford.bank.collections.repository.CollectionCaseRepository;
import com.ford.bank.common.exception.ResourceNotFoundException;
import com.ford.bank.customer.repository.CustomerRepository;
import com.ford.bank.kafka.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionCaseServiceTest {

    @Mock
    private CollectionCaseRepository caseRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CollectionCaseService caseService;

    private UUID customerId;
    private UUID caseId;
    private CollectionCase sampleCase;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        caseId = UUID.randomUUID();

        sampleCase = new CollectionCase();
        sampleCase.setId(caseId);
        sampleCase.setCustomerId(customerId);
        sampleCase.setExternalAccountReference("ACC-987654");
        sampleCase.setOutstandingAmount(new BigDecimal("15000.5000"));
        sampleCase.setCurrency("INR");
        sampleCase.setDueDate(LocalDate.now().plusDays(10));
        sampleCase.setStatus(CollectionCaseStatus.OPEN);
        sampleCase.setPriority(Priority.HIGH);
    }

    @Test
    @DisplayName("Should create collection case successfully and publish outbox event")
    void createCase_Success() {
        CreateCollectionCaseRequest req = new CreateCollectionCaseRequest(
                customerId, "ACC-987654", new BigDecimal("15000.5000"), "INR",
                LocalDate.now().plusDays(10), Priority.HIGH, "Initial delinquent case"
        );

        when(customerRepository.existsById(customerId)).thenReturn(true);
        when(caseRepository.save(any(CollectionCase.class))).thenReturn(sampleCase);

        CollectionCaseResponse res = caseService.createCase(req);

        assertThat(res).isNotNull();
        assertThat(res.externalAccountReference()).isEqualTo("ACC-987654");
        assertThat(res.outstandingAmount()).isEqualTo(new BigDecimal("15000.5000"));
        verify(outboxService).publishEvent(eq("CollectionCase"), eq(caseId.toString()), eq("COLLECTION_CASE_CREATED"), any());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when creating case for non-existent customer")
    void createCase_CustomerNotFound() {
        CreateCollectionCaseRequest req = new CreateCollectionCaseRequest(
                customerId, "ACC-987654", new BigDecimal("15000.5000"), "INR",
                LocalDate.now().plusDays(10), Priority.HIGH, null
        );

        when(customerRepository.existsById(customerId)).thenReturn(false);

        assertThatThrownBy(() -> caseService.createCase(req))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(caseRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update case to RESOLVED and set resolved timestamp")
    void updateCase_Resolve_SetsTimestamp() {
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(sampleCase));
        when(caseRepository.save(any(CollectionCase.class))).thenReturn(sampleCase);

        UpdateCollectionCaseRequest updateReq = new UpdateCollectionCaseRequest(
                CollectionCaseStatus.RESOLVED, null, BigDecimal.ZERO, null, "Paid in full"
        );

        CollectionCaseResponse res = caseService.updateCase(caseId, updateReq);

        assertThat(sampleCase.getStatus()).isEqualTo(CollectionCaseStatus.RESOLVED);
        assertThat(sampleCase.getResolvedAt()).isNotNull();
        verify(outboxService).publishEvent(eq("CollectionCase"), eq(caseId.toString()), eq("COLLECTION_CASE_UPDATED"), any());
    }
}
