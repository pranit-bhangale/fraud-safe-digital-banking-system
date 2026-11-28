-- Migration V3: Create account_types table in strict BCNF and establish FK relationship
CREATE TABLE IF NOT EXISTS account_types
(
    type_code            VARCHAR(20)    NOT NULL,
    name                 VARCHAR(50)    NOT NULL,
    min_opening_balance  NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    daily_transfer_limit NUMERIC(19, 2) NOT NULL DEFAULT 50000.00,
    is_active            BOOLEAN        NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_account_types PRIMARY KEY (type_code),
    CONSTRAINT ck_account_types_min_balance CHECK (min_opening_balance >= 0),
    CONSTRAINT ck_account_types_daily_limit CHECK (daily_transfer_limit >= 0)
);

-- Seed initial standard account types
INSERT INTO account_types (type_code, name, min_opening_balance, daily_transfer_limit, is_active)
VALUES
    ('SAVING', 'Savings Account', 100.00, 25000.00, TRUE),
    ('CURRENT', 'Current Checking Account', 500.00, 100000.00, TRUE),
    ('BUSINESS', 'Business Corporate Account', 1000.00, 500000.00, TRUE),
    ('SALARY', 'Salary Account', 0.00, 50000.00, TRUE),
    ('FIXED_DEPOSIT', 'Fixed Term Deposit Account', 1000.00, 0.00, TRUE)
ON CONFLICT (type_code) DO NOTHING;

-- Establish Foreign Key constraint to enforce BCNF relationship
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_accounts_account_type'
    ) THEN
        ALTER TABLE accounts
            ADD CONSTRAINT fk_accounts_account_type
            FOREIGN KEY (account_type) REFERENCES account_types(type_code);
    END IF;
END $$;
