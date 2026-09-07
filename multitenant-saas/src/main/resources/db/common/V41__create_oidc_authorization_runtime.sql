CREATE TABLE oidc_authorization_transactions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    identity_provider_id UUID NOT NULL,
    identity_provider_version BIGINT NOT NULL,
    state_hash VARCHAR(64) NOT NULL,
    nonce_hash VARCHAR(64) NOT NULL,
    pkce_verifier_ciphertext VARCHAR(4096) NOT NULL,
    persistent_session BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_oidc_authorization_transaction_state UNIQUE (state_hash),
    CONSTRAINT fk_oidc_authorization_transaction_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_oidc_authorization_transaction_provider
        FOREIGN KEY (identity_provider_id) REFERENCES tenant_identity_providers(id) ON DELETE CASCADE,
    CONSTRAINT chk_oidc_authorization_transaction_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_oidc_authorization_transaction_expiry
    ON oidc_authorization_transactions (expires_at, consumed_at);
CREATE INDEX idx_oidc_authorization_transaction_provider
    ON oidc_authorization_transactions (tenant_id, identity_provider_id);

CREATE TABLE tenant_federated_identities (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    identity_provider_id UUID NOT NULL,
    user_id UUID NOT NULL,
    issuer VARCHAR(2048) NOT NULL,
    issuer_hash VARCHAR(64) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    email_at_link VARCHAR(150) NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_tenant_federated_identity_subject
        UNIQUE (tenant_id, issuer_hash, subject),
    CONSTRAINT uk_tenant_federated_identity_user_provider
        UNIQUE (identity_provider_id, user_id, issuer_hash),
    CONSTRAINT fk_tenant_federated_identity_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_federated_identity_provider
        FOREIGN KEY (identity_provider_id) REFERENCES tenant_identity_providers(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_federated_identity_user
        FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_tenant_federated_identity_user
    ON tenant_federated_identities (tenant_id, user_id);
CREATE INDEX idx_tenant_federated_identity_provider
    ON tenant_federated_identities (identity_provider_id, last_login_at);
