package com.ford.bank.communication.repository;

import com.ford.bank.communication.domain.WebhookNonce;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookNonceRepository extends JpaRepository<WebhookNonce, String> {
}
