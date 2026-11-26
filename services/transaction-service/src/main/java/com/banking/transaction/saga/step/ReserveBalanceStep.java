package com.banking.transaction.saga.step;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.BalanceOperationRequest;
import com.banking.transaction.dto.BalanceOperationResponse;
import com.banking.transaction.saga.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Step 1: Reserve sender balance for the transaction.
 * Calls Account Service to reserve the amount.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReserveBalanceStep {

    private final AccountClient accountClient;

    public void execute(SagaContext context) {
        log.info("Reserving balance: txnId={}, sagaId={}, account={}, amount={}",
                context.referenceNo(), context.sagaId(), context.senderAccountNo(), context.amount());
        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .transactionId(context.transactionId())
                .sagaId(context.sagaId())
                .amount(context.amount())
                .description("Reserve for " + context.transactionType().name())
                .build();
        BalanceOperationResponse response = accountClient.reserveBalance(context.senderAccountNo(), request);
        if (!response.success()) {
            throw new ReserveBalanceException("Failed to reserve balance: " + response.message());
        }
        log.info("Balance reserved successfully: txnId={}, sagaId={}, newAvailableBalance={}",
                context.referenceNo(), context.sagaId(), response.availableBalance());
    }

    public static class ReserveBalanceException extends RuntimeException {
        public ReserveBalanceException(String message) {
            super(message);
        }
    }
}