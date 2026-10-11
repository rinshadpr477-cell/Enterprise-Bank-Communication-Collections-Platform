package com.ford.bank.customer.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Customer entity - root aggregate for customer information.
 * All timestamps are stored in UTC (TIMESTAMPTZ in PostgreSQL).
 * This entity is never exposed directly via API; use CustomerResponse DTOs.
 */
@Entity
@Table(name = "customers")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "customer_reference", nullable = false, unique = true, length = 50)
    private String customerReference;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "preferred_language", nullable = false, length = 10)
    private String preferredLanguage = "en";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Customer(String customerReference, String fullName, String email,
                    String phoneNumber, String preferredLanguage) {
        this.customerReference = customerReference;
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.preferredLanguage = preferredLanguage != null ? preferredLanguage : "en";
    }

    public boolean isActive() {
        return CustomerStatus.ACTIVE == this.status;
    }
}
