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

-- Pre-seeded trader — account_id matches Keycloak user id from realm-hft.json
-- API key is for service-to-service calls only; UI authenticates via Keycloak JWT
INSERT INTO accounts.traders (account_id, username, api_key, buying_power, margin_limit)
VALUES ('00000000-0000-0000-0000-000000000001', 'trader', 'hft-svc-key-internal', 100000.0, 50000.0)
ON CONFLICT DO NOTHING;

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
