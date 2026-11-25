package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * API response representing the state of a transaction.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionResponse(
        boolean status,
        UUID transactionId,
        UUID userId,
        String referenceNo,
        String senderAccountNo,
        String receiverAccountNo,
        TransactionType type,
        TransactionStatus transactionStatus,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Integer fraudScore,
        String description,
        String message,
        Instant transactionDate
) {
}
