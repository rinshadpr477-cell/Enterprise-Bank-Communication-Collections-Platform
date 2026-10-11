-- V1__create_customers_and_preferences.sql
-- Initial schema: Customer management, contact preferences, and suppression

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- =============================================================
-- CUSTOMERS
-- =============================================================
CREATE TABLE customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_reference VARCHAR(50) NOT NULL UNIQUE,
    full_name       VARCHAR(255) NOT NULL,
    email           VARCHAR(320),
    phone_number    VARCHAR(20),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'en',
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_customer_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED','DECEASED'))
);

CREATE INDEX idx_customers_status ON customers(status);
CREATE INDEX idx_customers_email ON customers(email) WHERE email IS NOT NULL;
CREATE INDEX idx_customers_phone ON customers(phone_number) WHERE phone_number IS NOT NULL;
CREATE INDEX idx_customers_created_at ON customers(created_at);
-- Trigram index for fuzzy name search
CREATE INDEX idx_customers_full_name_trgm ON customers USING gin(full_name gin_trgm_ops);

-- =============================================================
-- CONTACT PREFERENCES
-- =============================================================
CREATE TABLE contact_preferences (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id     UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    channel         VARCHAR(20) NOT NULL,
    permitted       BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_contact_pref UNIQUE (customer_id, channel),
    CONSTRAINT chk_channel CHECK (channel IN ('VOICE','SMS','EMAIL','WHATSAPP'))
);

CREATE INDEX idx_contact_pref_customer ON contact_preferences(customer_id);

-- =============================================================
-- SUPPRESSION LIST
-- =============================================================
CREATE TABLE suppression_entries (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    normalized_contact  VARCHAR(320) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    reason              VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ,
    CONSTRAINT chk_suppression_channel CHECK (channel IN ('VOICE','SMS','EMAIL','WHATSAPP','ALL')),
    CONSTRAINT uq_suppression UNIQUE (normalized_contact, channel)
);

CREATE INDEX idx_suppression_contact_channel ON suppression_entries(normalized_contact, channel);
CREATE INDEX idx_suppression_expires ON suppression_entries(expires_at) WHERE expires_at IS NOT NULL;
