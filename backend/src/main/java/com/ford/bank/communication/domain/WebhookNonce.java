package com.ford.bank.communication.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "webhook_nonces")
@Getter
@Setter
@NoArgsConstructor
public class WebhookNonce {

    @Id
    @Column(name = "nonce", nullable = false, length = 255)
    private String nonce;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    public WebhookNonce(String nonce, String provider, OffsetDateTime expiresAt) {
        this.nonce = nonce;
        this.provider = provider;
        this.receivedAt = OffsetDateTime.now();
        this.expiresAt = expiresAt;
    }
}
