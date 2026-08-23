CREATE TABLE tenant_api_keys (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(16) NOT NULL,
    key_hash VARCHAR(64) NOT NULL,
    created_by_user_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_tenant_api_key_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_api_key_creator
        FOREIGN KEY (created_by_user_id) REFERENCES app_users(id),
    CONSTRAINT uk_tenant_api_key_prefix UNIQUE (key_prefix)
);

CREATE INDEX idx_tenant_api_key_tenant_created
    ON tenant_api_keys(tenant_id, created_at);
