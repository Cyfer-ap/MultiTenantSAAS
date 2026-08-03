CREATE TABLE authorization_permissions (
                                           id UUID NOT NULL,
                                           tenant_id UUID,
                                           catalog_key VARCHAR(64) NOT NULL,
                                           code VARCHAR(120) NOT NULL,
                                           name VARCHAR(150) NOT NULL,
                                           description VARCHAR(500),
                                           category VARCHAR(60) NOT NULL,
                                           source VARCHAR(30) NOT NULL,
                                           status VARCHAR(30) NOT NULL,
                                           created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                           updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                           CONSTRAINT pk_authorization_permissions
                                               PRIMARY KEY (id),

                                           CONSTRAINT uk_authorization_permission_catalog_code
                                               UNIQUE (
                                                       catalog_key,
                                                       code
                                                   ),

                                           CONSTRAINT fk_authorization_permission_tenant
                                               FOREIGN KEY (tenant_id)
                                                   REFERENCES tenants (id),

                                           CONSTRAINT ck_authorization_permission_owner
                                               CHECK (
                                                   (
                                                       source = 'PLATFORM'
                                                           AND tenant_id IS NULL
                                                           AND catalog_key = 'PLATFORM'
                                                       )
                                                       OR
                                                   (
                                                       source = 'TENANT'
                                                           AND tenant_id IS NOT NULL
                                                           AND catalog_key <> 'PLATFORM'
                                                       )
                                                   ),

                                           CONSTRAINT ck_authorization_permission_status
                                               CHECK (
                                                   status IN (
                                                              'ACTIVE',
                                                              'INACTIVE'
                                                       )
                                                   )
);


CREATE INDEX idx_authorization_permission_source_status
    ON authorization_permissions (
                                  source,
                                  status
        );


CREATE INDEX idx_authorization_permission_tenant_status
    ON authorization_permissions (
                                  tenant_id,
                                  status
        );


CREATE INDEX idx_authorization_permission_category
    ON authorization_permissions (
                                  category
        );


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
VALUES
    (
        '10000000-0000-0000-0000-000000000001',
        NULL,
        'PLATFORM',
        'tenant.read',
        'Read tenant',
        'View tenant details and configuration.',
        'TENANT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000002',
        NULL,
        'PLATFORM',
        'tenant.update',
        'Update tenant',
        'Update tenant details and configuration.',
        'TENANT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000003',
        NULL,
        'PLATFORM',
        'user.read',
        'Read users',
        'View tenant users and user profiles.',
        'USER',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000004',
        NULL,
        'PLATFORM',
        'user.create',
        'Create users',
        'Create or invite tenant users.',
        'USER',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000005',
        NULL,
        'PLATFORM',
        'user.update',
        'Update users',
        'Update tenant user profile information.',
        'USER',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000006',
        NULL,
        'PLATFORM',
        'user.status.update',
        'Update user status',
        'Activate, suspend, or deactivate tenant users.',
        'USER',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000007',
        NULL,
        'PLATFORM',
        'organization.unit.read',
        'Read organizational units',
        'View the organizational hierarchy.',
        'ORGANIZATION',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000008',
        NULL,
        'PLATFORM',
        'organization.unit.manage',
        'Manage organizational units',
        'Create, update, move, and deactivate organizational units.',
        'ORGANIZATION',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000009',
        NULL,
        'PLATFORM',
        'organization.assignment.read',
        'Read organizational assignments',
        'View user organizational assignments and reporting relationships.',
        'ORGANIZATION',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000010',
        NULL,
        'PLATFORM',
        'organization.assignment.manage',
        'Manage organizational assignments',
        'Create and deactivate organizational assignments.',
        'ORGANIZATION',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000011',
        NULL,
        'PLATFORM',
        'project.read',
        'Read projects',
        'View projects and project details.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000012',
        NULL,
        'PLATFORM',
        'project.create',
        'Create projects',
        'Create projects within an authorized scope.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000013',
        NULL,
        'PLATFORM',
        'project.update',
        'Update projects',
        'Update projects within an authorized scope.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000014',
        NULL,
        'PLATFORM',
        'project.archive',
        'Archive projects',
        'Archive projects within an authorized scope.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000015',
        NULL,
        'PLATFORM',
        'project.member.manage',
        'Manage project members',
        'Add, update, and remove project members.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000016',
        NULL,
        'PLATFORM',
        'project.task.read',
        'Read project tasks',
        'View project tasks within an authorized scope.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000017',
        NULL,
        'PLATFORM',
        'project.task.manage',
        'Manage project tasks',
        'Create and update project tasks within an authorized scope.',
        'PROJECT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000018',
        NULL,
        'PLATFORM',
        'audit.read',
        'Read audit logs',
        'View tenant audit history.',
        'AUDIT',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        '10000000-0000-0000-0000-000000000019',
        NULL,
        'PLATFORM',
        'authorization.manage',
        'Manage authorization',
        'Manage permissions, roles, and scoped role assignments.',
        'AUTHORIZATION',
        'PLATFORM',
        'ACTIVE',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );