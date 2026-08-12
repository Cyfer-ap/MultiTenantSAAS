import { systemHttpClient } from '../../../api/systemHttpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ChangeTenantSubscriptionPlanInput,
    CreateSubscriptionPlanInput,
    StartTenantSubscriptionInput,
    SubscriptionPlan,
    SubscriptionPlanStatus,
    TenantSubscription,
    UpdateSubscriptionPlanInput,
    UpdateTenantSubscriptionLifecycleInput,
} from '../types/subscriptions'

async function getPlans(activeOnly = false): Promise<SubscriptionPlan[]> {
    const response = await systemHttpClient.get<ApiResponse<SubscriptionPlan[]>>(
        '/api/system/subscription-plans',
        {
            params: { activeOnly },
        },
    )
    return response.data.data
}

async function createPlan(input: CreateSubscriptionPlanInput): Promise<SubscriptionPlan> {
    const response = await systemHttpClient.post<ApiResponse<SubscriptionPlan>>(
        '/api/system/subscription-plans',
        input,
    )
    return response.data.data
}

async function updatePlan(
    planId: string,
    input: UpdateSubscriptionPlanInput,
): Promise<SubscriptionPlan> {
    const response = await systemHttpClient.put<ApiResponse<SubscriptionPlan>>(
        `/api/system/subscription-plans/${planId}`,
        input,
    )
    return response.data.data
}

async function updatePlanStatus(
    planId: string,
    status: SubscriptionPlanStatus,
): Promise<SubscriptionPlan> {
    const response = await systemHttpClient.patch<ApiResponse<SubscriptionPlan>>(
        `/api/system/subscription-plans/${planId}/status`,
        { status },
    )
    return response.data.data
}

async function getTenantSubscription(tenantId: string): Promise<TenantSubscription> {
    const response = await systemHttpClient.get<ApiResponse<TenantSubscription>>(
        `/api/system/tenants/${tenantId}/subscription`,
    )
    return response.data.data
}

async function startTenantSubscription(
    tenantId: string,
    input: StartTenantSubscriptionInput,
): Promise<TenantSubscription> {
    const response = await systemHttpClient.post<ApiResponse<TenantSubscription>>(
        `/api/system/tenants/${tenantId}/subscription`,
        input,
    )
    return response.data.data
}

async function changeTenantPlan(
    tenantId: string,
    input: ChangeTenantSubscriptionPlanInput,
): Promise<TenantSubscription> {
    const response = await systemHttpClient.put<ApiResponse<TenantSubscription>>(
        `/api/system/tenants/${tenantId}/subscription/plan`,
        input,
    )
    return response.data.data
}

async function updateTenantLifecycle(
    tenantId: string,
    input: UpdateTenantSubscriptionLifecycleInput,
): Promise<TenantSubscription> {
    const response = await systemHttpClient.patch<ApiResponse<TenantSubscription>>(
        `/api/system/tenants/${tenantId}/subscription/lifecycle`,
        input,
    )
    return response.data.data
}

export const systemSubscriptionApi = {
    getPlans,
    createPlan,
    updatePlan,
    updatePlanStatus,
    getTenantSubscription,
    startTenantSubscription,
    changeTenantPlan,
    updateTenantLifecycle,
}
