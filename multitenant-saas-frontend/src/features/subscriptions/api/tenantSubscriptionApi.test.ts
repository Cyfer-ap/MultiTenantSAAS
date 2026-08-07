import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { tenantSubscriptionApi } from './tenantSubscriptionApi'

const access = {
    tenantId: 'tenant-1',
    subscriptionStatus: 'ACTIVE' as const,
    accessLevel: 'FULL_ACCESS' as const,
    accessReason: 'ACTIVE' as const,
    serviceAvailable: true,
    mutationsAllowed: true,
    userCreationAllowed: true,
    projectCreationAllowed: true,
    userLimitReached: false,
    projectLimitReached: false,
    cancelAtPeriodEnd: false,
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    evaluatedAt: '2026-08-07T12:00:00Z',
}

const entitlements = {
    tenantId: 'tenant-1',
    subscriptionId: 'subscription-1',
    planId: 'plan-1',
    planCode: 'GROWTH',
    planName: 'Growth',
    subscriptionStatus: 'ACTIVE' as const,
    accessLevel: 'FULL_ACCESS' as const,
    accessReason: 'ACTIVE' as const,
    serviceAvailable: true,
    mutationsAllowed: true,
    cancelAtPeriodEnd: false,
    currentPeriodEnd: '2026-09-05T12:00:00Z',
    trialEndsAt: null,
    evaluatedAt: '2026-08-07T12:00:00Z',
    users: {
        used: 2,
        limit: 25,
        remaining: 23,
        unlimited: false,
        limitReached: false,
        overLimit: false,
        creationAllowed: true,
    },
    projects: {
        used: 7,
        limit: 100,
        remaining: 93,
        unlimited: false,
        limitReached: false,
        overLimit: false,
        creationAllowed: true,
    },
}

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

    it('loads detailed tenant entitlements', async () => {
        const get = vi.spyOn(
            httpClient,
            'get',
        ).mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: entitlements,
            },
        })

        await expect(
            tenantSubscriptionApi.getEntitlements(
                'tenant-1',
            ),
        ).resolves.toEqual(entitlements)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/subscription/entitlements',
        )
    })

    it('loads lightweight workspace access state', async () => {
        const get = vi.spyOn(
            httpClient,
            'get',
        ).mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: access,
            },
        })

        await expect(
            tenantSubscriptionApi.getAccess(
                'tenant-1',
            ),
        ).resolves.toEqual(access)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/subscription/access',
        )
    })
})
