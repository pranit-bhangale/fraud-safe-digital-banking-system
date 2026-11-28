package com.banking.account.dto;

import com.banking.account.model.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Schema(description = "Bank account opening request payload")
public record AccountCreationRequest(
        @Schema(description = "Customer owner UUID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "User ID is required")
        UUID userId,

        @Schema(description = "Type of bank account", example = "CHECKING", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Account type is required")
        AccountType type,

        @Schema(description = "Initial deposit amount (must meet minimum for account type)", example = "1000.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Initial balance is required")
        @PositiveOrZero(message = "Balance cannot be negative")
        @Digits(integer = 15, fraction = 2, message = "Balance must have at most 15 integer digits and 2 decimal places")
        BigDecimal balance,

        @Schema(description = "Currency code (3-letter ISO)", example = "USD", defaultValue = "USD")
        String currency,

        @Schema(description = "Client idempotency key", example = "idem-550e8400-e29b-41d4-a716-446655440000")
        String idempotencyKey
) {
    public AccountCreationRequest {
        if (currency == null || currency.isBlank()) {
            currency = "USD";
        }
    }
}
