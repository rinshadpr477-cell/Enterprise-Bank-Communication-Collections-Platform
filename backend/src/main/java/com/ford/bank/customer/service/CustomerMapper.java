package com.ford.bank.customer.service;

import com.ford.bank.customer.domain.Customer;
import com.ford.bank.customer.dto.CustomerResponse;
import org.springframework.stereotype.Component;

/**
 * Maps between Customer domain entities and DTOs.
 * Phone numbers are masked for data minimization in API responses.
 */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCustomerReference(),
                customer.getFullName(),
                customer.getEmail(),
                maskPhone(customer.getPhoneNumber()),
                customer.getPreferredLanguage(),
                customer.getStatus(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    /**
     * Masks all but the last 4 digits of a phone number.
     * E.g., +919876543210 -> +91XXXXXX3210
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() <= 4) return phone;
        String visible = phone.substring(phone.length() - 4);
        String prefix = phone.substring(0, Math.min(3, phone.length() - 4));
        return prefix + "XXXXXX" + visible;
    }
}
