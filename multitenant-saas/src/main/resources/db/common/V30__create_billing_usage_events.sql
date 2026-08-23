CREATE TABLE billing_usage_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    metric_code VARCHAR(64) NOT NULL,
    quantity BIGINT NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_billing_usage_event_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT uk_billing_usage_event_idempotency
        UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT ck_billing_usage_event_quantity_positive
        CHECK (quantity > 0)
);

CREATE INDEX idx_billing_usage_event_period
    ON billing_usage_events(tenant_id, metric_code, occurred_at);
