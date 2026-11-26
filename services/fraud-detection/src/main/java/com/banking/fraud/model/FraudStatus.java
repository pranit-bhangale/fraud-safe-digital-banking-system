package com.banking.fraud.model;

/**
 * Standard fraud evaluation outcomes for account transactions.
 */
public enum FraudStatus {
    /**
     * Transaction is safe and approved for settlement.
     */
    SAFE,

    /**
     * Transaction exceeds fraud risk thresholds and is blocked.
     */
    FRAUD,

    /**
     * System error or malformed request during evaluation.
     */
    ERROR
}
