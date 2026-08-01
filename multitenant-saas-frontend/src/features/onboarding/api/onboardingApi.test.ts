import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { publicHttpClient } from '../../../api/httpClient'
import { onboardingApi } from './onboardingApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-01T12:00:00Z',
        },
    }
}

describe('onboardingApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('creates a tenant through the public onboarding endpoint', async () => {
        const input = {
            tenantName: 'Research Workspace',
            tenantSlug: 'research-workspace',
            adminFullName: 'Grace Admin',
            adminEmail: 'grace@example.com',
            adminPassword: 'Strong@123',
        }
        const response = {
            tenant: {
                id: 'tenant-1',
                name: 'Research Workspace',
                slug: 'research-workspace',
                status: 'ACTIVE' as const,
                createdAt: '2026-08-01T12:00:00Z',
                updatedAt: '2026-08-01T12:00:00Z',
            },
            adminUser: {
                id: 'user-1',
                tenantId: 'tenant-1',
                fullName: 'Grace Admin',
                email: 'grace@example.com',
                role: 'TENANT_ADMIN' as const,
                status: 'ACTIVE' as const,
                createdAt: '2026-08-01T12:00:00Z',
                updatedAt: '2026-08-01T12:00:00Z',
            },
            message: 'Tenant onboarded successfully.',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(
            onboardingApi.onboardTenant(input),
        ).resolves.toEqual(response)

        expect(post).toHaveBeenCalledWith(
            '/api/onboarding/tenants',
            input,
        )
    })
})
