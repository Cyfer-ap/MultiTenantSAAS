INSERT INTO authorization_permissions (
    id,
    tenant_id,
    catalog_key,
    code,
    name,
    description,
    category,
    source,
    status,
    created_at,
    updated_at
)
SELECT
    '10000000-0000-0000-0000-000000000021',
    NULL,
    'PLATFORM',
    'authorization.delegate',
    'Delegate authorization',
    'Delegate a bounded subset of existing direct authorization authority.',
    'AUTHORIZATION',
    'PLATFORM',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM authorization_permissions
    WHERE catalog_key = 'PLATFORM'
      AND code = 'authorization.delegate'
);

CREATE TABLE authorization_delegations (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    delegator_user_id UUID NOT NULL,
    delegate_user_id UUID NOT NULL,
    parent_authority_assignment_id UUID NOT NULL,
    delegated_assignment_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    revoked_by_user_id UUID,
    CONSTRAINT uk_authorization_delegation_assignment
        UNIQUE (delegated_assignment_id),
    CONSTRAINT ck_authorization_delegation_status
        CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_authorization_delegation_distinct_users
        CHECK (delegator_user_id <> delegate_user_id),
    CONSTRAINT fk_authorization_delegation_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_authorization_delegation_delegator
        FOREIGN KEY (delegator_user_id) REFERENCES app_users(id),
    CONSTRAINT fk_authorization_delegation_delegate
        FOREIGN KEY (delegate_user_id) REFERENCES app_users(id),
    CONSTRAINT fk_authorization_delegation_parent_assignment
        FOREIGN KEY (parent_authority_assignment_id)
            REFERENCES authorization_user_role_assignments(id),
    CONSTRAINT fk_authorization_delegation_delegated_assignment
        FOREIGN KEY (delegated_assignment_id)
            REFERENCES authorization_user_role_assignments(id),
    CONSTRAINT fk_authorization_delegation_revoked_by
        FOREIGN KEY (revoked_by_user_id) REFERENCES app_users(id)
);

CREATE INDEX idx_authorization_delegation_delegator
    ON authorization_delegations (tenant_id, delegator_user_id, status);

CREATE INDEX idx_authorization_delegation_delegate
    ON authorization_delegations (tenant_id, delegate_user_id, status);

CREATE INDEX idx_authorization_delegation_parent
    ON authorization_delegations (tenant_id, parent_authority_assignment_id, status);
