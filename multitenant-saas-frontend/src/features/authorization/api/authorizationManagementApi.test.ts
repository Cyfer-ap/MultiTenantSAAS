import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type {
    AuthorizationRole,
    AuthorizationUserRoleAssignment,
} from '../types/authorization'
import { authorizationApi } from './authorizationApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-05T00:00:00Z',
        },
    }
}

const role: AuthorizationRole = {
    id: 'role-1',
    tenantId: 'tenant-1',
    code: 'PROJECT_COORDINATOR',
    name: 'Project Coordinator',
    description: null,
    source: 'TENANT',
    status: 'ACTIVE',
    permissions: [],
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

const assignment: AuthorizationUserRoleAssignment = {
    id: 'assignment-1',
    tenantId: 'tenant-1',
    userId: 'user-1',
    userFullName: 'Grace User',
    userEmail: 'grace@example.com',
    roleId: role.id,
    roleCode: role.code,
    roleName: role.name,
    roleSource: 'TENANT',
    scopeType: 'PROJECT',
    scopeTargetId: 'project-1',
    status: 'ACTIVE',
    validFrom: '2026-08-05T00:00:00Z',
    validUntil: null,
    createdByUserId: 'admin-1',
    createdByUserEmail: 'admin@example.com',
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

describe('authorization management API', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads all tenant authorization roles', async () => {
        const get = vi.spyOn(httpClient, 'get')
            .mockResolvedValue(successfulResponse([role]))

        await expect(
            authorizationApi.getRoles('tenant-1'),
        ).resolves.toEqual([role])

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/authorization/roles',
            { params: { activeOnly: false } },
        )
    })

    it('creates a scoped user-role assignment', async () => {
        const post = vi.spyOn(httpClient, 'post')
            .mockResolvedValue(successfulResponse(assignment))

        const input = {
            userId: 'user-1',
            roleId: 'role-1',
            scopeType: 'PROJECT' as const,
            scopeTargetId: 'project-1',
            validFrom: null,
            validUntil: null,
        }

        await expect(
            authorizationApi.createAssignment(
                'tenant-1',
                input,
            ),
        ).resolves.toEqual(assignment)

        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/authorization/assignments',
            input,
        )
    })

    it('deactivates an authorization assignment', async () => {
        const patch = vi.spyOn(httpClient, 'patch')
            .mockResolvedValue(
                successfulResponse({
                    ...assignment,
                    status: 'INACTIVE',
                }),
            )

        await authorizationApi.deactivateAssignment(
            'tenant-1',
            'assignment-1',
        )

        expect(patch).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/authorization/assignments/assignment-1/deactivate',
        )
    })
})
