-- V6__seed_reference_data_and_performance_indexes.sql
-- Composite indexes for specific query patterns and reference data

-- =============================================================
-- ADDITIONAL COMPOSITE INDEXES FOR COMMON QUERY PATTERNS
-- =============================================================

-- For customer search by status + created_at (pagination)
CREATE INDEX idx_customers_status_created ON customers(status, created_at DESC);

-- For getting jobs due for retry with status filter
CREATE INDEX idx_comm_jobs_retry_due ON communication_jobs(next_retry_at ASC, id)
    WHERE status = 'RETRYING' AND next_retry_at IS NOT NULL;

-- For campaign job count reporting
CREATE INDEX idx_comm_jobs_campaign_status ON communication_jobs(campaign_id, status)
    WHERE campaign_id IS NOT NULL;

-- For collection cases ordered by amount for priority processing
CREATE INDEX idx_collection_cases_amount ON collection_cases(outstanding_amount DESC)
    WHERE status = 'OPEN';

-- =============================================================
-- SEED DEVELOPMENT DATA (only in non-production environments)
-- These are safe placeholder suppression rules for testing
-- =============================================================
INSERT INTO suppression_entries (normalized_contact, channel, reason)
VALUES
    ('test-suppressed@example.com', 'EMAIL', 'OPT_OUT'),
    ('+910000000000', 'SMS', 'DND_REGISTRY'),
    ('+910000000000', 'VOICE', 'DND_REGISTRY')
ON CONFLICT (normalized_contact, channel) DO NOTHING;
