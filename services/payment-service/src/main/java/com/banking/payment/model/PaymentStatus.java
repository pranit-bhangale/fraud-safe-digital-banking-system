package com.banking.payment.model;

public enum PaymentStatus {
    CREATED,
    FRAUD_CHECK,
    PROCESSING,
    COMPLETED,
    DECLINED_FRAUD,
    FAILED
}
