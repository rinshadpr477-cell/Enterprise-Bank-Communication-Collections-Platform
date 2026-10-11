-- V4__create_collections_and_agent_tasks.sql
-- Collections management and human-agent task system

-- =============================================================
-- COLLECTION CASES
-- Use NUMERIC for monetary amounts - never FLOAT or DOUBLE
-- =============================================================
CREATE TABLE collection_cases (
    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id                 UUID NOT NULL REFERENCES customers(id),
    external_account_reference  VARCHAR(100) NOT NULL,
    outstanding_amount           NUMERIC(19,4) NOT NULL,
    currency                    CHAR(3) NOT NULL DEFAULT 'INR',
    due_date                    DATE NOT NULL,
    status                      VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_agent_id           VARCHAR(100),
    priority                    VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    notes                       TEXT,
    resolved_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_case_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','WRITTEN_OFF','ESCALATED')),
    CONSTRAINT chk_case_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_outstanding_positive CHECK (outstanding_amount >= 0)
);

CREATE INDEX idx_collection_cases_customer ON collection_cases(customer_id);
CREATE INDEX idx_collection_cases_status ON collection_cases(status);
CREATE INDEX idx_collection_cases_due_date ON collection_cases(due_date) WHERE status = 'OPEN';
CREATE INDEX idx_collection_cases_agent ON collection_cases(assigned_agent_id) WHERE assigned_agent_id IS NOT NULL;

-- =============================================================
-- AGENT TASKS
-- =============================================================
CREATE TABLE agent_tasks (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    collection_case_id  UUID NOT NULL REFERENCES collection_cases(id),
    task_type           VARCHAR(50) NOT NULL,
    status              VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    assigned_agent_id   VARCHAR(100),
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    due_at              TIMESTAMPTZ,
    notes               TEXT,
    completed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_task_status CHECK (status IN ('OPEN','ASSIGNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT chk_task_type CHECK (task_type IN ('CALL_CUSTOMER','SEND_NOTICE','VERIFY_PAYMENT','ESCALATE','FIELD_VISIT','OTHER')),
    CONSTRAINT chk_task_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL'))
);

CREATE INDEX idx_agent_tasks_case ON agent_tasks(collection_case_id);
CREATE INDEX idx_agent_tasks_agent ON agent_tasks(assigned_agent_id) WHERE assigned_agent_id IS NOT NULL;
CREATE INDEX idx_agent_tasks_status ON agent_tasks(status);
CREATE INDEX idx_agent_tasks_due_at ON agent_tasks(due_at) WHERE status IN ('OPEN','ASSIGNED');
-- Composite index for agent workqueue
CREATE INDEX idx_agent_tasks_workqueue ON agent_tasks(assigned_agent_id, status, due_at)
    WHERE status IN ('ASSIGNED','IN_PROGRESS');
