package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionType;
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
public record TransactionRequest(

        UUID userId,
        @NotBlank(message = "Sender account number is required")
        String senderAccountNo,
        String receiverAccountNo,
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be strictly greater than zero")
        BigDecimal amount,
        TransactionType transactionType,
        String accountType,
        String description
) {
}