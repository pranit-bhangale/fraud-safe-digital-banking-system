package com.banking.account.entity;

import com.banking.account.model.BalanceOperationType;
import com.banking.utility.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Idempotency tracking for balance operations.
 * Prevents duplicate processing of the same transaction/saga combination.
 */
@Entity
@Table(name = "processed_transactions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class ProcessedTransaction extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "saga_id", nullable = false)
    private UUID sagaId;

    @Column(name = "account_no", nullable = false, length = 20)
    private String accountNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private BalanceOperationType operationType;

    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "result_status", nullable = false, length = 20)
    private String resultStatus; // SUCCESS, FAILURE

    @Column(name = "result_message", length = 500)
    private String resultMessage;
}