ALTER TABLE tenant_identity_providers
    ADD COLUMN sso_mode VARCHAR(20) NOT NULL DEFAULT 'OPTIONAL';

ALTER TABLE tenant_identity_providers
    ADD CONSTRAINT chk_tenant_identity_provider_sso_mode
    CHECK (sso_mode IN ('OPTIONAL', 'REQUIRED'));
