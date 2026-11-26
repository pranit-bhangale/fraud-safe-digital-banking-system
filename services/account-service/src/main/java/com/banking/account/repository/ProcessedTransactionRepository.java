package com.banking.account.repository;

import com.banking.account.entity.ProcessedTransaction;
import com.banking.account.model.BalanceOperationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, UUID> {

    Optional<ProcessedTransaction> findByTransactionIdAndSagaIdAndOperationType(
            UUID transactionId, UUID sagaId, BalanceOperationType operationType);

    boolean existsByTransactionIdAndSagaIdAndOperationType(
            UUID transactionId, UUID sagaId, BalanceOperationType operationType);
}