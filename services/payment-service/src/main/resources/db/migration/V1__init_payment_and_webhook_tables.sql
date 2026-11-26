-- Migration V1: Payment Service core schema (Orders, Webhook Subscriptions, and Delivery Auditing)
CREATE TABLE IF NOT EXISTS payment_orders
(
    id                  UUID                     NOT NULL,
    order_reference     VARCHAR(64)              NOT NULL,
    merchant_id         UUID                     NOT NULL,
    customer_account_no VARCHAR(20)              NOT NULL,
    amount              NUMERIC(19, 2)           NOT NULL,
    currency            VARCHAR(3)               NOT NULL DEFAULT 'USD',
    status              VARCHAR(30)              NOT NULL,
    transaction_id      UUID,
    failure_reason      VARCHAR(255),
    idempotency_key     VARCHAR(128)             NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_payment_orders PRIMARY KEY (id),
    CONSTRAINT uq_payment_orders_reference UNIQUE (order_reference),
    CONSTRAINT uq_payment_orders_idempotency UNIQUE (idempotency_key),
    CONSTRAINT ck_payment_orders_amount CHECK (amount > 0),
    CONSTRAINT ck_payment_orders_status CHECK (status IN (
        'CREATED', 'FRAUD_CHECK', 'PROCESSING', 'COMPLETED', 'DECLINED_FRAUD', 'FAILED'
    ))
);

CREATE INDEX IF NOT EXISTS idx_payment_orders_merchant ON payment_orders(merchant_id);
CREATE INDEX IF NOT EXISTS idx_payment_orders_customer ON payment_orders(customer_account_no);
CREATE INDEX IF NOT EXISTS idx_payment_orders_status ON payment_orders(status);

CREATE TABLE IF NOT EXISTS webhook_subscriptions
(
    id                  UUID                     NOT NULL,
    merchant_id         UUID                     NOT NULL,
    target_url          VARCHAR(1024)            NOT NULL,
    secret_key          VARCHAR(255)             NOT NULL,
    event_types         VARCHAR(255)             NOT NULL,
    is_active           BOOLEAN                  NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_webhook_subscriptions PRIMARY KEY (id),
    CONSTRAINT uq_webhook_sub_merchant_url UNIQUE (merchant_id, target_url)
);

CREATE INDEX IF NOT EXISTS idx_webhook_sub_merchant ON webhook_subscriptions(merchant_id);

CREATE TABLE IF NOT EXISTS webhook_deliveries
(
    id                  UUID                     NOT NULL,
    subscription_id     UUID                     NOT NULL,
    event_id            VARCHAR(64)              NOT NULL,
    event_type          VARCHAR(50)              NOT NULL,
    payload             TEXT                     NOT NULL,
    signature_header    VARCHAR(255)             NOT NULL,
    http_status_code    INT,
    response_body       TEXT,
    attempt_count       INT                      NOT NULL DEFAULT 1,
    delivery_status     VARCHAR(20)              NOT NULL,
    next_retry_at       TIMESTAMP WITH TIME ZONE,
    delivered_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_webhook_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_webhook_deliveries_sub FOREIGN KEY (subscription_id) REFERENCES webhook_subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT ck_webhook_delivery_status CHECK (delivery_status IN ('PENDING', 'DELIVERED', 'RETRYING', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_webhook_retry ON webhook_deliveries(delivery_status, next_retry_at);
CREATE INDEX IF NOT EXISTS idx_webhook_event_id ON webhook_deliveries(event_id);
