CREATE TABLE oidc_session_handoffs (
    id UUID PRIMARY KEY,
    code_hash VARCHAR(64) NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    persistent_session BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_oidc_session_handoff_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_oidc_session_handoff_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_oidc_session_handoff_user
        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_oidc_session_handoffs_expiry
    ON oidc_session_handoffs (expires_at, consumed_at);

CREATE INDEX idx_oidc_session_handoffs_tenant_user
    ON oidc_session_handoffs (tenant_id, user_id);
