package com.banking.fraud.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Event(
        PaymentRequest request,
        PaymentApproved approved,
        FraudEvent event
) {
    /**
     * Input: topic payment.initiated (Kafka). Key = userId.
     */
    @Builder
    public record PaymentRequest(
            String txnId,
            String userId,
            BigDecimal amount,
            String currency,
            String cardToken,
            String deviceId,
            String ipAddress,
            String merchantId,
            long createdAtMs) {
    }

    /**
     * Output when SAFE: topic payment.approved (Kafka). Consumed by the payment service.
     */
    @Builder
    public record PaymentApproved(
            String txnId,
            String userId,
            BigDecimal amount,
            String currency,
            String merchantId,
            int riskScore,
            long approvedAtMs) {
    }

    /**
     * Output when FRAUD: topic fraud.detected (Kafka). Audit trail + notification relay.
     */
    @Builder
    public record FraudEvent(
            String eventId,
            String txnId,
            String userId,
            BigDecimal amount,
            String currency,
            int riskScore,
            List<String> reasons,
            long detectedAtMs) {
    }
}
