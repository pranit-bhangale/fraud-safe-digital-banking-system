package com.banking.fraud.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

/**
 * Enterprise Banking Event Envelope for Account-to-Account Transactions.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Event(
        PaymentRequest request,
        PaymentApproved approved,
        FraudEvent event
) {
    /**
     * Inbound: topic banking.payment.initiated.v1. Key = senderAccountNo.
     */
    @Builder
    public record PaymentRequest(
            String txnId,
            String senderAccountNo,
            String receiverAccountNo,
            BigDecimal amount,
            String accountType,
            long createdAtMs) {
    }

    /**
     * Outbound when SAFE: topic banking.payment.fraud-ack.v1.
     */
    @Builder
    public record PaymentApproved(
            String txnId,
            String senderAccountNo,
            String receiverAccountNo,
            BigDecimal amount,
            String accountType,
            int riskScore,
            long approvedAtMs) {
    }

    /**
     * Outbound when FRAUD or ERROR: topic banking.payment.fraud-ack.v1 and banking.fraud.alerts.v1.
     */
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
            long detectedAtMs) {
    }
}
