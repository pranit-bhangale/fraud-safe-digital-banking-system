package com.banking.transaction.repository;

import com.banking.transaction.entity.Saga;
import com.banking.transaction.entity.Transaction;
import com.banking.transaction.saga.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for saga instances.
 */
@Repository
public interface SagaRepository extends JpaRepository<Saga, UUID> {

    Optional<Saga> findByTransaction(Transaction transaction);

    Optional<Saga> findByTransactionId(UUID transactionId);

    List<Saga> findBySagaState(SagaState sagaState);

    List<Saga> findBySagaStateIn(List<SagaState> states);

    boolean existsByTransactionId(UUID transactionId);
}