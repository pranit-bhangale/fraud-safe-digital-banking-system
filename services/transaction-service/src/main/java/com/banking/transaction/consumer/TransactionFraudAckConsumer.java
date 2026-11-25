package com.banking.transaction.consumer;

import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.Event;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.repository.TransactionRepository;
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
                handleApproved(transaction, event.approved());
            } else if (event.event() != null) {
                handleFraudRejected(transaction, event.event());
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

    private void handleApproved(Transaction transaction, Event.PaymentApproved approved) {
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setFraudScore(approved.riskScore());
        this.transactionRepository.save(transaction);
        log.info("Transaction approved and completed: txnId: {} sender: {} amount: {}",
                transaction.getReferenceNo(), transaction.getAccountNo(), transaction.getAmount());
    }

    private void handleFraudRejected(Transaction transaction, Event.FraudEvent fraud) {
        transaction.setStatus(TransactionStatus.REJECTED_FRAUD);
        transaction.setFraudScore(fraud.riskScore());
        String reasons = fraud.reasons() != null ? String.join(", ", fraud.reasons()) : "BLOCKED_BY_FRAUD";
        transaction.setRejectionReasons(reasons);
        this.transactionRepository.save(transaction);
        log.warn("Transaction rejected due to fraud detection: txnId: {} sender: {} score: {} reasons: {}",
                transaction.getReferenceNo(), transaction.getAccountNo(), fraud.riskScore(), reasons);
    }

    private void handleUnknownOrError(Transaction transaction, Message<Event> message) {
        String fraudStatus = String.valueOf(message.getHeaders().get(HEADER_FRAUD_STATUS));
        if ("ERROR".equalsIgnoreCase(fraudStatus)) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRejectionReasons("FRAUD_CHECK_ERROR");
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setRejectionReasons("UNKNOWN_EVALUATION_RESULT");
        }
        this.transactionRepository.save(transaction);
        log.error("Transaction failed during security evaluation: txnId: {} status: {}",
                transaction.getReferenceNo(), transaction.getStatus());
    }
}