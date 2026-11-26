package com.banking.transaction.saga.step;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.dto.BalanceOperationRequest;
import com.banking.transaction.dto.BalanceOperationResponse;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.saga.SagaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Step 3: Update balances after fraud approval.
 * - Debit sender (move from reserved to actual debit)
 * - Credit receiver (for TRANSFER type)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBalanceStep {

    private final AccountClient accountClient;

    public void execute(SagaContext context, Event.PaymentApproved approved) {
        log.info("Updating balances after fraud approval: txnId={}, sagaId={}, type={}",
                context.referenceNo(), context.sagaId(), context.transactionType());

        // Debit sender (move from reserved to actual)
        debitSender(context);

        // Credit receiver for TRANSFER type
        if (context.transactionType() == TransactionType.TRANSFER && context.receiverAccountNo() != null) {
            creditReceiver(context);
        }

        log.info("Balances updated successfully: txnId={}, sagaId={}", context.referenceNo(), context.sagaId());
    }

    private void debitSender(SagaContext context) {
        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .transactionId(context.transactionId())
                .sagaId(context.sagaId())
                .amount(context.amount())
                .description("Debit for " + context.transactionType().name())
                .build();

        BalanceOperationResponse response = accountClient.debitBalance(context.senderAccountNo(), request);

        if (!response.success()) {
            throw new UpdateBalanceException("Failed to debit sender: " + response.message());
        }

        log.info("Sender debited: txnId={}, sagaId={}, account={}, newBalance={}",
                context.referenceNo(), context.sagaId(), context.senderAccountNo(), response.newBalance());
    }

    private void creditReceiver(SagaContext context) {
        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .transactionId(context.transactionId())
                .sagaId(context.sagaId())
                .amount(context.amount())
                .description("Credit for TRANSFER from " + context.senderAccountNo())
                .build();

        BalanceOperationResponse response = accountClient.creditBalance(context.receiverAccountNo(), request);

        if (!response.success()) {
            throw new UpdateBalanceException("Failed to credit receiver: " + response.message());
        }

        log.info("Receiver credited: txnId={}, sagaId={}, account={}, newBalance={}",
                context.referenceNo(), context.sagaId(), context.receiverAccountNo(), response.newBalance());
    }

    public static class UpdateBalanceException extends RuntimeException {
        public UpdateBalanceException(String message) {
            super(message);
        }
    }
}