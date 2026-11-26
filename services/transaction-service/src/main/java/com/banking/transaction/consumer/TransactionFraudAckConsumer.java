package com.banking.transaction.consumer;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.saga.TransactionSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TransactionFraudAckConsumer {

    public static final String HEADER_FRAUD_STATUS = "X-Fraud-Status";
    public static final String HEADER_TXN_ID = "X-Txn-Id";

    private final TransactionRepository transactionRepository;
    private final TransactionSagaOrchestrator sagaOrchestrator;

    @Bean
    public Consumer<Message<Event>> fraudAckConsumer() {
        return message -> {
            if (message == null) return;
            Event event = message.getPayload();
            String txnId = extractTxnId(message, event);
            if (txnId == null || txnId.isBlank() || "unknown".equalsIgnoreCase(txnId)) {
                log.warn("Discarding fraud ACK: missing or invalid transaction ID");
                return;
            }
            Optional<Transaction> optionalTxn = this.transactionRepository.findByReferenceNo(txnId);
            if (optionalTxn.isEmpty()) {
                log.warn("Received fraud ACK for unknown transaction reference: txnId: {}", txnId);
                return;
            }
            Transaction transaction = optionalTxn.get();
            if (transaction.getStatus() == TransactionStatus.COMPLETED ||
                    transaction.getStatus() == TransactionStatus.REJECTED_FRAUD) {
                log.info("Transaction already processed: txnId: {} status: {}", txnId, transaction.getStatus());
                return;
            }
            if (event.approved() != null) {
                this.sagaOrchestrator.handleFraudApproved(event.approved());
            } else if (event.event() != null) {
                this.sagaOrchestrator.handleFraudRejected(event.event());
            } else {
                handleUnknownOrError(transaction, message);
            }
        };
    }

    private String extractTxnId(Message<Event> message, Event event) {
        if (event != null && event.approved() != null && event.approved().txnId() != null) {
            return event.approved().txnId();
        }
        if (event != null && event.event() != null && event.event().txnId() != null) {
            return event.event().txnId();
        }
        Object headerTxnId = message.getHeaders().get(HEADER_TXN_ID);
        return headerTxnId != null ? headerTxnId.toString() : null;
    }

    private void handleUnknownOrError(Transaction transaction, Message<Event> message) {
        String fraudStatus = String.valueOf(message.getHeaders().get(HEADER_FRAUD_STATUS));
        String errorReason = "ERROR".equalsIgnoreCase(fraudStatus) ? "FRAUD_CHECK_ERROR" : "UNKNOWN_EVALUATION_RESULT";
        this.sagaOrchestrator.handleFraudError(transaction.getReferenceNo(), errorReason);
    }
}