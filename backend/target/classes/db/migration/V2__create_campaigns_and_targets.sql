-- V2__create_campaigns_and_targets.sql
-- Campaign management with state machine enforcement

-- =============================================================
-- CAMPAIGNS
-- =============================================================
CREATE TABLE campaigns (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(255) NOT NULL,
    campaign_type   VARCHAR(50) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    description     TEXT,
    scheduled_at    TIMESTAMPTZ,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_campaign_status CHECK (status IN ('DRAFT','SCHEDULED','RUNNING','PAUSED','COMPLETED','CANCELLED')),
    CONSTRAINT chk_campaign_type CHECK (campaign_type IN ('PAYMENT_REMINDER','VOICE_OUTREACH','SMS_NOTIFICATION','COLLECTIONS','FOLLOW_UP'))
);

CREATE INDEX idx_campaigns_status ON campaigns(status);
CREATE INDEX idx_campaigns_scheduled_at ON campaigns(scheduled_at) WHERE status = 'SCHEDULED';
CREATE INDEX idx_campaigns_created_at ON campaigns(created_at);

-- =============================================================
-- CAMPAIGN TARGETS
-- =============================================================
CREATE TABLE campaign_targets (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_id     UUID NOT NULL REFERENCES campaigns(id),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ,
    CONSTRAINT uq_campaign_customer UNIQUE (campaign_id, customer_id),
    CONSTRAINT chk_target_status CHECK (status IN ('PENDING','PROCESSING','COMPLETED','FAILED','SKIPPED'))
);

CREATE INDEX idx_campaign_targets_campaign ON campaign_targets(campaign_id);
CREATE INDEX idx_campaign_targets_customer ON campaign_targets(customer_id);
CREATE INDEX idx_campaign_targets_status ON campaign_targets(campaign_id, status);
-- Composite index for batch fetching pending targets
CREATE INDEX idx_campaign_targets_pending ON campaign_targets(campaign_id, created_at)
    WHERE status = 'PENDING';
