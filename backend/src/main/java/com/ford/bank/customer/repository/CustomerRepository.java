package com.ford.bank.customer.repository;

import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.domain.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByCustomerReference(String customerReference);

    boolean existsByCustomerReference(String customerReference);

    boolean existsByEmail(String email);

    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    @Query("""
        SELECT c FROM Customer c
        WHERE (:status IS NULL OR c.status = :status)
        AND (:search IS NULL OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
             OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<Customer> findWithFilters(
            @Param("status") CustomerStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
