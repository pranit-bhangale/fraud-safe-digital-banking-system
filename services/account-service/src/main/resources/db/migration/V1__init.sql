
CREATE TABLE IF NOT EXISTS accounts
(
    id             UUID           NOT NULL,
    account_no     VARCHAR(20)    NOT NULL,
    user_id        UUID           NOT NULL,
    account_type   VARCHAR(20)    NOT NULL,
    account_status VARCHAR(20)    NOT NULL,
    balance        NUMERIC(19, 2) NOT NULL DEFAULT 0.00,

    -- Audit fields
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by     UUID,
    updated_by     UUID,
    version        BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_accounts
        PRIMARY KEY (id),

    CONSTRAINT uq_accounts_account_no
        UNIQUE (account_no),

    CONSTRAINT ck_accounts_account_no_not_blank
        CHECK (length(btrim(account_no)) > 0),

    CONSTRAINT ck_accounts_account_type
        CHECK (account_type IN (
            'SAVING',
            'CURRENT',
            'BUSINESS',
            'SALARY',
            'FIXED_DEPOSIT'
        )),

    CONSTRAINT ck_accounts_account_status
        CHECK (account_status IN (
            'PENDING',
            'ACTIVE',
            'INACTIVE',
            'FROZEN',
            'BLOCKED',
            'CLOSED'
        )),

    CONSTRAINT ck_accounts_balance_non_negative
        CHECK (balance >= 0),

    CONSTRAINT ck_accounts_version_non_negative
        CHECK (version >= 0)
);

-- User ID index
CREATE INDEX idx_accounts_user_id
    ON accounts(user_id);

-- Account status index
CREATE INDEX idx_accounts_account_status
    ON accounts(account_status);

-- Account type index
CREATE INDEX idx_accounts_account_type
    ON accounts(account_type);

-- Audit indexes
CREATE INDEX idx_accounts_created_at
    ON accounts(created_at);

CREATE INDEX idx_accounts_created_by
    ON accounts(created_by);

-- Table documentation
COMMENT ON TABLE accounts
    IS 'Stores customer bank account information';

COMMENT ON COLUMN accounts.id
    IS 'Primary key generated as UUID';

COMMENT ON COLUMN accounts.account_no
    IS 'Unique bank account number';

COMMENT ON COLUMN accounts.user_id
    IS 'References the owner user ID';

COMMENT ON COLUMN accounts.account_type
    IS 'Type of bank account';

COMMENT ON COLUMN accounts.account_status
    IS 'Current lifecycle status of the bank account';

COMMENT ON COLUMN accounts.balance
    IS 'Current account balance with precision of 19 digits and 2 decimal places';

COMMENT ON COLUMN accounts.created_at
    IS 'Timestamp when the account was created';

COMMENT ON COLUMN accounts.updated_at
    IS 'Timestamp when the account was last updated';

COMMENT ON COLUMN accounts.created_by
    IS 'User ID who created the account';

COMMENT ON COLUMN accounts.updated_by
    IS 'User ID who last updated the account';

COMMENT ON COLUMN accounts.version
    IS 'Optimistic locking version';
