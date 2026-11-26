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

/**
 * Step: Update balances after fraud approval in the Saga pipeline.
 * - Deducts from sender directly.
 * - Credits receiver (for TRANSFER / PAYMENT types).
 * - Executes automated compensating refund if receiver credit fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBalanceStep {

    private final AccountClient accountClient;

    public void execute(SagaContext context, Event.PaymentApproved approved) {
        log.info("Executing balance updates following fraud approval: txnId={}, sagaId={}, type={}, amount={}",
                context.referenceNo(), context.sagaId(), context.transactionType(), context.amount());

        // Step 1: Debit sender account
        debitSender(context);

        // Step 2: Credit receiver account (for TRANSFER type)
        if (context.transactionType() == TransactionType.TRANSFER && context.receiverAccountNo() != null) {
            try {
                creditReceiver(context);
            } catch (Exception ex) {
                log.error("Receiver credit failed for txnId={}. Initiating automatic compensation to refund sender {}: {}",
                        context.referenceNo(), context.senderAccountNo(), ex.getMessage());
                compensateDebit(context);
                throw new UpdateBalanceException("Receiver credit failed; sender balance has been refunded: " + ex.getMessage());
            }
        }

        log.info("Balances updated successfully for txnId={}, sagaId={}", context.referenceNo(), context.sagaId());
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

        log.info("Sender debited successfully: txnId={}, sagaId={}, account={}, newBalance={}",
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

        log.info("Receiver credited successfully: txnId={}, sagaId={}, account={}, newBalance={}",
                context.referenceNo(), context.sagaId(), context.receiverAccountNo(), response.newBalance());
    }

    public void compensateDebit(SagaContext context) {
        log.warn("Executing compensation: Refunding sender balance: txnId={}, sagaId={}, account={}, amount={}",
                context.referenceNo(), context.sagaId(), context.senderAccountNo(), context.amount());

        BalanceOperationRequest request = BalanceOperationRequest.builder()
                .transactionId(context.transactionId())
                .sagaId(context.sagaId())
                .amount(context.amount())
                .description("Compensating refund for failed transfer " + context.referenceNo())
                .build();

        BalanceOperationResponse response = accountClient.creditBalance(context.senderAccountNo(), request);

        if (!response.success()) {
            log.error("CRITICAL SAGA FAILURE: Failed to refund sender {} for txnId={}: {}",
                    context.senderAccountNo(), context.referenceNo(), response.message());
            throw new UpdateBalanceException("Critical: Failed to compensate sender debit: " + response.message());
        }

        log.info("Sender refunded successfully: account={}, newBalance={}",
                context.senderAccountNo(), response.newBalance());
    }

    public static class UpdateBalanceException extends RuntimeException {
        public UpdateBalanceException(String message) {
            super(message);
        }
    }
}
