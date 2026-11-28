package com.banking.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a balance mutation or reservation operation")
public record BalanceOperationResponse(
        @Schema(description = "Whether the balance operation succeeded", example = "true")
        boolean success,

        @Schema(description = "Transaction UUID", example = "660e8400-e29b-41d4-a716-446655440001")
        UUID transactionId,

        @Schema(description = "Saga UUID", example = "770e8400-e29b-41d4-a716-446655440002")
        UUID sagaId,

        @Schema(description = "Target account number", example = "ACC-1234567890")
        String accountNo,

        @Schema(description = "Account balance prior to operation", example = "1000.00")
        BigDecimal previousBalance,

        @Schema(description = "Account balance after operation", example = "750.00")
        BigDecimal newBalance,

        @Schema(description = "Current reserved balance", example = "0.00")
        BigDecimal reservedBalance,

        @Schema(description = "Available spendable balance", example = "750.00")
        BigDecimal availableBalance,

        @Schema(description = "Status or failure message", example = "Balance operation executed successfully")
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
