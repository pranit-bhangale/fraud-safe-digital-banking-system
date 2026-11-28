package com.banking.payment.dto;

import com.banking.payment.model.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Schema(description = "Payment order status and settlement details")
public record PaymentOrderResponse(
        @Schema(description = "Internal payment order UUID", example = "880e8400-e29b-41d4-a716-446655440003")
        UUID id,

        @Schema(description = "Public payment order reference", example = "ORD-1732737600000-ABCDEF12")
        String orderReference,

        @Schema(description = "Merchant unique UUID", example = "990e8400-e29b-41d4-a716-446655440004")
        UUID merchantId,

        @Schema(description = "Customer account number", example = "ACC-1234567890")
        String customerAccountNo,

        @Schema(description = "Order amount", example = "99.99")
        BigDecimal amount,

        @Schema(description = "Order currency", example = "USD")
        String currency,

        @Schema(description = "Order settlement status (CREATED, PROCESSING, COMPLETED, FAILED)", example = "COMPLETED")
        PaymentStatus status,

        @Schema(description = "Associated transaction settlement UUID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID transactionId,

        @Schema(description = "Failure explanation if payment failed")
        String failureReason,

        @Schema(description = "Timestamp when order was placed")
        Instant createdAt
) {
}
