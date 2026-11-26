-- Saga table for distributed transaction orchestration
-- Minimal schema - only saga-specific state, transaction data accessed via FK
CREATE TABLE IF NOT EXISTS sagas
(
    id               UUID                              NOT NULL,
    transaction_id   UUID                              NOT NULL,
    saga_state       VARCHAR(30)                       NOT NULL,
    error_message    VARCHAR(1000),
    completed_at     TIMESTAMP WITH TIME ZONE,

    -- Audit fields
    created_at       TIMESTAMP WITH TIME ZONE          NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE          NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT                            NOT NULL DEFAULT 0,

    CONSTRAINT pk_sagas
        PRIMARY KEY (id),

    CONSTRAINT fk_sagas_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions (id),

    CONSTRAINT ck_sagas_state
        CHECK (saga_state IN (
            'STARTED',
            'RESERVED',
            'FRAUD_CHECK',
            'BALANCE_UPDATED',
            'COMPLETED',
            'COMPENSATING',
            'ROLLED_BACK',
            'FAILED'
        ))
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sagas_transaction_id
    ON sagas(transaction_id);

CREATE INDEX IF NOT EXISTS idx_sagas_saga_state
    ON sagas(saga_state);

CREATE INDEX IF NOT EXISTS idx_sagas_created_at
    ON sagas(created_at);

-- Table documentation
COMMENT ON TABLE sagas
    IS 'Tracks saga instances for distributed transaction orchestration - minimal state only';

COMMENT ON COLUMN sagas.id
    IS 'Primary key generated as UUID';

COMMENT ON COLUMN sagas.transaction_id
    IS 'References the transaction this saga belongs to (FK to transactions.id)';

COMMENT ON COLUMN sagas.saga_state
    IS 'Current saga state: STARTED, RESERVED, FRAUD_CHECK, BALANCE_UPDATED, COMPLETED, COMPENSATING, ROLLED_BACK, FAILED';

COMMENT ON COLUMN sagas.error_message
    IS 'Error details when saga fails';

COMMENT ON COLUMN sagas.completed_at
    IS 'Timestamp when saga reached terminal state';