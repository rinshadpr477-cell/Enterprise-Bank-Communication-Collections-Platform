-- V3__create_communication_jobs_and_attempts.sql
-- Communication job lifecycle and attempt history

-- =============================================================
-- COMMUNICATION JOBS
-- =============================================================
CREATE TABLE communication_jobs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL REFERENCES customers(id),
    campaign_id     UUID REFERENCES campaigns(id),
    channel         VARCHAR(20) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    scheduled_at    TIMESTAMPTZ,
    attempt_count   INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 3,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    last_error      TEXT,
    next_retry_at   TIMESTAMPTZ,
    claimed_by      VARCHAR(100),
    claimed_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_job_channel CHECK (channel IN ('VOICE','SMS','EMAIL','WHATSAPP')),
    CONSTRAINT chk_job_status CHECK (status IN (
        'PENDING','QUEUED','IN_PROGRESS','COMPLETED','FAILED',
        'RETRYING','CANCELLED','UNCERTAIN','SUPPRESSED'
    ))
);

-- Index for the scheduler query: find due pending jobs using SKIP LOCKED
-- This is critical for performance with concurrent scheduler instances
CREATE INDEX idx_comm_jobs_scheduler ON communication_jobs(scheduled_at, id)
    WHERE status IN ('PENDING', 'RETRYING');

-- Index for retry eligibility lookup
CREATE INDEX idx_comm_jobs_retry ON communication_jobs(next_retry_at)
    WHERE status = 'RETRYING';

CREATE INDEX idx_comm_jobs_customer ON communication_jobs(customer_id);
CREATE INDEX idx_comm_jobs_campaign ON communication_jobs(campaign_id) WHERE campaign_id IS NOT NULL;
CREATE INDEX idx_comm_jobs_status ON communication_jobs(status);
CREATE INDEX idx_comm_jobs_created_at ON communication_jobs(created_at);

-- =============================================================
-- COMMUNICATION ATTEMPTS
-- Each attempt is recorded independently; never overwrite history.
-- =============================================================
CREATE TABLE communication_attempts (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id              UUID NOT NULL REFERENCES communication_jobs(id),
    provider            VARCHAR(50) NOT NULL,
    provider_reference  VARCHAR(255),
    attempt_number      INT NOT NULL,
    status              VARCHAR(30) NOT NULL,
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at        TIMESTAMPTZ,
    failure_code        VARCHAR(50),
    failure_reason      TEXT,
    raw_response        TEXT,
    CONSTRAINT uq_attempt_job_number UNIQUE (job_id, attempt_number),
    CONSTRAINT chk_attempt_status CHECK (status IN (
        'IN_PROGRESS','DELIVERED','FAILED','TIMEOUT','UNCERTAIN'
    ))
);

CREATE INDEX idx_attempts_job_id ON communication_attempts(job_id);
CREATE INDEX idx_attempts_provider_ref ON communication_attempts(provider_reference)
    WHERE provider_reference IS NOT NULL;
CREATE INDEX idx_attempts_started_at ON communication_attempts(started_at);
