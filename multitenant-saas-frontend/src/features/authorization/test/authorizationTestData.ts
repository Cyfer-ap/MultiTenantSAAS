import type { CurrentAuthorizationContext } from '../types/authorization'

interface TenantAuthorizationContextOptions {
    tenantId?: string
    userId?: string
    permissionCodes: readonly string[]
}

export function createTenantAuthorizationContext({
    tenantId = 'tenant-1',
    userId = 'user-1',
    permissionCodes,
}: TenantAuthorizationContextOptions): CurrentAuthorizationContext {
    const uniquePermissionCodes = Array.from(new Set(permissionCodes)).sort()

    return {
        tenantId,
        userId,
        fullName: 'Authorization Test User',
        email: 'authorization.test@example.com',
        evaluatedAt: '2026-08-04T12:00:00Z',
        tenantPermissionCodes: uniquePermissionCodes,
        allPermissionCodes: uniquePermissionCodes,
        grants:
            uniquePermissionCodes.length === 0
                ? []
                : [
                      {
                          assignmentId: 'test-assignment',
                          roleId: 'test-role',
                          roleCode: 'TEST_ROLE',
                          roleName: 'Test Role',
                          roleSource: 'TENANT',
                          scopeType: 'TENANT',
                          scopeTargetId: null,
                          validFrom: '2026-08-01T00:00:00Z',
                          validUntil: null,
                          permissionCodes: uniquePermissionCodes,
                      },
                  ],
    }
}

interface ProjectAuthorizationContextOptions {
    tenantId?: string
    userId?: string
    projectId: string
    tenantPermissionCodes?: readonly string[]
    permissionCodes: readonly string[]
}

export function createProjectAuthorizationContext({
    tenantId = 'tenant-1',
    userId = 'user-1',
    projectId,
    tenantPermissionCodes = [],
    permissionCodes,
}: ProjectAuthorizationContextOptions): CurrentAuthorizationContext {
    const uniqueTenantPermissionCodes = Array.from(new Set(tenantPermissionCodes)).sort()
    const uniqueProjectPermissionCodes = Array.from(new Set(permissionCodes)).sort()
    const allPermissionCodes = Array.from(
        new Set([...uniqueTenantPermissionCodes, ...uniqueProjectPermissionCodes]),
    ).sort()

    return {
        tenantId,
        userId,
        fullName: 'Authorization Test User',
        email: 'authorization.test@example.com',
        evaluatedAt: '2026-08-04T12:00:00Z',
        tenantPermissionCodes: uniqueTenantPermissionCodes,
        allPermissionCodes,
        grants: [
            ...(uniqueTenantPermissionCodes.length === 0
                ? []
                : [
                      {
                          assignmentId: 'test-tenant-assignment',
                          roleId: 'test-tenant-role',
                          roleCode: 'TEST_TENANT_ROLE',
                          roleName: 'Test Tenant Role',
                          roleSource: 'TENANT' as const,
                          scopeType: 'TENANT' as const,
                          scopeTargetId: null,
                          validFrom: '2026-08-01T00:00:00Z',
                          validUntil: null,
                          permissionCodes: uniqueTenantPermissionCodes,
                      },
                  ]),
            {
                assignmentId: 'test-project-assignment',
                roleId: 'test-project-role',
                roleCode: 'TEST_PROJECT_ROLE',
                roleName: 'Test Project Role',
                roleSource: 'TENANT',
                scopeType: 'PROJECT',
                scopeTargetId: projectId,
                validFrom: '2026-08-01T00:00:00Z',
                validUntil: null,
                permissionCodes: uniqueProjectPermissionCodes,
            },
        ],
    }
}
