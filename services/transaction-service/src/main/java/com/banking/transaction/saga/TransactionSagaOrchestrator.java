package com.banking.transaction.saga;

import com.banking.transaction.entity.Saga;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.producer.TransactionProducer;
import com.banking.transaction.repository.SagaRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.saga.step.UpdateBalanceStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Orchestrates the saga for transaction processing with FRAUD DETECTION RUNNING FIRST.
 * Step 1: Submit transaction to Fraud Service before locking or touching any account balances.
 * Step 2: If Fraud rejects -> Terminate immediately as REJECTED_FRAUD (zero balance impact).
 * Step 3: If Fraud approves -> Forward balance operations (debit sender, credit receiver).
 * Step 4: If receiver credit fails -> Compensating rollback (refund sender).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final SagaRepository sagaRepository;
    private final TransactionProducer transactionProducer;
    private final UpdateBalanceStep updateBalanceStep;

    /**
     * Starts the saga for a new transaction.
     * Fraud detection runs FIRST - no balance is reserved yet.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void startSaga(Transaction transaction) {
        Saga saga = Saga.builder()
                .transaction(transaction)
                .sagaState(SagaState.FRAUD_CHECK)
                .build();
        saga = sagaRepository.save(saga);
        SagaContext context = SagaContext.fromTransactionAndSaga(transaction, saga);

        log.info("Starting saga: Fraud evaluation running FIRST for txnId={}, sagaId={}, sender={}, amount={}",
                transaction.getReferenceNo(), saga.getId(), transaction.getAccountNo(), transaction.getAmount());

        submitToFraudService(context);
    }

    /**
     * Handles fraud approval - proceeds with balance updates (debit sender, credit receiver).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudApproved(Event.PaymentApproved approved) {
        String txnId = approved.txnId();
        log.info("Fraud approved for txnId: {}, proceeding to execute balance updates", txnId);

        Transaction transaction = transactionRepository.findByReferenceNo(txnId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));

        Saga saga = sagaRepository.findByTransaction(transaction)
                .orElseThrow(() -> new IllegalStateException("Saga not found for txnId: " + txnId));

        if (!SagaState.FRAUD_CHECK.equals(saga.getSagaState())) {
            log.warn("Invalid saga state for fraud approval: txnId={}, state={}", txnId, saga.getSagaState());
            return;
        }

        SagaContext context = SagaContext.fromTransactionAndSaga(transaction, saga);
        transaction.setFraudScore(approved.riskScore());

        executeUpdateBalanceStep(context, saga, approved);
    }

    /**
     * Handles fraud rejection - cleanly marks transaction as REJECTED_FRAUD.
     * Since fraud detection runs first, no balances were locked or deducted!
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudRejected(Event.FraudEvent fraud) {
        String txnId = fraud.txnId();
        log.warn("Fraud rejected for txnId: {}, terminating transaction cleanly with zero balance impact", txnId);

        Transaction transaction = transactionRepository.findByReferenceNo(txnId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));

        Saga saga = sagaRepository.findByTransaction(transaction)
                .orElseThrow(() -> new IllegalStateException("Saga not found for txnId: " + txnId));

        if (!SagaState.FRAUD_CHECK.equals(saga.getSagaState())) {
            log.warn("Invalid saga state for fraud rejection: txnId={}, state={}", txnId, saga.getSagaState());
            return;
        }

        String rejectionReason = fraud.reasons() != null ? String.join(", ", fraud.reasons()) : "FRAUD_REJECTED";
        transaction.setFraudScore(fraud.riskScore());
        transaction.setRejectionReasons(rejectionReason);
        transaction.setStatus(TransactionStatus.REJECTED_FRAUD);
        transactionRepository.save(transaction);

        saga.setSagaState(SagaState.ROLLED_BACK);
        saga.setErrorMessage(rejectionReason);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);

        log.info("Saga successfully closed for rejected txnId={}. No balance rollback was needed.", txnId);
    }

    /**
     * Handles fraud check timeout or communication error.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudError(String txnId, String errorReason) {
        log.error("Fraud check error for txnId: {}, reason: {}", txnId, errorReason);

        Transaction transaction = transactionRepository.findByReferenceNo(txnId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));

        Saga saga = sagaRepository.findByTransaction(transaction)
                .orElseThrow(() -> new IllegalStateException("Saga not found for txnId: " + txnId));

        transaction.setRejectionReasons("FRAUD_CHECK_ERROR: " + errorReason);
        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);

        saga.setSagaState(SagaState.FAILED);
        saga.setErrorMessage("FRAUD_CHECK_ERROR: " + errorReason);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);
    }

    private void executeUpdateBalanceStep(SagaContext context, Saga saga, Event.PaymentApproved approved) {
        Transaction transaction = transactionRepository.findByReferenceNo(context.referenceNo()).orElseThrow();
        try {
            log.info("Updating balances for txnId={}, sagaId={}", context.referenceNo(), saga.getId());
            updateBalanceStep.execute(context, approved);

            updateSagaState(saga, SagaState.BALANCE_UPDATED);
            completeSaga(transaction, saga);
        } catch (Exception e) {
            log.error("Update balance step failed for txnId={}: {}", context.referenceNo(), e.getMessage(), e);

            saga.setSagaState(SagaState.FAILED);
            saga.setErrorMessage(e.getMessage());
            saga.setCompletedAt(Instant.now());
            sagaRepository.save(saga);

            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRejectionReasons(e.getMessage());
            transactionRepository.save(transaction);
        }
    }

    private void updateSagaState(Saga saga, SagaState newState) {
        saga.setSagaState(newState);
        sagaRepository.save(saga);
    }

    private void submitToFraudService(SagaContext context) {
        Event event = Event.builder()
                .request(Event.PaymentRequest.builder()
                        .txnId(context.referenceNo())
                        .senderAccountNo(context.senderAccountNo())
                        .receiverAccountNo(context.receiverAccountNo() != null ? context.receiverAccountNo() : "N/A")
                        .amount(context.amount())
                        .accountType(context.accountType())
                        .createdAtMs(System.currentTimeMillis())
                        .build())
                .build();

        transactionProducer.publishInitiatedPayment(context.senderAccountNo(), event);
        log.info("Submitted transaction to fraud detection service: txnId={}, sender={}, amount={}",
                context.referenceNo(), context.senderAccountNo(), context.amount());
    }

    private void completeSaga(Transaction transaction, Saga saga) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setBalanceAfter(calculateBalanceAfter(transaction));
        transactionRepository.save(transaction);

        saga.setSagaState(SagaState.COMPLETED);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);

        log.info("Saga completed successfully: txnId={}, sagaId={}", transaction.getReferenceNo(), saga.getId());
    }

    private BigDecimal calculateBalanceAfter(Transaction transaction) {
        return transaction.getAmount();
    }
}
