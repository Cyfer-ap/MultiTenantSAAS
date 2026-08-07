-- PostgreSQL baseline representing the application schema after H2 migrations V1-V17.
--
-- This file is only for fresh PostgreSQL databases. Existing H2 migration files are
-- intentionally preserved because changing them would invalidate Flyway checksums.
-- Future portable migrations start at V18 under classpath:db/common.

CREATE TABLE tenants (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uk_tenant_slug UNIQUE (slug),
    CONSTRAINT ck_tenant_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')
    )
);

CREATE TABLE system_admins (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    email VARCHAR(150) NOT NULL,
    failed_login_attempts INTEGER NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_system_admins PRIMARY KEY (id),
    CONSTRAINT uk_system_admin_email UNIQUE (email),
    CONSTRAINT ck_system_admin_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')
    )
);

CREATE TABLE app_users (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    email VARCHAR(150) NOT NULL,
    failed_login_attempts INTEGER NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    locked_until TIMESTAMP WITH TIME ZONE,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID NOT NULL,
    session_version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uk_user_email_per_tenant UNIQUE (tenant_id, email),
    CONSTRAINT uk_app_user_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_app_user_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT ck_app_user_role CHECK (
        role IN ('TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER')
    ),
    CONSTRAINT ck_app_user_status CHECK (
        status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')
    )
);

