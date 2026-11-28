package com.banking.transaction.service;

import com.banking.transaction.client.AccountClient;
import com.banking.transaction.client.AccountDto;
import com.banking.transaction.dto.TransactionRequest;
import com.banking.transaction.dto.TransactionResponse;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.event.Event;
import com.banking.transaction.exception.InsufficientFundsException;
import com.banking.transaction.exception.InvalidTransactionException;
import com.banking.transaction.exception.TransactionNotFoundException;
import com.banking.transaction.model.TransactionStatus;
import com.banking.transaction.model.TransactionType;
import com.banking.transaction.producer.TransactionProducer;
import com.banking.transaction.repository.TransactionRepository;
import com.banking.transaction.saga.TransactionSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionProducer transactionProducer;
    private final AccountClient accountClient;
    private final TransactionSagaOrchestrator sagaOrchestrator;

    @Override
    @Transactional
    public TransactionResponse createNewTransaction(TransactionRequest request) {
        validateRequest(request);
        try {
            AccountDto account = this.accountClient.getAccount(request.senderAccountNo());
            if (account != null) {
                if ("BLOCKED".equalsIgnoreCase(account.status()) || "SUSPENDED".equalsIgnoreCase(account.status())) {
                    throw new InvalidTransactionException("Sender account is not active: " + account.status());
                }
                // Note: Balance check is now done during saga reserve step
            }
        } catch (InsufficientFundsException | InvalidTransactionException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not query account-service via HTTP Interface for {}: {}", request.senderAccountNo(), ex.getMessage());
        }
        String referenceNo = generateReferenceNo();
        UUID userId = request.userId() != null ? request.userId() : UUID.randomUUID();
        TransactionType type = request.transactionType() != null ? request.transactionType() : TransactionType.TRANSFER;
        String accountType = request.accountType() != null && !request.accountType().isBlank() ? request.accountType() : "SAVINGS";
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .referenceNo(referenceNo)
                .accountNo(request.senderAccountNo())
                .receiverAccountNo(request.receiverAccountNo())
                .accountType(accountType)
                .type(type)
                .status(TransactionStatus.PENDING)
                .amount(request.amount())
                .description(request.description())
                .transactionDate(Instant.now())
                .build();
        Transaction saved = this.transactionRepository.save(transaction);
        log.info("Transaction initiated: referenceNo: {} sender: {} amount: {}",
                saved.getReferenceNo(), saved.getAccountNo(), saved.getAmount());

        // Start saga orchestration: Fraud evaluation runs first before any balance operations
        this.sagaOrchestrator.startSaga(saved);

        return mapToResponse(saved, "Transaction initiated, evaluating fraud risk prior to account debit");
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceNo) {
        Transaction transaction = this.transactionRepository.findByReferenceNo(referenceNo)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for reference: " + referenceNo));
        return mapToResponse(transaction, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID id) {
        Transaction transaction = this.transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for id: " + id));
        return mapToResponse(transaction, null);
    }

    private void validateRequest(TransactionRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Transaction request must not be null");
        }
        if (request.senderAccountNo() == null || request.senderAccountNo().isBlank()) {
            throw new InvalidTransactionException("Sender account number is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transaction amount must be strictly greater than zero");
        }
        if (request.receiverAccountNo() != null && request.receiverAccountNo().equalsIgnoreCase(request.senderAccountNo())) {
            throw new InvalidTransactionException("Sender and receiver account numbers cannot be identical");
        }
    }

    private String generateReferenceNo() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction t, String message) {
        return TransactionResponse.builder()
                .status(t.getStatus() == TransactionStatus.COMPLETED || t.getStatus() == TransactionStatus.PENDING)
                .transactionId(t.getId())
                .userId(t.getUserId())
                .referenceNo(t.getReferenceNo())
                .senderAccountNo(t.getAccountNo())
                .receiverAccountNo(t.getReceiverAccountNo())
                .type(t.getType())
                .transactionStatus(t.getStatus())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .fraudScore(t.getFraudScore())
                .description(t.getDescription())
                .message(message)
                .transactionDate(t.getTransactionDate())
                .build();
    }
}
