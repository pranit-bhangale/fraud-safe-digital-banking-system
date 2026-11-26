CREATE TABLE IF NOT EXISTS transactions
(
    id                  UUID                     NOT NULL,
    user_id             UUID                     NOT NULL,
    reference_no        VARCHAR(50)              NOT NULL,
    account_no          VARCHAR(50)              NOT NULL,
    receiver_account_no VARCHAR(50),
    transaction_type    VARCHAR(20)              NOT NULL,
    status              VARCHAR(30)              NOT NULL DEFAULT 'INITIATED',
    account_type        VARCHAR(20)              DEFAULT 'SAVINGS',
    amount              NUMERIC(19, 2)           NOT NULL,
    balance_after       NUMERIC(19, 2),
    fraud_score         INT                      DEFAULT 0,
    rejection_reasons   VARCHAR(500),
    description         VARCHAR(500),
    transaction_date    TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Audit fields
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by          UUID,
    updated_by          UUID,
    version             BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_transactions
        PRIMARY KEY (id),

    CONSTRAINT uq_transactions_reference_no
        UNIQUE (reference_no),

    CONSTRAINT ck_transactions_user_id_not_null
        CHECK (user_id IS NOT NULL),

    CONSTRAINT ck_transactions_reference_no_not_blank
        CHECK (length(btrim(reference_no)) > 0),

    CONSTRAINT ck_transactions_type
        CHECK (transaction_type IN (
            'CREDIT',
            'DEBIT',
            'WITHDRAWAL',
            'DEPOSIT',
            'TRANSFER'
        )),

    CONSTRAINT ck_transactions_status
        CHECK (status IN (
            'INITIATED',
            'PENDING',
            'COMPLETED',
            'REJECTED_FRAUD',
            'FAILED'
        )),

    CONSTRAINT ck_transactions_amount_positive
        CHECK (amount > 0),

    CONSTRAINT ck_transactions_balance_after_non_negative
        CHECK (balance_after IS NULL OR balance_after >= 0),

    CONSTRAINT ck_transactions_version_non_negative
        CHECK (version >= 0)
);

-- User ID index
CREATE INDEX IF NOT EXISTS idx_transactions_user_id
    ON transactions(user_id);

-- Account ID index
CREATE INDEX IF NOT EXISTS idx_transactions_account_no
    ON transactions(account_no);

-- Receiver Account ID index
CREATE INDEX IF NOT EXISTS idx_transactions_receiver_account_no
    ON transactions(receiver_account_no);

-- Status index
CREATE INDEX IF NOT EXISTS idx_transactions_status
    ON transactions(status);

-- Transaction type index
CREATE INDEX IF NOT EXISTS idx_transactions_type
    ON transactions(transaction_type);

-- Transaction date index
CREATE INDEX IF NOT EXISTS idx_transactions_transaction_date
    ON transactions(transaction_date);

-- Audit indexes
CREATE INDEX IF NOT EXISTS idx_transactions_created_at
    ON transactions(created_at);

CREATE INDEX IF NOT EXISTS idx_transactions_created_by
    ON transactions(created_by);

-- Composite index for user transaction history
CREATE INDEX IF NOT EXISTS idx_transactions_user_date
    ON transactions(user_id, transaction_date DESC);

-- Composite index for account transaction history
CREATE INDEX IF NOT EXISTS idx_transactions_account_date
    ON transactions(account_no, transaction_date DESC);

-- Table documentation
COMMENT ON TABLE transactions
    IS 'Stores financial transactions performed on customer bank accounts';

COMMENT ON COLUMN transactions.id
    IS 'Primary key generated as UUID';

COMMENT ON COLUMN transactions.user_id
    IS 'References the user who owns the transaction account';

COMMENT ON COLUMN transactions.reference_no
    IS 'Unique reference number identifying the transaction';

COMMENT ON COLUMN transactions.account_no
    IS 'References the sender bank account associated with the transaction';

COMMENT ON COLUMN transactions.receiver_account_no
    IS 'References the beneficiary/receiver bank account for transfers';

COMMENT ON COLUMN transactions.transaction_type
    IS 'Type of financial transaction: CREDIT, DEBIT, WITHDRAWAL, DEPOSIT or TRANSFER';

COMMENT ON COLUMN transactions.status
    IS 'Current lifecycle state: INITIATED, PENDING, COMPLETED, REJECTED_FRAUD or FAILED';

COMMENT ON COLUMN transactions.account_type
    IS 'Account category: SAVINGS, CURRENT, CHECKING, etc.';

COMMENT ON COLUMN transactions.amount
    IS 'Transaction amount with precision of 19 digits and 2 decimal places';

COMMENT ON COLUMN transactions.balance_after
    IS 'Account balance immediately after the transaction';

COMMENT ON COLUMN transactions.fraud_score
    IS 'Security risk score assigned by the fraud detection service';

COMMENT ON COLUMN transactions.rejection_reasons
    IS 'Security check failure reasons if transaction was blocked or rejected';

COMMENT ON COLUMN transactions.description
    IS 'Optional description of the transaction';

COMMENT ON COLUMN transactions.transaction_date
    IS 'Date and time when the transaction occurred';

COMMENT ON COLUMN transactions.created_at
    IS 'Timestamp when the transaction record was created';

COMMENT ON COLUMN transactions.updated_at
    IS 'Timestamp when the transaction record was last updated';

COMMENT ON COLUMN transactions.created_by
    IS 'User ID who created the transaction record';

COMMENT ON COLUMN transactions.updated_by
    IS 'User ID who last updated the transaction record';

COMMENT ON COLUMN transactions.version
    IS 'Optimistic locking version';
