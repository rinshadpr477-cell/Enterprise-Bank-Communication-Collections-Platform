package com.ford.bank.customer.repository;

import com.ford.bank.customer.domain.ContactPreference;
import com.ford.bank.customer.domain.CommunicationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactPreferenceRepository extends JpaRepository<ContactPreference, UUID> {

    List<ContactPreference> findByCustomerId(UUID customerId);

    Optional<ContactPreference> findByCustomerIdAndChannel(UUID customerId, CommunicationChannel channel);

    boolean existsByCustomerIdAndChannelAndPermitted(UUID customerId, CommunicationChannel channel, boolean permitted);
}
