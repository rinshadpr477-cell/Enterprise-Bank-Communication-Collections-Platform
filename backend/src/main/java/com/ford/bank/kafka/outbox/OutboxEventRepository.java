package com.ford.bank.kafka.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Claim unpublished records for this publisher instance.
     * Uses pessimistic locking via FOR UPDATE SKIP LOCKED to allow
     * concurrent publisher instances to work on different records safely.
     * Also reclaims stale locks from crashed instances (lock timeout).
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE published_at IS NULL
        AND (locked_by IS NULL OR locked_at < :lockExpiry)
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEvent> findAndClaimUnpublished(
            @Param("lockExpiry") OffsetDateTime lockExpiry,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
        UPDATE OutboxEvent o SET o.lockedBy = :instanceId, o.lockedAt = :now
        WHERE o.id IN :ids AND o.publishedAt IS NULL
        """)
    void lockForPublisher(
            @Param("ids") List<UUID> ids,
            @Param("instanceId") String instanceId,
            @Param("now") OffsetDateTime now
    );

    @Query("SELECT COUNT(o) FROM OutboxEvent o WHERE o.publishedAt IS NULL")
    long countUnpublished();
}
