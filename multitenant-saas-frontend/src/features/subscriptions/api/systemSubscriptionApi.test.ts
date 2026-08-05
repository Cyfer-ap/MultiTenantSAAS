import { beforeEach, describe, expect, it, vi } from 'vitest'

import { systemHttpClient } from '../../../api/systemHttpClient'
import { systemSubscriptionApi } from './systemSubscriptionApi'

function successfulResponse<T>(data: T) {
    return { data: { success: true, message: 'Success', data } }
}

const plan = {
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
}

const subscription = {
    id: 'subscription-1',
    tenantId: 'tenant-1',
    tenantName: 'Research Lab',
    plan,
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

describe('systemSubscriptionApi', () => {
    beforeEach(() => { vi.restoreAllMocks() })

    it('manages the subscription plan catalog', async () => {
        const get = vi.spyOn(systemHttpClient, 'get').mockResolvedValue(successfulResponse([plan]))
        const post = vi.spyOn(systemHttpClient, 'post').mockResolvedValue(successfulResponse(plan))
        const put = vi.spyOn(systemHttpClient, 'put').mockResolvedValue(successfulResponse(plan))
        const patch = vi.spyOn(systemHttpClient, 'patch').mockResolvedValue(successfulResponse(plan))
        const createInput = { code: 'GROWTH', name: 'Growth', description: null, billingInterval: 'MONTHLY' as const, price: 49, currency: 'USD', maxUsers: 25, maxProjects: 100, maxStorageMb: 10240 }
        const withoutCode = {
            name: createInput.name,
            description: createInput.description,
            billingInterval: createInput.billingInterval,
            price: createInput.price,
            currency: createInput.currency,
            maxUsers: createInput.maxUsers,
            maxProjects: createInput.maxProjects,
            maxStorageMb: createInput.maxStorageMb,
        }

        await systemSubscriptionApi.getPlans(false)
        await systemSubscriptionApi.createPlan(createInput)
        await systemSubscriptionApi.updatePlan(plan.id, withoutCode)
        await systemSubscriptionApi.updatePlanStatus(plan.id, 'INACTIVE')

        expect(get).toHaveBeenCalledWith('/api/system/subscription-plans', { params: { activeOnly: false } })
        expect(post).toHaveBeenCalledWith('/api/system/subscription-plans', createInput)
        expect(put).toHaveBeenCalledWith('/api/system/subscription-plans/plan-1', withoutCode)
        expect(patch).toHaveBeenCalledWith('/api/system/subscription-plans/plan-1/status', { status: 'INACTIVE' })
    })

    it('manages a tenant subscription through protected system endpoints', async () => {
        const get = vi.spyOn(systemHttpClient, 'get').mockResolvedValue(successfulResponse(subscription))
        const post = vi.spyOn(systemHttpClient, 'post').mockResolvedValue(successfulResponse(subscription))
        const put = vi.spyOn(systemHttpClient, 'put').mockResolvedValue(successfulResponse(subscription))
        const patch = vi.spyOn(systemHttpClient, 'patch').mockResolvedValue(successfulResponse(subscription))
        const startInput = { planId: plan.id, status: 'ACTIVE' as const, startedAt: subscription.startedAt, currentPeriodStart: subscription.currentPeriodStart, currentPeriodEnd: subscription.currentPeriodEnd, trialEndsAt: null, cancelAtPeriodEnd: false }
        const changeInput = { planId: plan.id, currentPeriodStart: subscription.currentPeriodStart, currentPeriodEnd: subscription.currentPeriodEnd }
        const lifecycleInput = { status: 'PAST_DUE' as const, cancelAtPeriodEnd: false, currentPeriodEnd: subscription.currentPeriodEnd, trialEndsAt: null }

        await systemSubscriptionApi.getTenantSubscription('tenant-1')
        await systemSubscriptionApi.startTenantSubscription('tenant-1', startInput)
        await systemSubscriptionApi.changeTenantPlan('tenant-1', changeInput)
        await systemSubscriptionApi.updateTenantLifecycle('tenant-1', lifecycleInput)

        expect(get).toHaveBeenCalledWith('/api/system/tenants/tenant-1/subscription')
        expect(post).toHaveBeenCalledWith('/api/system/tenants/tenant-1/subscription', startInput)
        expect(put).toHaveBeenCalledWith('/api/system/tenants/tenant-1/subscription/plan', changeInput)
        expect(patch).toHaveBeenCalledWith('/api/system/tenants/tenant-1/subscription/lifecycle', lifecycleInput)
    })
})
