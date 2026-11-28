package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Inbound payload for initiating a financial transaction.
 */
@Builder
@Schema(description = "Transaction initiation payload")
public record TransactionRequest(

        @Schema(description = "Initiating customer UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Sender account number", example = "ACC-1234567890", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Sender account number is required")
        String senderAccountNo,

        @Schema(description = "Receiver account number (required for TRANSFER)", example = "ACC-0987654321")
        String receiverAccountNo,

        @Schema(description = "Transaction amount", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,

        @Schema(description = "Transaction type (e.g. TRANSFER, DEPOSIT, WITHDRAWAL)", example = "TRANSFER")
        TransactionType transactionType,

        @Schema(description = "Source account type", example = "CHECKING")
        String accountType,

        @Schema(description = "Transaction memo / description", example = "Monthly rent payment")
        String description
) {
}
