package com.banking.transaction.dto;

import com.banking.transaction.model.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record TransactionResponse(
        boolean status,
        UUID transactionId,
        UUID userId,
        String referenceNo,
        String accountNo,
        TransactionType type,
        BigDecimal amount,
        BigDecimal remainingAmount,
        String description,
        Instant transactionDate
) {
}
