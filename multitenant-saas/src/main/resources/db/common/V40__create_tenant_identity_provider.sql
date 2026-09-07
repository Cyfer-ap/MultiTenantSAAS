CREATE TABLE tenant_identity_providers (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    protocol VARCHAR(20) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    issuer_uri VARCHAR(2048) NOT NULL,
    client_id VARCHAR(512) NOT NULL,
    client_secret_ciphertext VARCHAR(4096) NOT NULL,
    client_secret_hint VARCHAR(32) NOT NULL,
    secret_version INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    disabled_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL,
    updated_by_user_id UUID NOT NULL,
    secret_rotated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_tenant_identity_provider_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_tenant_identity_provider_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_identity_provider_creator
        FOREIGN KEY (created_by_user_id) REFERENCES app_users(id),
    CONSTRAINT fk_tenant_identity_provider_updater
        FOREIGN KEY (updated_by_user_id) REFERENCES app_users(id),
    CONSTRAINT chk_tenant_identity_provider_secret_version
        CHECK (secret_version > 0)
);

CREATE TABLE tenant_identity_provider_scopes (
    identity_provider_id UUID NOT NULL,
    scope VARCHAR(100) NOT NULL,
    CONSTRAINT pk_tenant_identity_provider_scopes
        PRIMARY KEY (identity_provider_id, scope),
    CONSTRAINT fk_tenant_identity_provider_scope_provider
        FOREIGN KEY (identity_provider_id) REFERENCES tenant_identity_providers(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_identity_provider_status
    ON tenant_identity_providers (status, updated_at);
