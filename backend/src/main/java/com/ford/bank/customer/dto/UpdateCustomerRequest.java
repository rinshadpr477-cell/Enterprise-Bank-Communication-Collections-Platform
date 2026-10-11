package com.ford.bank.customer.dto;

import com.ford.bank.customer.domain.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Partial update request for customers. All fields are optional.
 * null means "no change"; only non-null fields are updated.
 */
public record UpdateCustomerRequest(

        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Email(message = "Email must be a valid address")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164 format")
        String phoneNumber,

        @Size(max = 10)
        String preferredLanguage,

        CustomerStatus status
) {}
