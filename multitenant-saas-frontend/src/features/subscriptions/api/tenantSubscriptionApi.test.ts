import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { tenantSubscriptionApi } from './tenantSubscriptionApi'

const subscription = {
    id: 'subscription-1',
    tenantId: 'tenant-1',
    tenantName: 'Research Lab',
    plan: {
        id: 'plan-1',
        code: 'GROWTH',
        name: 'Growth',
        description: null,
        billingInterval: 'MONTHLY' as const,
        price: 49,
        currency: 'USD',
        maxUsers: 25,
        maxProjects: 100,
        maxStorageMb: 10240,
        status: 'ACTIVE' as const,
        createdAt: '2026-08-05T12:00:00Z',
        updatedAt: '2026-08-05T12:00:00Z',
    },
    status: 'ACTIVE' as const,
    startedAt: '2026-08-05T12:00:00Z',
    currentPeriodStart: '2026-08-05T12:00:00Z',
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    cancelAtPeriodEnd: false,
    cancelledAt: null,
    createdAt: '2026-08-05T12:00:00Z',
    updatedAt: '2026-08-05T12:00:00Z',
}

describe('tenantSubscriptionApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads the current tenant subscription', async () => {
        const get = vi.spyOn(
            httpClient,
            'get',
        ).mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: subscription,
            },
        })

        await expect(
            tenantSubscriptionApi.getSubscription(
                'tenant-1',
            ),
        ).resolves.toEqual(subscription)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/subscription',
        )
    })
})
