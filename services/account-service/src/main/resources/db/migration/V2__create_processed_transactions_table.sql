-- Idempotency tracking for balance operations
CREATE TABLE IF NOT EXISTS processed_transactions
(
    id               UUID                              NOT NULL,
    transaction_id   UUID                              NOT NULL,
    saga_id          UUID                              NOT NULL,
    account_no       VARCHAR(20)                       NOT NULL,
    operation_type   VARCHAR(20)                       NOT NULL,
    amount           NUMERIC(19, 2),
    result_status    VARCHAR(20)                       NOT NULL,
    result_message   VARCHAR(500),

    -- Audit fields
    created_at       TIMESTAMP WITH TIME ZONE          NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE          NOT NULL,
    created_by       UUID,
    updated_by       UUID,
    version          BIGINT                            NOT NULL DEFAULT 0,

    CONSTRAINT pk_processed_transactions
        PRIMARY KEY (id),

    CONSTRAINT uq_processed_transactions_idempotency
        UNIQUE (transaction_id, saga_id, operation_type),

    CONSTRAINT ck_processed_transactions_operation_type
        CHECK (operation_type IN ('RESERVE', 'RELEASE', 'DEBIT', 'CREDIT')),

    CONSTRAINT ck_processed_transactions_result_status
        CHECK (result_status IN ('SUCCESS', 'FAILURE'))
);

-- Index for quick lookup
CREATE INDEX IF NOT EXISTS idx_processed_transactions_txn_saga
    ON processed_transactions(transaction_id, saga_id);

-- Table documentation
COMMENT ON TABLE processed_transactions
    IS 'Idempotency tracking for balance operations - prevents duplicate processing';

COMMENT ON COLUMN processed_transactions.id
    IS 'Primary key generated as UUID';

COMMENT ON COLUMN processed_transactions.transaction_id
    IS 'Transaction ID from transaction service';

COMMENT ON COLUMN processed_transactions.saga_id
    IS 'Saga ID from transaction service';

COMMENT ON COLUMN processed_transactions.account_no
    IS 'Account number the operation was performed on';

COMMENT ON COLUMN processed_transactions.operation_type
    IS 'Type of balance operation: RESERVE, RELEASE, DEBIT, CREDIT';

COMMENT ON COLUMN processed_transactions.result_status
    IS 'Result of the operation: SUCCESS or FAILURE';

COMMENT ON COLUMN processed_transactions.result_message
    IS 'Error message if operation failed';