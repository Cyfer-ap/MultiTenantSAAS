CREATE TABLE outbound_webhook_events (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    payload_json TEXT NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_outbound_webhook_event_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE
);

CREATE TABLE outbound_webhook_deliveries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    event_id UUID NOT NULL,
    endpoint_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    processing_started_at TIMESTAMP WITH TIME ZONE,
    lease_token UUID,
    last_http_status INTEGER,
    last_error VARCHAR(1000),
    sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_outbound_webhook_delivery_event_endpoint
        UNIQUE (event_id, endpoint_id),
    CONSTRAINT fk_outbound_webhook_delivery_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_webhook_delivery_event
        FOREIGN KEY (event_id) REFERENCES outbound_webhook_events(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_webhook_delivery_endpoint
        FOREIGN KEY (endpoint_id) REFERENCES outbound_webhook_endpoints(id) ON DELETE CASCADE,
    CONSTRAINT chk_outbound_webhook_delivery_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_outbound_webhook_event_tenant_created
    ON outbound_webhook_events (tenant_id, created_at);

CREATE INDEX idx_outbound_webhook_delivery_ready
    ON outbound_webhook_deliveries (status, next_attempt_at, created_at);

CREATE INDEX idx_outbound_webhook_delivery_processing
    ON outbound_webhook_deliveries (status, processing_started_at);

CREATE INDEX idx_outbound_webhook_delivery_tenant_created
    ON outbound_webhook_deliveries (tenant_id, created_at);