CREATE TABLE password_reset_tokens (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    used BOOLEAN NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    user_id UUID NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_password_reset_user
    ON password_reset_tokens (user_id);
CREATE INDEX idx_password_reset_expires_at
    ON password_reset_tokens (expires_at);

CREATE TABLE refresh_tokens (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    token_hash VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_refresh_token_user
    ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_token_expires_at
    ON refresh_tokens (expires_at);

CREATE TABLE audit_logs (
    id UUID NOT NULL,
    action VARCHAR(60) NOT NULL,
    actor_type VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    message VARCHAR(500),
    success BOOLEAN NOT NULL,
    actor_system_admin_id UUID,
    actor_user_id UUID,
    target_user_id UUID,
    tenant_id UUID NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_audit_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_audit_actor_user
        FOREIGN KEY (actor_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_audit_target_user
        FOREIGN KEY (target_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_audit_actor_system_admin
        FOREIGN KEY (actor_system_admin_id) REFERENCES system_admins (id),
    CONSTRAINT ck_audit_actor_type CHECK (
        actor_type IS NULL OR
        actor_type IN ('SYSTEM', 'SYSTEM_ADMIN', 'TENANT_USER')
    )
);

CREATE INDEX idx_audit_tenant ON audit_logs (tenant_id);
CREATE INDEX idx_audit_actor_user ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_actor_system_admin ON audit_logs (actor_system_admin_id);
CREATE INDEX idx_audit_target_user ON audit_logs (target_user_id);
CREATE INDEX idx_audit_action ON audit_logs (action);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

CREATE TABLE platform_audit_logs (
    id UUID NOT NULL,
    action VARCHAR(60) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    message VARCHAR(500),
    success BOOLEAN NOT NULL,
    actor_system_admin_id UUID,
    target_system_admin_id UUID,
    CONSTRAINT pk_platform_audit_logs PRIMARY KEY (id),
    CONSTRAINT fk_platform_audit_actor
        FOREIGN KEY (actor_system_admin_id) REFERENCES system_admins (id),
    CONSTRAINT fk_platform_audit_target
        FOREIGN KEY (target_system_admin_id) REFERENCES system_admins (id)
);

CREATE INDEX idx_platform_audit_actor
    ON platform_audit_logs (actor_system_admin_id);
CREATE INDEX idx_platform_audit_target
    ON platform_audit_logs (target_system_admin_id);
CREATE INDEX idx_platform_audit_action
    ON platform_audit_logs (action);
CREATE INDEX idx_platform_audit_created_at
    ON platform_audit_logs (created_at);

CREATE TABLE user_invitations (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    invited_by_user_id UUID,
    invited_by_system_admin_id UUID,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    role VARCHAR(30) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_user_invitations PRIMARY KEY (id),
    CONSTRAINT uk_user_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_invitation_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_user_invitation_actor_user
        FOREIGN KEY (invited_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_user_invitation_actor_system_admin
        FOREIGN KEY (invited_by_system_admin_id) REFERENCES system_admins (id)
);

CREATE INDEX idx_user_invitation_tenant
    ON user_invitations (tenant_id);
CREATE INDEX idx_user_invitation_email
    ON user_invitations (tenant_id, email);
CREATE INDEX idx_user_invitation_status
    ON user_invitations (status);
CREATE INDEX idx_user_invitation_expires_at
    ON user_invitations (expires_at);

CREATE TABLE projects (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_project_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_project_created_by_user
        FOREIGN KEY (created_by_user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_project_tenant ON projects (tenant_id);
CREATE INDEX idx_project_tenant_status ON projects (tenant_id, status);
CREATE INDEX idx_project_tenant_created_at ON projects (tenant_id, created_at);

CREATE TABLE project_members (
    id UUID NOT NULL,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_project_members PRIMARY KEY (id),
    CONSTRAINT uk_project_member UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_member_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_member_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_project_member_assigned_by
        FOREIGN KEY (assigned_by_user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_project_member_project ON project_members (project_id);
CREATE INDEX idx_project_member_user ON project_members (user_id);
CREATE INDEX idx_project_member_project_role ON project_members (project_id, role);

CREATE TABLE project_tasks (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    assignee_user_id UUID,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000),
    status VARCHAR(30) NOT NULL,
    priority VARCHAR(30) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_project_tasks PRIMARY KEY (id),
    CONSTRAINT fk_project_task_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_project_task_project
        FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT fk_project_task_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT fk_project_task_assignee
        FOREIGN KEY (assignee_user_id) REFERENCES app_users (id)
);

CREATE INDEX idx_project_task_tenant ON project_tasks (tenant_id);
CREATE INDEX idx_project_task_project ON project_tasks (project_id);
CREATE INDEX idx_project_task_assignee ON project_tasks (assignee_user_id);
CREATE INDEX idx_project_task_project_status ON project_tasks (project_id, status);
CREATE INDEX idx_project_task_project_priority ON project_tasks (project_id, priority);
CREATE INDEX idx_project_task_due_at ON project_tasks (due_at);

CREATE TABLE organizational_units (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    parent_unit_id UUID,
    name VARCHAR(150) NOT NULL,
    code VARCHAR(100),
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_organizational_units PRIMARY KEY (id),
    CONSTRAINT uk_organizational_unit_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_organizational_unit_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_organizational_unit_parent
        FOREIGN KEY (parent_unit_id) REFERENCES organizational_units (id),
    CONSTRAINT ck_organizational_unit_not_self_parent
        CHECK (parent_unit_id IS NULL OR parent_unit_id <> id)
);

CREATE INDEX idx_organizational_unit_tenant
    ON organizational_units (tenant_id);
CREATE INDEX idx_organizational_unit_parent
    ON organizational_units (tenant_id, parent_unit_id);
CREATE INDEX idx_organizational_unit_status
    ON organizational_units (tenant_id, status);
CREATE INDEX idx_organizational_unit_type
    ON organizational_units (tenant_id, type);

CREATE TABLE organizational_unit_closure (
    tenant_id UUID NOT NULL,
    ancestor_unit_id UUID NOT NULL,
    descendant_unit_id UUID NOT NULL,
    depth INTEGER NOT NULL,
    CONSTRAINT pk_organizational_unit_closure
        PRIMARY KEY (tenant_id, ancestor_unit_id, descendant_unit_id),
    CONSTRAINT fk_organizational_unit_closure_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_organizational_unit_closure_ancestor
        FOREIGN KEY (ancestor_unit_id) REFERENCES organizational_units (id),
    CONSTRAINT fk_organizational_unit_closure_descendant
        FOREIGN KEY (descendant_unit_id) REFERENCES organizational_units (id),
    CONSTRAINT ck_organizational_unit_closure_depth CHECK (depth >= 0),
    CONSTRAINT ck_organizational_unit_closure_self_depth CHECK (
        (ancestor_unit_id = descendant_unit_id AND depth = 0)
        OR
        (ancestor_unit_id <> descendant_unit_id AND depth > 0)
    )
);

CREATE INDEX idx_organizational_unit_closure_ancestor
    ON organizational_unit_closure (tenant_id, ancestor_unit_id, depth);
CREATE INDEX idx_organizational_unit_closure_descendant
    ON organizational_unit_closure (tenant_id, descendant_unit_id, depth);

CREATE TABLE user_organization_assignments (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    organizational_unit_id UUID NOT NULL,
    reports_to_assignment_id UUID,
    position_title VARCHAR(150),
    primary_assignment BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_user_organization_assignments PRIMARY KEY (id),
    CONSTRAINT uk_user_organization_assignment_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_user_organization_assignment_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_user_organization_assignment_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_user_organization_assignment_unit
        FOREIGN KEY (organizational_unit_id) REFERENCES organizational_units (id),
    CONSTRAINT fk_user_organization_assignment_reports_to
        FOREIGN KEY (reports_to_assignment_id) REFERENCES user_organization_assignments (id),
    CONSTRAINT fk_user_organization_assignment_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT ck_user_organization_assignment_validity
        CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT ck_user_organization_assignment_not_self_report
        CHECK (reports_to_assignment_id IS NULL OR reports_to_assignment_id <> id)
);

CREATE INDEX idx_user_org_assignment_user
    ON user_organization_assignments (tenant_id, user_id, status);
CREATE INDEX idx_user_org_assignment_unit
    ON user_organization_assignments (tenant_id, organizational_unit_id, status);
CREATE INDEX idx_user_org_assignment_primary
    ON user_organization_assignments (
        tenant_id,
        user_id,
        primary_assignment,
        status
    );
CREATE INDEX idx_user_org_assignment_reports_to
    ON user_organization_assignments (
        tenant_id,
        reports_to_assignment_id
    );
CREATE INDEX idx_user_org_assignment_validity
    ON user_organization_assignments (tenant_id, valid_from, valid_until);

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
    CONSTRAINT pk_authorization_permissions PRIMARY KEY (id),
    CONSTRAINT uk_authorization_permission_catalog_code UNIQUE (catalog_key, code),
    CONSTRAINT fk_authorization_permission_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT ck_authorization_permission_owner CHECK (
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
    CONSTRAINT ck_authorization_permission_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    )
);

CREATE INDEX idx_authorization_permission_source_status
    ON authorization_permissions (source, status);
CREATE INDEX idx_authorization_permission_tenant_status
    ON authorization_permissions (tenant_id, status);
CREATE INDEX idx_authorization_permission_category
    ON authorization_permissions (category);

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
    ('10000000-0000-0000-0000-000000000001', NULL, 'PLATFORM', 'tenant.read', 'Read tenant', 'View tenant details and configuration.', 'TENANT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000002', NULL, 'PLATFORM', 'tenant.update', 'Update tenant', 'Update tenant details and configuration.', 'TENANT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000003', NULL, 'PLATFORM', 'user.read', 'Read users', 'View tenant users and user profiles.', 'USER', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000004', NULL, 'PLATFORM', 'user.create', 'Create users', 'Create or invite tenant users.', 'USER', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000005', NULL, 'PLATFORM', 'user.update', 'Update users', 'Update tenant user profile information.', 'USER', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000006', NULL, 'PLATFORM', 'user.status.update', 'Update user status', 'Activate, suspend, or deactivate tenant users.', 'USER', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000007', NULL, 'PLATFORM', 'organization.unit.read', 'Read organizational units', 'View the organizational hierarchy.', 'ORGANIZATION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000008', NULL, 'PLATFORM', 'organization.unit.manage', 'Manage organizational units', 'Create, update, move, and deactivate organizational units.', 'ORGANIZATION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000009', NULL, 'PLATFORM', 'organization.assignment.read', 'Read organizational assignments', 'View user organizational assignments and reporting relationships.', 'ORGANIZATION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000010', NULL, 'PLATFORM', 'organization.assignment.manage', 'Manage organizational assignments', 'Create and deactivate organizational assignments.', 'ORGANIZATION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000011', NULL, 'PLATFORM', 'project.read', 'Read projects', 'View projects and project details.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000012', NULL, 'PLATFORM', 'project.create', 'Create projects', 'Create projects within an authorized scope.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000013', NULL, 'PLATFORM', 'project.update', 'Update projects', 'Update projects within an authorized scope.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000014', NULL, 'PLATFORM', 'project.archive', 'Archive projects', 'Archive projects within an authorized scope.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000015', NULL, 'PLATFORM', 'project.member.manage', 'Manage project members', 'Add, update, and remove project members.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000016', NULL, 'PLATFORM', 'project.task.read', 'Read project tasks', 'View project tasks within an authorized scope.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000017', NULL, 'PLATFORM', 'project.task.manage', 'Manage project tasks', 'Create and update project tasks within an authorized scope.', 'PROJECT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000018', NULL, 'PLATFORM', 'audit.read', 'Read audit logs', 'View tenant audit history.', 'AUDIT', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000019', NULL, 'PLATFORM', 'authorization.manage', 'Manage authorization', 'Manage permissions, roles, and scoped role assignments.', 'AUTHORIZATION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('10000000-0000-0000-0000-000000000020', NULL, 'PLATFORM', 'subscription.read', 'Read subscription', 'View the tenant subscription, plan, and lifecycle dates.', 'SUBSCRIPTION', 'PLATFORM', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

CREATE TABLE authorization_roles (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    source VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_authorization_roles PRIMARY KEY (id),
    CONSTRAINT uk_authorization_role_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT fk_authorization_role_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT ck_authorization_role_source CHECK (
        source IN ('SYSTEM', 'TENANT')
    ),
    CONSTRAINT ck_authorization_role_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    )
);

CREATE INDEX idx_authorization_role_tenant_status
    ON authorization_roles (tenant_id, status);
CREATE INDEX idx_authorization_role_tenant_source
    ON authorization_roles (tenant_id, source);

CREATE TABLE authorization_role_permissions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_authorization_role_permissions PRIMARY KEY (id),
    CONSTRAINT uk_authorization_role_permission UNIQUE (role_id, permission_id),
    CONSTRAINT fk_authorization_role_permission_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_authorization_role_permission_role
        FOREIGN KEY (role_id) REFERENCES authorization_roles (id),
    CONSTRAINT fk_authorization_role_permission_permission
        FOREIGN KEY (permission_id) REFERENCES authorization_permissions (id)
);

CREATE INDEX idx_authorization_role_permission_role
    ON authorization_role_permissions (tenant_id, role_id);
CREATE INDEX idx_authorization_role_permission_permission
    ON authorization_role_permissions (permission_id);

CREATE TABLE authorization_user_role_assignments (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    scope_type VARCHAR(40) NOT NULL,
    scope_target_id UUID,
    scope_key VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
    valid_until TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_auth_user_role_assignments PRIMARY KEY (id),
    CONSTRAINT fk_auth_user_role_assignment_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_auth_user_role_assignment_user
        FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT fk_auth_user_role_assignment_role
        FOREIGN KEY (role_id) REFERENCES authorization_roles (id),
    CONSTRAINT fk_auth_user_role_assignment_created_by
        FOREIGN KEY (created_by_user_id) REFERENCES app_users (id),
    CONSTRAINT ck_auth_user_role_assignment_scope CHECK (
        (
            scope_type IN ('TENANT', 'SELF')
            AND scope_target_id IS NULL
        )
        OR
        (
            scope_type IN (
                'ORGANIZATIONAL_UNIT',
                'ORGANIZATIONAL_SUBTREE',
                'DIRECT_REPORTS',
                'PROJECT'
            )
            AND scope_target_id IS NOT NULL
        )
    ),
    CONSTRAINT ck_auth_user_role_assignment_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    ),
    CONSTRAINT ck_auth_user_role_assignment_validity CHECK (
        valid_until IS NULL OR valid_until > valid_from
    )
);

CREATE INDEX idx_auth_user_role_assignment_user
    ON authorization_user_role_assignments (tenant_id, user_id, status);
CREATE INDEX idx_auth_user_role_assignment_role
    ON authorization_user_role_assignments (tenant_id, role_id, status);
CREATE INDEX idx_auth_user_role_assignment_scope
    ON authorization_user_role_assignments (tenant_id, scope_type, scope_key, status);
CREATE INDEX idx_auth_user_role_assignment_validity
    ON authorization_user_role_assignments (tenant_id, valid_from, valid_until);

CREATE TABLE subscription_plans (
    id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    billing_interval VARCHAR(20) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    max_users INTEGER,
    max_projects INTEGER,
    max_storage_mb BIGINT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_subscription_plans PRIMARY KEY (id),
    CONSTRAINT uk_subscription_plan_code UNIQUE (code),
    CONSTRAINT ck_subscription_plan_price CHECK (price >= 0),
    CONSTRAINT ck_subscription_plan_max_users CHECK (
        max_users IS NULL OR max_users >= 0
    ),
    CONSTRAINT ck_subscription_plan_max_projects CHECK (
        max_projects IS NULL OR max_projects >= 0
    ),
    CONSTRAINT ck_subscription_plan_max_storage CHECK (
        max_storage_mb IS NULL OR max_storage_mb >= 0
    ),
    CONSTRAINT ck_subscription_plan_status CHECK (
        status IN ('ACTIVE', 'INACTIVE')
    ),
    CONSTRAINT ck_subscription_plan_interval CHECK (
        billing_interval IN ('MONTHLY', 'YEARLY')
    )
);

CREATE INDEX idx_subscription_plan_status_price
    ON subscription_plans (status, price);

CREATE TABLE tenant_subscriptions (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_start TIMESTAMP WITH TIME ZONE NOT NULL,
    current_period_end TIMESTAMP WITH TIME ZONE NOT NULL,
    trial_ends_at TIMESTAMP WITH TIME ZONE,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    cancelled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_tenant_subscriptions PRIMARY KEY (id),
    CONSTRAINT uk_tenant_subscription_tenant UNIQUE (tenant_id),
    CONSTRAINT fk_tenant_subscription_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants (id),
    CONSTRAINT fk_tenant_subscription_plan
        FOREIGN KEY (plan_id) REFERENCES subscription_plans (id),
    CONSTRAINT ck_tenant_subscription_status CHECK (
        status IN (
            'TRIALING',
            'ACTIVE',
            'PAST_DUE',
            'CANCELLED',
            'EXPIRED'
        )
    ),
    CONSTRAINT ck_tenant_subscription_period CHECK (
        current_period_end > current_period_start
    ),
    CONSTRAINT ck_tenant_subscription_trial CHECK (
        trial_ends_at IS NULL
        OR (
            trial_ends_at >= current_period_start
            AND trial_ends_at <= current_period_end
        )
    )
);

CREATE INDEX idx_tenant_subscription_plan_status
    ON tenant_subscriptions (plan_id, status);
CREATE INDEX idx_tenant_subscription_period_end
    ON tenant_subscriptions (status, current_period_end);
