CREATE TABLE external_access_grants (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    revoked_by_user_id UUID,
    guest_name VARCHAR(150) NOT NULL,
    guest_email VARCHAR(150) NOT NULL,
    invitation_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    row_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_external_access_grants PRIMARY KEY (id),
    CONSTRAINT uk_external_access_grant_scope_id
        UNIQUE (tenant_id, project_id, id),
    CONSTRAINT uk_external_access_grant_invitation_hash
        UNIQUE (invitation_token_hash),
    CONSTRAINT fk_external_access_grant_project
        FOREIGN KEY (tenant_id, project_id) REFERENCES projects (tenant_id, id),
    CONSTRAINT fk_external_access_grant_creator
        FOREIGN KEY (tenant_id, created_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT fk_external_access_grant_revoker
        FOREIGN KEY (tenant_id, revoked_by_user_id) REFERENCES app_users (tenant_id, id),
    CONSTRAINT ck_external_access_grant_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_external_access_grants_project_created
    ON external_access_grants (tenant_id, project_id, created_at DESC);

CREATE INDEX idx_external_access_grants_project_expiry
    ON external_access_grants (tenant_id, project_id, expires_at);

CREATE TABLE external_access_grant_capabilities (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    grant_id UUID NOT NULL,
    capability VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_external_access_grant_capabilities PRIMARY KEY (id),
    CONSTRAINT uk_external_access_grant_capability
        UNIQUE (tenant_id, project_id, grant_id, capability),
    CONSTRAINT fk_external_access_grant_capability_grant
        FOREIGN KEY (tenant_id, project_id, grant_id)
            REFERENCES external_access_grants (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_external_access_grant_capability
        CHECK (capability IN ('PROJECT_READ', 'TASK_READ'))
);

CREATE INDEX idx_external_access_grant_capabilities_grant
    ON external_access_grant_capabilities (tenant_id, project_id, grant_id);

CREATE TABLE external_guest_sessions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    grant_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    row_version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_external_guest_sessions PRIMARY KEY (id),
    CONSTRAINT uk_external_guest_session_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_external_guest_session_grant
        FOREIGN KEY (tenant_id, project_id, grant_id)
            REFERENCES external_access_grants (tenant_id, project_id, id)
            ON DELETE CASCADE,
    CONSTRAINT ck_external_guest_session_expiry
        CHECK (expires_at > created_at)
);

CREATE INDEX idx_external_guest_sessions_grant
    ON external_guest_sessions (tenant_id, project_id, grant_id, expires_at);
