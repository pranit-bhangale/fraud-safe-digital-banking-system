package com.banking.transaction.saga;

import com.banking.transaction.entity.Saga;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.producer.TransactionProducer;
import com.banking.transaction.repository.SagaRepository;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.saga.step.ReleaseReservationStep;
import com.banking.transaction.saga.step.ReserveBalanceStep;
import com.banking.transaction.saga.step.UpdateBalanceStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Orchestrates the saga for transaction processing with fraud detection.
 * Manages state transitions and coordinates between services via Kafka/REST.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionSagaOrchestrator {

    private final TransactionRepository transactionRepository;
    private final SagaRepository sagaRepository;
    private final TransactionProducer transactionProducer;
    private final ReserveBalanceStep reserveBalanceStep;
    private final UpdateBalanceStep updateBalanceStep;
    private final ReleaseReservationStep releaseReservationStep;

    /**
     * Starts the saga for a new transaction.
     * Step 1: Reserve sender balance
     * Step 2: Submit to fraud service via Kafka
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void startSaga(Transaction transaction) {
        Saga saga = Saga.builder()
                .transaction(transaction)
                .sagaState(SagaState.STARTED)
                .build();
        saga = sagaRepository.save(saga);
        SagaContext context = SagaContext.fromTransactionAndSaga(transaction, saga);
        log.info("Starting saga for transaction: txnId={}, sagaId={}, type={}, amount={}",
                transaction.getReferenceNo(), saga.getId(), transaction.getType(), transaction.getAmount());
        executeReserveStep(context, saga);
    }

    /**
     * Handles fraud approval - continues saga to update balances.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudApproved(Event.PaymentApproved approved) {
        String txnId = approved.txnId();
        log.info("Fraud approved for txnId: {}, continuing saga", txnId);

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

        // Execute step: Update balances (debit sender, credit receiver)
        executeUpdateBalanceStep(context, saga, approved);
    }

    /**
     * Handles fraud rejection - compensates by releasing reservation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudRejected(Event.FraudEvent fraud) {
        String txnId = fraud.txnId();
        log.warn("Fraud rejected for txnId: {}, starting compensation", txnId);

        Transaction transaction = transactionRepository.findByReferenceNo(txnId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));

        Saga saga = sagaRepository.findByTransaction(transaction)
                .orElseThrow(() -> new IllegalStateException("Saga not found for txnId: " + txnId));

        if (!SagaState.FRAUD_CHECK.equals(saga.getSagaState())) {
            log.warn("Invalid saga state for fraud rejection: txnId={}, state={}", txnId, saga.getSagaState());
            return;
        }

        SagaContext context = SagaContext.fromTransactionAndSaga(transaction, saga);
        transaction.setFraudScore(fraud.riskScore());
        transaction.setRejectionReasons(fraud.reasons() != null ? String.join(", ", fraud.reasons()) : "FRAUD_REJECTED");
        saga.setErrorMessage(fraud.reasons() != null ? String.join(", ", fraud.reasons()) : "FRAUD_REJECTED");

        // Execute compensation: Release reservation
        executeCompensation(context, saga, transaction);
    }

    /**
     * Handles fraud check timeout or error - compensates.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void handleFraudError(String txnId, String errorReason) {
        log.error("Fraud check error for txnId: {}, reason: {}", txnId, errorReason);

        Transaction transaction = transactionRepository.findByReferenceNo(txnId)
                .orElseThrow(() -> new IllegalStateException("Transaction not found: " + txnId));

        Saga saga = sagaRepository.findByTransaction(transaction)
                .orElseThrow(() -> new IllegalStateException("Saga not found for txnId: " + txnId));

        if (!SagaState.FRAUD_CHECK.equals(saga.getSagaState())) {
            log.warn("Invalid saga state for fraud error: txnId={}, state={}", txnId, saga.getSagaState());
            return;
        }

        SagaContext context = SagaContext.fromTransactionAndSaga(transaction, saga);
        transaction.setRejectionReasons("FRAUD_CHECK_ERROR: " + errorReason);
        saga.setErrorMessage("FRAUD_CHECK_ERROR: " + errorReason);

        executeCompensation(context, saga, transaction);
    }

    private void executeReserveStep(SagaContext context, Saga saga) {
        try {
            log.debug("Executing reserve balance step: txnId={}", context.referenceNo());
            reserveBalanceStep.execute(context);
            updateSagaState(saga, SagaState.RESERVED);
            submitToFraudService(context);
        } catch (Exception e) {
            log.error("Reserve balance step failed: txnId={}, error={}", context.referenceNo(), e.getMessage(), e);
            saga.setErrorMessage(e.getMessage());
            executeCompensation(context, saga, transactionRepository.findByReferenceNo(context.referenceNo()).orElseThrow());
        }
    }

    private void executeUpdateBalanceStep(SagaContext context, Saga saga, Event.PaymentApproved approved) {
        try {
            log.debug("Executing update balance step: txnId={}", context.referenceNo());
            updateBalanceStep.execute(context, approved);
            updateSagaState(saga, SagaState.BALANCE_UPDATED);
            completeSaga(transactionRepository.findByReferenceNo(context.referenceNo()).orElseThrow(), saga);
        } catch (Exception e) {
            log.error("Update balance step failed: txnId={}, error={}", context.referenceNo(), e.getMessage(), e);
            // No retry - immediate compensation
            saga.setErrorMessage(e.getMessage());
            executeCompensation(context, saga, transactionRepository.findByReferenceNo(context.referenceNo()).orElseThrow());
        }
    }

    private void executeCompensation(SagaContext context, Saga saga, Transaction transaction) {
        try {
            log.info("Executing compensation for txnId: {}", context.referenceNo());

            // Update to compensating state
            updateSagaState(saga, SagaState.COMPENSATING);

            releaseReservationStep.execute(context);

            // Mark as rolled back
            updateSagaState(saga, SagaState.ROLLED_BACK);
            saga.setCompletedAt(java.time.Instant.now());
            transaction.setStatus(TransactionStatus.REJECTED_FRAUD);
            transactionRepository.save(transaction);

            log.info("Saga compensation completed for txnId: {}", context.referenceNo());

        } catch (Exception e) {
            log.error("Saga compensation failed: txnId={}, error={}",
                    context.referenceNo(), e.getMessage(), e);
            // Mark as failed - requires manual intervention
            saga.setSagaState(SagaState.FAILED);
            saga.setErrorMessage("COMPENSATION_FAILED: " + e.getMessage());
            saga.setCompletedAt(java.time.Instant.now());
            sagaRepository.save(saga);
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRejectionReasons("COMPENSATION_FAILED: " + e.getMessage());
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
        log.info("Submitted to fraud service: txnId={}, sagaId={}", context.referenceNo(), context.sagaId());
    }

    private void completeSaga(Transaction transaction, Saga saga) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        saga.setSagaState(SagaState.COMPLETED);
        saga.setCompletedAt(java.time.Instant.now());
        sagaRepository.save(saga);
        transaction.setBalanceAfter(calculateBalanceAfter(transaction));
        transactionRepository.save(transaction);
        log.info("Saga completed successfully: txnId={}, sagaId={}", transaction.getReferenceNo(), saga.getId());
    }

    private BigDecimal calculateBalanceAfter(Transaction transaction) {
        // For simplicity, return the transaction amount as balance after
        // In reality, this would query the account service for actual balance
        return transaction.getAmount();
    }
}