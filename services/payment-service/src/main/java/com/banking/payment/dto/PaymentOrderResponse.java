package com.banking.payment.dto;

import com.banking.payment.model.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PaymentOrderResponse(
        UUID id,
        String orderReference,
        UUID merchantId,
        String customerAccountNo,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        UUID transactionId,
        String failureReason,
        Instant createdAt
) {
}
