import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { systemHttpClient } from '../../../api/systemHttpClient'
import { subscriptionHistoryApi } from './subscriptionHistoryApi'

const entry = {
    id: 'history-1',
    subscriptionId: 'subscription-1',
    tenantId: 'tenant-1',
    tenantName: 'Research Lab',
    planId: 'plan-1',
    planCode: 'GROWTH',
    planName: 'Growth',
    planDescription: null,
    billingInterval: 'MONTHLY' as const,
    price: 49,
    currency: 'USD',
    maxUsers: 25,
    maxProjects: 100,
    maxStorageMb: 10240,
    status: 'ACTIVE' as const,
    startedAt: '2026-08-05T12:00:00Z',
    currentPeriodStart: '2026-09-05T12:00:00Z',
    currentPeriodEnd: '2026-10-05T12:00:00Z',
    trialEndsAt: null,
    cancelAtPeriodEnd: false,
    cancelledAt: null,
    billingProvider: 'STRIPE' as const,
    providerSubscriptionId: 'sub_123',
    providerEventCreatedAt: '2026-09-05T12:00:00Z',
    eventType: 'PROVIDER_SYNCHRONIZED' as const,
    recordedAt: '2026-09-05T12:00:01Z',
}

function springPage(number: number) {
    return {
        content: [entry],
        number,
        size: 10,
        totalElements: 21,
        totalPages: 3,
        first: number === 0,
        last: number === 2,
    }
}

describe('subscriptionHistoryApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads tenant history and normalizes the Spring page number', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: springPage(2),
            },
        })

        const result = await subscriptionHistoryApi.getTenantHistory('tenant-1', 2, 10)

        expect(result.page).toBe(2)
        expect(result.content).toEqual([entry])
        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/subscription/history', {
            params: { page: 2, size: 10 },
        })
    })

    it('uses the system-admin history endpoint for platform operations', async () => {
        const get = vi.spyOn(systemHttpClient, 'get').mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: springPage(0),
            },
        })

        await expect(
            subscriptionHistoryApi.getSystemTenantHistory('tenant-1', 0, 20),
        ).resolves.toMatchObject({ page: 0, totalElements: 21 })

        expect(get).toHaveBeenCalledWith('/api/system/tenants/tenant-1/subscription/history', {
            params: { page: 0, size: 20 },
        })
    })
})
