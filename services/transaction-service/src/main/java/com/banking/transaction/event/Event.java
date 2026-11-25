package com.banking.transaction.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Canonical event envelope for fraud detection and payment processing.
 * Exactly aligned with the fraud-detection microservice contract.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Event(
        PaymentRequest request,
        PaymentApproved approved,
        FraudEvent event
) {

    @Builder
    public record PaymentRequest(
            String txnId,
            String senderAccountNo,
            String receiverAccountNo,
            BigDecimal amount,
            String accountType,
            long createdAtMs
    ) {
    }

    @Builder
    public record PaymentApproved(
            String txnId,
            String senderAccountNo,
            String receiverAccountNo,
            BigDecimal amount,
            String accountType,
            int riskScore,
            long approvedAtMs
    ) {
    }

    @Builder
    public record FraudEvent(
            String eventId,
            String txnId,
            String senderAccountNo,
            String receiverAccountNo,
            BigDecimal amount,
            String accountType,
            int riskScore,
            List<String> reasons,
            String action,
            long detectedAtMs
    ) {
    }
}
