package com.ford.bank.customer.domain;

/**
 * Customer lifecycle states.
 * DECEASED requires special handling - suppress all communications.
 */
public enum CustomerStatus {
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    DECEASED
}
