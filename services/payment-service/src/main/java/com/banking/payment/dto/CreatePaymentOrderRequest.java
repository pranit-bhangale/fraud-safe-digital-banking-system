package com.banking.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record CreatePaymentOrderRequest(
        @NotNull(message = "Merchant ID is required")
        UUID merchantId,

        @NotBlank(message = "Customer account number is required")
        String customerAccountNo,

        @NotNull(message = "Payment amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        String currency,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
    public CreatePaymentOrderRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
