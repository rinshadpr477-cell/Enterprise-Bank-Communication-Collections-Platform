package com.ford.bank.customer.dto;

import com.ford.bank.customer.domain.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable record for customer creation requests.
 * Bean Validation ensures data quality before reaching the service layer.
 */
public record CreateCustomerRequest(

        @NotBlank(message = "Customer reference is required")
        @Size(min = 3, max = 50, message = "Customer reference must be 3-50 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Customer reference must be alphanumeric uppercase")
        String customerReference,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @Email(message = "Email must be a valid address")
        @Size(max = 320, message = "Email must not exceed 320 characters")
        String email,

        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164 format")
        String phoneNumber,

        @Size(max = 10, message = "Preferred language code must not exceed 10 characters")
        String preferredLanguage
) {}
