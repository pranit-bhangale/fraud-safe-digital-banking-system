package com.banking.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Response payload for balance operations from account-service.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BalanceOperationResponse(
        boolean success,
        UUID transactionId,
        UUID sagaId,
        String accountNo,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        BigDecimal reservedBalance,
        BigDecimal availableBalance,
        String message
) {
}