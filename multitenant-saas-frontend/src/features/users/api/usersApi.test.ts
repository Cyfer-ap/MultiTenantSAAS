import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { TenantUser } from '../types/users'
import { usersApi } from './usersApi'

const tenantUser: TenantUser = {
    id: 'user-2',
    tenantId: 'tenant-1',
    fullName: 'Grace User',
    email: 'grace@example.com',
    role: 'TENANT_USER',
    status: 'ACTIVE',
    createdAt: '2026-07-16T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

function successfulResponse(user: TenantUser) {
    return {
        data: {
            success: true,
            message: 'Success',
            data: user,
            timestamp: '2026-08-01T10:30:00Z',
        },
    }
}

describe('usersApi management operations', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('creates a user in the selected tenant', async () => {
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValue(successfulResponse(tenantUser))
        const input = {
            fullName: 'Grace User',
            email: 'grace@example.com',
            password: 'Strong@123',
            role: 'TENANT_USER' as const,
        }

        await expect(
            usersApi.createUser('tenant-1', input),
        ).resolves.toEqual(tenantUser)

        expect(post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/users',
            input,
        )
    })

    it('uses the dedicated profile, role, status, and unlock endpoints', async () => {
        const put = vi
            .spyOn(httpClient, 'put')
            .mockResolvedValue(successfulResponse(tenantUser))
        const patch = vi
            .spyOn(httpClient, 'patch')
            .mockResolvedValue(successfulResponse(tenantUser))

        await usersApi.updateUser(
            'tenant-1',
            'user-2',
            {
                fullName: 'Grace User',
                email: 'grace@example.com',
            },
        )
        await usersApi.updateUserRole(
            'tenant-1',
            'user-2',
            { role: 'TENANT_MANAGER' },
        )
        await usersApi.updateUserStatus(
            'tenant-1',
            'user-2',
            { status: 'SUSPENDED' },
        )
        await usersApi.unlockUserLogin(
            'tenant-1',
            'user-2',
        )

        expect(put).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/users/user-2',
            {
                fullName: 'Grace User',
                email: 'grace@example.com',
            },
        )
        expect(patch).toHaveBeenNthCalledWith(
            1,
            '/api/tenants/tenant-1/users/user-2/role',
            { role: 'TENANT_MANAGER' },
        )
        expect(patch).toHaveBeenNthCalledWith(
            2,
            '/api/tenants/tenant-1/users/user-2/status',
            { status: 'SUSPENDED' },
        )
        expect(patch).toHaveBeenNthCalledWith(
            3,
            '/api/tenants/tenant-1/users/user-2/unlock',
        )
    })
})
