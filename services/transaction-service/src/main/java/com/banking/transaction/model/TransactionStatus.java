package com.banking.transaction.model;

/**
 * Lifecycle states of a financial transaction.
 */
public enum TransactionStatus {
    INITIATED,
    PENDING,
    COMPLETED,
    REJECTED_FRAUD,
    FAILED
}
