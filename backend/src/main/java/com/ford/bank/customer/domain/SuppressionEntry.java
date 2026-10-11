package com.ford.bank.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Suppression entry prevents communication to a specific contact on a specific channel.
 * normalizedContact stores the canonical form (lowercase email, E.164 phone, etc.)
 * for reliable matching regardless of how the contact was originally stored.
 */
@Entity
@Table(name = "suppression_entries",
       uniqueConstraints = @UniqueConstraint(name = "uq_suppression", columnNames = {"normalized_contact", "channel"}))
@Getter
@Setter
@NoArgsConstructor
public class SuppressionEntry {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "normalized_contact", nullable = false, length = 320)
    private String normalizedContact;

    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "reason", nullable = false, length = 100)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public SuppressionEntry(String normalizedContact, String channel, String reason) {
        this.normalizedContact = normalizedContact;
        this.channel = channel;
        this.reason = reason;
    }

    public boolean isActive() {
        return expiresAt == null || expiresAt.isAfter(OffsetDateTime.now());
    }
}
