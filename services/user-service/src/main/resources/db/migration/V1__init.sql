
CREATE TABLE IF NOT EXISTS users
(
    id         UUID         NOT NULL,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    email      VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT pk_users
    PRIMARY KEY (id),

    CONSTRAINT uq_users_username
    UNIQUE (username),

    CONSTRAINT uq_users_email
    UNIQUE (email),

    CONSTRAINT ck_users_username_not_blank
    CHECK (length(btrim(username)) > 0),

    CONSTRAINT ck_users_password_not_blank
    CHECK (length(btrim(password)) > 0),

    CONSTRAINT ck_users_email_not_blank
    CHECK (length(btrim(email)) > 0),

    CONSTRAINT ck_users_first_name_not_blank
    CHECK (length(btrim(first_name)) > 0),

    CONSTRAINT ck_users_last_name_not_blank
    CHECK (length(btrim(last_name)) > 0),

    CONSTRAINT ck_users_version_non_negative
    CHECK (version >= 0)
    );

-- Username index
CREATE INDEX idx_users_username
    ON users(username);

-- Email index
CREATE INDEX idx_users_email
    ON users(email);

-- Table documentation
COMMENT ON TABLE users
    IS 'Stores registered digital banking users';

COMMENT ON COLUMN users.id
    IS 'Primary key generated as UUID';

COMMENT ON COLUMN users.password
    IS 'Stores BCrypt or Argon2 encoded password hash';

COMMENT ON COLUMN users.version
    IS 'Optimistic locking version';