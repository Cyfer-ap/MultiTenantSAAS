ALTER TABLE outbound_webhook_deliveries
    ADD COLUMN replay_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE outbound_webhook_deliveries
    ADD CONSTRAINT chk_outbound_webhook_replay_count CHECK (replay_count >= 0);

CREATE TABLE outbound_webhook_delivery_attempts (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delivery_id UUID NOT NULL,
    replay_number INTEGER NOT NULL,
    attempt_number INTEGER NOT NULL,
    lease_token UUID NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    http_status INTEGER,
    error VARCHAR(1000),
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_outbound_webhook_attempt_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_webhook_attempt_delivery
        FOREIGN KEY (delivery_id) REFERENCES outbound_webhook_deliveries(id) ON DELETE CASCADE,
    CONSTRAINT uk_outbound_webhook_attempt_lease
        UNIQUE (delivery_id, lease_token),
    CONSTRAINT chk_outbound_webhook_attempt_replay_number
        CHECK (replay_number >= 0),
    CONSTRAINT chk_outbound_webhook_attempt_number
        CHECK (attempt_number > 0)
);

CREATE INDEX idx_outbound_webhook_attempt_delivery_started
    ON outbound_webhook_delivery_attempts (delivery_id, started_at);

CREATE INDEX idx_outbound_webhook_attempt_tenant_started
    ON outbound_webhook_delivery_attempts (tenant_id, started_at);
