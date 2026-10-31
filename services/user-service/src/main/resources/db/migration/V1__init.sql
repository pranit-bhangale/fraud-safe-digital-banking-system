CREATE TABLE IF NOT EXISTS users
(
    id         UUID         NOT NULL,
    username   VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL,
    role       VARCHAR(30)  NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by UUID,
    updated_by UUID,

    version    BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_users
        PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_enabled
    ON users (enabled);
CREATE INDEX IF NOT EXISTS idx_users_created_at
    ON users (created_at);
