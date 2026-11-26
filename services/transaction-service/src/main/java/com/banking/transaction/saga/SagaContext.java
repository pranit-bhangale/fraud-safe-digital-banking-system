package com.banking.transaction.saga;

import com.banking.transaction.entity.Saga;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.model.TransactionType;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable context carrying all data needed for saga execution.
 * Data sourced from Transaction entity via FK - no duplication.
 */
@Builder
public record SagaContext(
        UUID sagaId,
        UUID transactionId,
        String referenceNo,
        UUID userId,
        String senderAccountNo,
        String receiverAccountNo,
        TransactionType transactionType,
        BigDecimal amount,
        String accountType,
        String description,
        SagaState currentState,
        long startedAt,
        long updatedAt
) {

    public SagaContext withState(SagaState newState) {
        return new SagaContext(
                sagaId, transactionId, referenceNo, userId,
                senderAccountNo, receiverAccountNo, transactionType,
                amount, accountType, description,
                newState, startedAt, System.currentTimeMillis()
        );
    }

    public static SagaContext fromTransactionAndSaga(Transaction transaction, Saga saga) {
        return new SagaContext(
                saga.getId(),
                transaction.getId(),
                transaction.getReferenceNo(),
                transaction.getUserId(),
                transaction.getAccountNo(),
                transaction.getReceiverAccountNo(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getAccountType(),
                transaction.getDescription(),
                saga.getSagaState(),
                saga.getCreatedAt().toEpochMilli(),
                System.currentTimeMillis()
        );
    }
}