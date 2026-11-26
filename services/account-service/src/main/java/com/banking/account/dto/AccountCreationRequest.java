package com.banking.account.dto;

import com.banking.account.model.AccountType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record AccountCreationRequest(
        @NotNull(message = "User ID is required")
        UUID userId,

        @NotNull(message = "Account type is required")
        AccountType type,

        @NotNull(message = "Initial balance is required")
        @PositiveOrZero(message = "Balance cannot be negative")
        @Digits(integer = 15, fraction = 2, message = "Balance must have at most 15 integer digits and 2 decimal places")
        BigDecimal balance,

        String currency,

        String idempotencyKey
) {
    public AccountCreationRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
