package com.banking.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

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
    public static BalanceOperationResponse success(
            UUID transactionId, UUID sagaId, String accountNo,
            BigDecimal previousBalance, BigDecimal newBalance,
            BigDecimal reservedBalance, String message) {
        return BalanceOperationResponse.builder()
                .success(true)
                .transactionId(transactionId)
                .sagaId(sagaId)
                .accountNo(accountNo)
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .reservedBalance(reservedBalance)
                .availableBalance(newBalance.subtract(reservedBalance))
                .message(message)
                .build();
    }

    public static BalanceOperationResponse failure(
            UUID transactionId, UUID sagaId, String accountNo,
            BigDecimal currentBalance, BigDecimal reservedBalance, String message) {
        return BalanceOperationResponse.builder()
                .success(false)
                .transactionId(transactionId)
                .sagaId(sagaId)
                .accountNo(accountNo)
                .previousBalance(currentBalance)
                .newBalance(currentBalance)
                .reservedBalance(reservedBalance)
                .availableBalance(currentBalance.subtract(reservedBalance))
                .message(message)
                .build();
    }
}