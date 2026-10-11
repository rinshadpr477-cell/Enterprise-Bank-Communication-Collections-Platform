package com.ford.bank.customer.repository;

import com.ford.bank.customer.domain.SuppressionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface SuppressionRepository extends JpaRepository<SuppressionEntry, UUID> {

    /**
     * Check if a contact is suppressed for a specific channel OR via the ALL channel.
     * This is called immediately before dispatching any communication.
     */
    @Query("""
        SELECT s FROM SuppressionEntry s
        WHERE s.normalizedContact = :contact
        AND s.channel IN (:channel, 'ALL')
        AND (s.expiresAt IS NULL OR s.expiresAt > :now)
        """)
    Optional<SuppressionEntry> findActiveSuppressionForContact(
            @Param("contact") String normalizedContact,
            @Param("channel") String channel,
            @Param("now") OffsetDateTime now
    );

    Optional<SuppressionEntry> findByNormalizedContactAndChannel(String normalizedContact, String channel);
}
