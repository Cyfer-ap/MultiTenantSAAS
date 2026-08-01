import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { publicHttpClient } from '../../../api/httpClient'
import { systemHttpClient } from '../../../api/systemHttpClient'
import { systemAdminApi } from './systemAdminApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-02T12:00:00Z',
        },
    }
}

const tenant = {
    id: 'tenant-1',
    name: 'Research Lab',
    slug: 'research-lab',
    status: 'ACTIVE' as const,
    createdAt: '2026-08-02T10:00:00Z',
    updatedAt: '2026-08-02T10:00:00Z',
}

describe('systemAdminApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('uses the public system-login endpoint and protected current-admin endpoint', async () => {
        const loginResponse = {
            systemAdminId: 'admin-1',
            fullName: 'Platform Owner',
            email: 'owner@example.com',
            role: 'SYSTEM_ADMIN' as const,
            accessToken: 'system-token',
            tokenType: 'Bearer',
            expiresInSeconds: 3600,
            message: 'System admin login successful',
        }
        const currentAdmin = {
            systemAdminId: 'admin-1',
            fullName: 'Platform Owner',
            email: 'owner@example.com',
            role: 'SYSTEM_ADMIN' as const,
            status: 'ACTIVE' as const,
        }
        const publicPost = vi.spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(loginResponse))
        const protectedGet = vi.spyOn(systemHttpClient, 'get')
            .mockResolvedValue(successfulResponse(currentAdmin))

        await expect(systemAdminApi.login({
            email: 'owner@example.com',
            password: 'Strong@123',
        })).resolves.toEqual(loginResponse)
        await expect(systemAdminApi.getCurrentAdmin()).resolves.toEqual(currentAdmin)

        expect(publicPost).toHaveBeenCalledWith(
            '/api/system/auth/login',
            { email: 'owner@example.com', password: 'Strong@123' },
        )
        expect(protectedGet).toHaveBeenCalledWith('/api/system/auth/me')
    })

    it('loads the global dashboard and filtered tenant directory', async () => {
        const dashboard = {
            totalTenants: 3,
            activeTenants: 2,
            inactiveTenants: 1,
            suspendedTenants: 0,
            totalUsers: 12,
            activeUsers: 10,
            inactiveUsers: 1,
            suspendedUsers: 1,
        }
        const tenantPage = {
            content: [tenant],
            page: 0,
            size: 25,
            totalElements: 1,
            totalPages: 1,
            first: true,
            last: true,
        }
        const get = vi.spyOn(systemHttpClient, 'get')
            .mockResolvedValueOnce(successfulResponse(dashboard))
            .mockResolvedValueOnce(successfulResponse(tenantPage))
        const params = {
            page: 0,
            size: 25,
            sortBy: 'name' as const,
            sortDir: 'asc' as const,
            status: 'ACTIVE' as const,
            search: 'research',
        }

        await expect(systemAdminApi.getDashboardSummary()).resolves.toEqual(dashboard)
        await expect(systemAdminApi.getTenants(params)).resolves.toEqual(tenantPage)

        expect(get).toHaveBeenNthCalledWith(1, '/api/dashboard/summary')
        expect(get).toHaveBeenNthCalledWith(2, '/api/tenants', { params })
    })

    it('updates tenant status and onboards through protected system endpoints', async () => {
        const suspendedTenant = { ...tenant, status: 'SUSPENDED' as const }
        const onboardingResponse = {
            tenant,
            adminUser: {
                id: 'user-1',
                tenantId: tenant.id,
                fullName: 'Grace Admin',
                email: 'grace@example.com',
                role: 'TENANT_ADMIN' as const,
                status: 'ACTIVE' as const,
                createdAt: tenant.createdAt,
                updatedAt: tenant.updatedAt,
            },
            message: 'Tenant onboarded successfully by system admin',
        }
        const patch = vi.spyOn(systemHttpClient, 'patch')
            .mockResolvedValue(successfulResponse(suspendedTenant))
        const post = vi.spyOn(systemHttpClient, 'post')
            .mockResolvedValue(successfulResponse(onboardingResponse))
        const input = {
            tenantName: 'Research Lab',
            tenantSlug: 'research-lab',
            adminFullName: 'Grace Admin',
            adminEmail: 'grace@example.com',
            adminPassword: 'Strong@123',
        }

        await systemAdminApi.updateTenantStatus('tenant-1', { status: 'SUSPENDED' })
        await systemAdminApi.onboardTenant(input)

        expect(patch).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/status',
            { status: 'SUSPENDED' },
        )
        expect(post).toHaveBeenCalledWith(
            '/api/system/onboarding/tenants',
            input,
        )
    })
})
