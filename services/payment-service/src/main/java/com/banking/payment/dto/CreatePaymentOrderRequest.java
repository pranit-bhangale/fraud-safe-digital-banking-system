package com.banking.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Schema(description = "Merchant payment order creation payload")
public record CreatePaymentOrderRequest(
        @Schema(description = "Merchant unique UUID", example = "990e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @Schema(description = "Customer checking/debit account number to charge", example = "ACC-1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Customer account number is required")
        String customerAccountNo,

        @Schema(description = "Payment order amount", example = "99.99", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        @Schema(description = "Payment currency code (3-letter ISO)", example = "USD", defaultValue = "USD")
        String currency,

        @Schema(description = "Client idempotency key", example = "pay-idem-990e8400-e29b-41d4-a716-446655440004", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
    public CreatePaymentOrderRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
