package com.ford.bank.communication.repository;

import com.ford.bank.communication.domain.CommunicationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationAttemptRepository extends JpaRepository<CommunicationAttempt, UUID> {

    List<CommunicationAttempt> findByJobIdOrderByAttemptNumberDesc(UUID jobId);

    Optional<CommunicationAttempt> findByProviderReference(String providerReference);

    int countByJobId(UUID jobId);

    Optional<CommunicationAttempt> findByJobIdAndAttemptNumber(UUID jobId, int attemptNumber);
}
