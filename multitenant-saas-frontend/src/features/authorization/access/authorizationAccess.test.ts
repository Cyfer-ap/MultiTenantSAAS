import { describe, expect, it } from 'vitest'

import {
    getDefaultAuthorizedPath,
    hasProjectPermission,
    hasTenantPermission,
} from './authorizationAccess'
import {
    authorizationPermissionCodes,
    type CurrentAuthorizationContext,
} from '../types/authorization'

const context: CurrentAuthorizationContext = {
    tenantId: 'tenant-1',
    userId: 'user-1',
    fullName: 'Scoped User',
    email: 'scoped@example.com',
    evaluatedAt: '2026-08-04T12:00:00Z',

    tenantPermissionCodes: [authorizationPermissionCodes.TENANT_READ],

    allPermissionCodes: [
        authorizationPermissionCodes.TENANT_READ,
        authorizationPermissionCodes.PROJECT_READ,
        authorizationPermissionCodes.AUDIT_READ,
    ],

    grants: [
        {
            assignmentId: 'assignment-1',
            roleId: 'role-1',
            roleCode: 'TENANT_READER',
            roleName: 'Tenant Reader',
            roleSource: 'TENANT',
            scopeType: 'TENANT',
            scopeTargetId: null,
            validFrom: '2026-08-01T00:00:00Z',
            validUntil: null,
            permissionCodes: [authorizationPermissionCodes.TENANT_READ],
        },
        {
            assignmentId: 'assignment-2',
            roleId: 'role-2',
            roleCode: 'PROJECT_READER',
            roleName: 'Project Reader',
            roleSource: 'TENANT',
            scopeType: 'PROJECT',
            scopeTargetId: 'project-1',
            validFrom: '2026-08-01T00:00:00Z',
            validUntil: null,
            permissionCodes: [authorizationPermissionCodes.PROJECT_READ],
        },
        {
            assignmentId: 'assignment-3',
            roleId: 'role-3',
            roleCode: 'SELF_AUDITOR',
            roleName: 'Self Auditor',
            roleSource: 'TENANT',
            scopeType: 'SELF',
            scopeTargetId: null,
            validFrom: '2026-08-01T00:00:00Z',
            validUntil: null,
            permissionCodes: [authorizationPermissionCodes.AUDIT_READ],
        },
    ],
}

describe('authorizationAccess', () => {
    it('does not treat scoped permissions as tenant-wide permissions', () => {
        expect(hasTenantPermission(context, authorizationPermissionCodes.TENANT_READ)).toBe(true)

        expect(hasTenantPermission(context, authorizationPermissionCodes.PROJECT_READ)).toBe(false)

        expect(hasTenantPermission(context, authorizationPermissionCodes.AUDIT_READ)).toBe(false)
    })

    it('matches only the project targeted by a project-scoped grant', () => {
        expect(
            hasProjectPermission(context, authorizationPermissionCodes.PROJECT_READ, 'project-1'),
        ).toBe(true)

        expect(
            hasProjectPermission(context, authorizationPermissionCodes.PROJECT_READ, 'project-2'),
        ).toBe(false)
    })

    it('uses the first readable scoped project as the fallback path', () => {
        expect(getDefaultAuthorizedPath(context)).toBe('/projects/project-1')
    })
})
