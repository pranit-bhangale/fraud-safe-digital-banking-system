package com.banking.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record BalanceOperationRequest(
        @NotNull(message = "Transaction ID is required")
        UUID transactionId,

        @NotNull(message = "Saga ID is required")
        UUID sagaId,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,

        String description
) {
}