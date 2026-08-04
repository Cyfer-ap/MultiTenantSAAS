import {
    describe,
    expect,
    it,
} from 'vitest'

import type {
    CurrentAuthorizationContext,
} from '../../authorization/types/authorization'
import type {
    OrganizationalUnitTree,
} from '../types/organization'
import {
    flattenOrganizationTree,
    hasOrganizationUnitPermission,
    isOrganizationUnitDescendant,
} from './organizationAccess'

const tree: OrganizationalUnitTree[] = [
    {
        id: 'company',
        tenantId: 'tenant-1',
        parentUnitId: null,
        name: 'Company',
        code: 'COMPANY',
        type: 'COMPANY',
        status: 'ACTIVE',
        children: [
            {
                id: 'engineering',
                tenantId: 'tenant-1',
                parentUnitId: 'company',
                name: 'Engineering',
                code: 'ENG',
                type: 'DIVISION',
                status: 'ACTIVE',
                children: [
                    {
                        id: 'platform',
                        tenantId: 'tenant-1',
                        parentUnitId: 'engineering',
                        name: 'Platform',
                        code: 'PLATFORM',
                        type: 'TEAM',
                        status: 'ACTIVE',
                        children: [],
                    },
                ],
            },
        ],
    },
]

function context(
    scopeType: 'ORGANIZATIONAL_UNIT' |
        'ORGANIZATIONAL_SUBTREE',
    scopeTargetId: string,
): CurrentAuthorizationContext {
    return {
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'ada@example.com',
        evaluatedAt: '2026-08-05T00:00:00Z',
        tenantPermissionCodes: [],
        allPermissionCodes: [
            'organization.unit.manage',
        ],
        grants: [
            {
                assignmentId: 'assignment-1',
                roleId: 'role-1',
                roleCode: 'ORG_MANAGER',
                roleName: 'Organization manager',
                roleSource: 'TENANT',
                scopeType,
                scopeTargetId,
                validFrom: '2026-08-01T00:00:00Z',
                validUntil: null,
                permissionCodes: [
                    'organization.unit.manage',
                ],
            },
        ],
    }
}

describe('organization access helpers', () => {
    it('flattens hierarchy depth-first', () => {
        expect(
            flattenOrganizationTree(tree).map(
                ({ id, depth }) => ({
                    id,
                    depth,
                }),
            ),
        ).toEqual([
            { id: 'company', depth: 0 },
            { id: 'engineering', depth: 1 },
            { id: 'platform', depth: 2 },
        ])
    })

    it('inherits subtree permissions to descendants', () => {
        const units = flattenOrganizationTree(tree)

        expect(
            hasOrganizationUnitPermission(
                context(
                    'ORGANIZATIONAL_SUBTREE',
                    'engineering',
                ),
                'organization.unit.manage',
                'platform',
                units,
            ),
        ).toBe(true)
    })

    it('keeps exact-unit permissions local', () => {
        const units = flattenOrganizationTree(tree)

        expect(
            hasOrganizationUnitPermission(
                context(
                    'ORGANIZATIONAL_UNIT',
                    'engineering',
                ),
                'organization.unit.manage',
                'platform',
                units,
            ),
        ).toBe(false)

        expect(
            isOrganizationUnitDescendant(
                units,
                'platform',
                'engineering',
            ),
        ).toBe(true)
    })
})
