package com.banking.transaction.repository;

import com.banking.transaction.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReferenceNo(String referenceNo);

    Page<Transaction> findByUserIdAndAccountNoOrderByTransactionDateDesc
            (UUID userId, String accountNo, Pageable pageable);

    Optional<Transaction> findFirstByAccountNoAndType(String accountNo, String type);
}
