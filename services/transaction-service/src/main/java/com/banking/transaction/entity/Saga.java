package com.banking.transaction.entity;

import com.banking.transaction.saga.SagaState;
import com.banking.utility.audit.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

/**
 * Saga instance tracking for distributed transaction orchestration.
 * Minimal entity - only saga-specific state, transaction data accessed via FK.
 */
@Entity
@Table(
        name = "sagas",
        indexes = {
                @Index(name = "idx_sagas_transaction_id", columnList = "transaction_id"),
                @Index(name = "idx_sagas_saga_state", columnList = "saga_state"),
                @Index(name = "idx_sagas_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
public class Saga extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "transaction_id", nullable = false, updatable = false)
    private Transaction transaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "saga_state", nullable = false, length = 30)
    private SagaState sagaState;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    public boolean isTerminalState() {
        return sagaState == SagaState.COMPLETED ||
                sagaState == SagaState.ROLLED_BACK ||
                sagaState == SagaState.FAILED;
    }

    // Convenience methods to access transaction data without duplication
    public String getReferenceNo() {
        return transaction != null ? transaction.getReferenceNo() : null;
    }

    public String getSenderAccountNo() {
        return transaction != null ? transaction.getAccountNo() : null;
    }

    public String getReceiverAccountNo() {
        return transaction != null ? transaction.getReceiverAccountNo() : null;
    }

    public String getTransactionType() {
        return transaction != null && transaction.getType() != null ? transaction.getType().name() : null;
    }

    public java.math.BigDecimal getAmount() {
        return transaction != null ? transaction.getAmount() : null;
    }

    public String getAccountType() {
        return transaction != null ? transaction.getAccountType() : null;
    }

    public String getDescription() {
        return transaction != null ? transaction.getDescription() : null;
    }
}