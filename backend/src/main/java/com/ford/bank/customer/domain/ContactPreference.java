package com.ford.bank.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contact_preferences",
       uniqueConstraints = @UniqueConstraint(name = "uq_contact_pref", columnNames = {"customer_id", "channel"}))
@Getter
@Setter
@NoArgsConstructor
public class ContactPreference {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private CommunicationChannel channel;

    @Column(name = "permitted", nullable = false)
    private boolean permitted = true;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public ContactPreference(UUID customerId, CommunicationChannel channel, boolean permitted) {
        this.customerId = customerId;
        this.channel = channel;
        this.permitted = permitted;
    }
}
