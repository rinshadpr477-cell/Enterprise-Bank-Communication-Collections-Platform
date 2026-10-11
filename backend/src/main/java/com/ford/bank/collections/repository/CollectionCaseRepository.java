package com.ford.bank.collections.repository;

import com.ford.bank.collections.domain.CollectionCase;
import com.ford.bank.collections.domain.CollectionCaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CollectionCaseRepository extends JpaRepository<CollectionCase, UUID> {

    List<CollectionCase> findByCustomerId(UUID customerId);

    java.util.Optional<CollectionCase> findByExternalAccountReference(String externalAccountReference);

    Page<CollectionCase> findByStatus(CollectionCaseStatus status, Pageable pageable);

    Page<CollectionCase> findByAssignedAgentId(String assignedAgentId, Pageable pageable);

    Page<CollectionCase> findByAssignedAgentIdAndStatus(String assignedAgentId, CollectionCaseStatus status, Pageable pageable);

    @Query("SELECT c FROM CollectionCase c WHERE c.status = 'OPEN' AND c.dueDate <= :dateThreshold")
    List<CollectionCase> findOverdueOpenCases(@Param("dateThreshold") LocalDate dateThreshold);
}
