CREATE TABLE IF NOT EXISTS billing_customers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_customer_id VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_billing_customers_tenant
    ON billing_customers(tenant_id);

CREATE TABLE IF NOT EXISTS billing_events (
    id UUID PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
