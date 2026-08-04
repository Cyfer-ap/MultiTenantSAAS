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
    const uniquePermissionCodes =
        Array.from(new Set(permissionCodes)).sort()

    return {
        tenantId,
        userId,
        fullName: 'Authorization Test User',
        email: 'authorization.test@example.com',
        evaluatedAt: '2026-08-04T12:00:00Z',
        tenantPermissionCodes: uniquePermissionCodes,
        allPermissionCodes: uniquePermissionCodes,
        grants: uniquePermissionCodes.length === 0
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
