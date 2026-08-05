ALTER TABLE platform_audit_logs
    ALTER COLUMN action SET DATA TYPE VARCHAR(60);

ALTER TABLE platform_audit_logs
    ALTER COLUMN action SET NOT NULL;


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
    '10000000-0000-0000-0000-000000000020',
    NULL,
    'PLATFORM',
    'subscription.read',
    'Read subscription',
    'View the tenant subscription, plan, and lifecycle dates.',
    'SUBSCRIPTION',
    'PLATFORM',
    'ACTIVE',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1
    FROM authorization_permissions
    WHERE catalog_key = 'PLATFORM'
      AND code = 'subscription.read'
);


INSERT INTO authorization_role_permissions (
    id,
    tenant_id,
    role_id,
    permission_id,
    created_at
)
SELECT
    RANDOM_UUID(),
    role.tenant_id,
    role.id,
    permission.id,
    CURRENT_TIMESTAMP
FROM authorization_roles role
JOIN authorization_permissions permission
  ON permission.catalog_key = 'PLATFORM'
 AND permission.code = 'subscription.read'
WHERE role.code = 'ADMIN'
  AND role.source = 'SYSTEM'
  AND NOT EXISTS (
      SELECT 1
      FROM authorization_role_permissions mapping
      WHERE mapping.tenant_id = role.tenant_id
        AND mapping.role_id = role.id
        AND mapping.permission_id = permission.id
  );
