-- Migration V3: Create saga_step_executions table for atomic step tracking and idempotent auditing
CREATE TABLE IF NOT EXISTS saga_step_executions
(
    id               UUID                     NOT NULL,
    saga_id          UUID                     NOT NULL,
    step_name        VARCHAR(50)              NOT NULL,
    step_status      VARCHAR(30)              NOT NULL,
    attempt_count    INT                      NOT NULL DEFAULT 1,
    payload          TEXT,
    error_message    VARCHAR(1000),
    executed_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_saga_step_executions PRIMARY KEY (id),
    CONSTRAINT fk_saga_step_executions_saga FOREIGN KEY (saga_id) REFERENCES sagas (id) ON DELETE CASCADE,
    CONSTRAINT ck_saga_step_status CHECK (step_status IN ('STARTED', 'PENDING', 'SUCCESS', 'FAILED', 'COMPENSATED')),
    CONSTRAINT uq_saga_step_name UNIQUE (saga_id, step_name)
);

CREATE INDEX IF NOT EXISTS idx_saga_step_executions_saga_id ON saga_step_executions(saga_id);
