CREATE TABLE outbound_webhook_endpoints (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    enabled BOOLEAN NOT NULL,
    secret_ciphertext VARCHAR(4096) NOT NULL,
    secret_hint VARCHAR(32) NOT NULL,
    secret_version INTEGER NOT NULL,
    created_by_user_id UUID NOT NULL,
    updated_by_user_id UUID NOT NULL,
    secret_rotated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_outbound_webhook_endpoint_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_webhook_endpoint_creator
        FOREIGN KEY (created_by_user_id) REFERENCES app_users(id),
    CONSTRAINT fk_outbound_webhook_endpoint_updater
        FOREIGN KEY (updated_by_user_id) REFERENCES app_users(id),
    CONSTRAINT chk_outbound_webhook_secret_version
        CHECK (secret_version > 0)
);

CREATE TABLE outbound_webhook_endpoint_events (
    endpoint_id UUID NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    CONSTRAINT pk_outbound_webhook_endpoint_events
        PRIMARY KEY (endpoint_id, event_type),
    CONSTRAINT fk_outbound_webhook_event_endpoint
        FOREIGN KEY (endpoint_id) REFERENCES outbound_webhook_endpoints(id) ON DELETE CASCADE
);

CREATE INDEX idx_outbound_webhook_endpoint_tenant_created
    ON outbound_webhook_endpoints (tenant_id, created_at);

CREATE INDEX idx_outbound_webhook_endpoint_tenant_archived
    ON outbound_webhook_endpoints (tenant_id, archived_at);
