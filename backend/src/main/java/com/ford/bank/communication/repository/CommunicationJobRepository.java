package com.ford.bank.communication.repository;

import com.ford.bank.communication.domain.CommunicationJob;
import com.ford.bank.communication.domain.JobStatus;
import com.ford.bank.customer.domain.CommunicationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationJobRepository extends JpaRepository<CommunicationJob, UUID> {

    Optional<CommunicationJob> findByIdempotencyKey(String idempotencyKey);

    Page<CommunicationJob> findByCustomerId(UUID customerId, Pageable pageable);

    Page<CommunicationJob> findByCampaignId(UUID campaignId, Pageable pageable);

    Page<CommunicationJob> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Claim a bounded batch of due jobs using SKIP LOCKED for concurrent safety.
     * SKIP LOCKED means concurrent scheduler instances will not conflict;
     * they each get a non-overlapping set of rows.
     * This prevents duplicate processing without application-level locks.
     */
    @Query(value = """
        SELECT * FROM communication_jobs
        WHERE status IN ('PENDING', 'RETRYING')
        AND (scheduled_at IS NULL OR scheduled_at <= :now)
        AND (next_retry_at IS NULL OR next_retry_at <= :now)
        ORDER BY scheduled_at ASC NULLS FIRST, id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<CommunicationJob> claimDueJobs(
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query("""
        UPDATE CommunicationJob j SET j.status = :status, j.updatedAt = :now
        WHERE j.id = :id AND j.status = :currentStatus
        """)
    int updateStatusIfCurrent(
            @Param("id") UUID id,
            @Param("currentStatus") JobStatus currentStatus,
            @Param("status") JobStatus status,
            @Param("now") OffsetDateTime now
    );

    @Query("""
        SELECT j FROM CommunicationJob j
        WHERE (:status IS NULL OR j.status = :status)
        AND (:channel IS NULL OR j.channel = :channel)
        AND (:customerId IS NULL OR j.customerId = :customerId)
        AND (:campaignId IS NULL OR j.campaignId = :campaignId)
        """)
    Page<CommunicationJob> findWithFilters(
            @Param("status") JobStatus status,
            @Param("channel") CommunicationChannel channel,
            @Param("customerId") UUID customerId,
            @Param("campaignId") UUID campaignId,
            Pageable pageable
    );
}
