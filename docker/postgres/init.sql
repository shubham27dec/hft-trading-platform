-- accounts schema — used by order-entry-service (ddl-auto: validate)
CREATE SCHEMA IF NOT EXISTS accounts;

CREATE TABLE IF NOT EXISTS accounts.traders (
    id            BIGSERIAL PRIMARY KEY,
    account_id    VARCHAR(36)       NOT NULL UNIQUE,
    username      VARCHAR(100)      NOT NULL UNIQUE,
    api_key       VARCHAR(64)       UNIQUE,
    buying_power  DOUBLE PRECISION  NOT NULL DEFAULT 100000.0,
    margin_limit  DOUBLE PRECISION  NOT NULL DEFAULT 50000.0
);

-- audit schema — used by audit-service (ddl-auto: validate)
CREATE SCHEMA IF NOT EXISTS audit;

CREATE TABLE IF NOT EXISTS audit.audit_events (
    id              BIGSERIAL PRIMARY KEY,
    event_type      VARCHAR(20)  NOT NULL,
    order_id        VARCHAR(36),
    account_id      VARCHAR(36),
    symbol          VARCHAR(10),
    details         TEXT,
    event_timestamp BIGINT,
    recorded_at     BIGINT
);

CREATE INDEX IF NOT EXISTS idx_audit_events_order_id   ON audit.audit_events (order_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_account_id ON audit.audit_events (account_id);
