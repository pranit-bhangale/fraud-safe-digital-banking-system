package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response representing the state of a transaction.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Transaction execution and status response")
public record TransactionResponse(
        @Schema(description = "Whether the request was accepted or succeeded", example = "true")
        boolean status,

        @Schema(description = "Transaction unique UUID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID transactionId,

        @Schema(description = "Customer owner UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,

        @Schema(description = "Unique human-readable transaction reference number", example = "TXN-1732737600000-ABCDEF12")
        String referenceNo,

        @Schema(description = "Sender account number", example = "ACC-1234567890")
        String senderAccountNo,

        @Schema(description = "Receiver account number", example = "ACC-0987654321")
        String receiverAccountNo,

        @Schema(description = "Transaction type", example = "TRANSFER")
        TransactionType type,

        @Schema(description = "Current lifecycle status (PENDING, COMPLETED, REJECTED_FRAUD, FAILED)", example = "COMPLETED")
        TransactionStatus transactionStatus,

        @Schema(description = "Transaction amount", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Sender account balance following transaction completion", example = "750.00")
        BigDecimal balanceAfter,

        @Schema(description = "Real-time AI fraud risk score (0-100)", example = "15")
        Integer fraudScore,

        @Schema(description = "Transaction memo", example = "Monthly rent payment")
        String description,

        @Schema(description = "Informational message", example = "Transaction accepted for processing")
        String message,

        @Schema(description = "Timestamp when transaction was recorded")
        Instant transactionDate
) {
}
