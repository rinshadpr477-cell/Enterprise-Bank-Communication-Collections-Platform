-- V5__create_kafka_outbox_and_audit.sql
-- Transactional outbox for reliable event publishing and audit records

-- =============================================================
-- OUTBOX EVENTS (Transactional Outbox Pattern)
-- Records are inserted in the same transaction as business changes.
-- A background publisher reads and sends to Kafka.
-- The crash window: between Kafka ack and marking published_at,
-- a crash causes republication - consumers must be idempotent.
-- =============================================================
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    schema_version  INT NOT NULL DEFAULT 1,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    publish_attempts INT NOT NULL DEFAULT 0,
    locked_by       VARCHAR(100),
    locked_at       TIMESTAMPTZ
);

-- Index for polling unpublished events; this is queried by the scheduler
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at)
    WHERE published_at IS NULL;

-- Index for stale lock detection (for crash recovery)
CREATE INDEX idx_outbox_locked ON outbox_events(locked_at)
    WHERE locked_by IS NOT NULL AND published_at IS NULL;

-- =============================================================
-- PROCESSED EVENTS (Consumer Idempotency)
-- Before processing any Kafka event, check this table.
-- Composite unique prevents the same consumer from processing twice.
-- =============================================================
CREATE TABLE processed_events (
    event_id        VARCHAR(128) NOT NULL,
    consumer_name   VARCHAR(100) NOT NULL,
    processed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX idx_processed_events_event_id ON processed_events(event_id);

-- =============================================================
-- AUDIT RECORDS
-- Critical: Never store credentials, tokens, card numbers, or full PII.
-- The metadata column stores only non-sensitive contextual information.
-- =============================================================
CREATE TABLE audit_records (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id        VARCHAR(100) NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       VARCHAR(100),
    correlation_id  VARCHAR(100),
    ip_address      VARCHAR(45),
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata        JSONB,
    outcome         VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    CONSTRAINT chk_audit_outcome CHECK (outcome IN ('SUCCESS','FAILURE','DENIED'))
);

CREATE INDEX idx_audit_actor ON audit_records(actor_id);
CREATE INDEX idx_audit_entity ON audit_records(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_records(timestamp);
CREATE INDEX idx_audit_correlation ON audit_records(correlation_id) WHERE correlation_id IS NOT NULL;

-- =============================================================
-- WEBHOOK REPLAY PROTECTION
-- Tracks processed webhook nonces to prevent replay attacks.
-- =============================================================
CREATE TABLE webhook_nonces (
    nonce           VARCHAR(255) NOT NULL PRIMARY KEY,
    provider        VARCHAR(50) NOT NULL,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_webhook_nonces_expires ON webhook_nonces(expires_at);
