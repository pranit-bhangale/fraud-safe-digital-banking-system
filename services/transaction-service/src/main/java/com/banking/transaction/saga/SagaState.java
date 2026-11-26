package com.banking.transaction.saga;

/**
 * Saga state machine states for transaction processing.
 * Flow: STARTED -> RESERVED -> FRAUD_CHECK -> (APPROVED -> BALANCE_UPDATED -> COMPLETED) | (REJECTED/ERROR -> COMPENSATING -> ROLLED_BACK)
 */
public enum SagaState {
    STARTED,
    RESERVED,
    FRAUD_CHECK,
    BALANCE_UPDATED,
    COMPLETED,
    COMPENSATING,
    ROLLED_BACK,
    FAILED
}