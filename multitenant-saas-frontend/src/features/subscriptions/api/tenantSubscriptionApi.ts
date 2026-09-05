import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    BillingCancellationResponse,
    BillingCheckoutConfiguration,
    BillingCheckoutInput,
    BillingCheckoutSession,
    TenantSubscription,
    TenantSubscriptionEntitlements,
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'

async function getSubscription(tenantId: string): Promise<TenantSubscription> {
    const response = await httpClient.get<ApiResponse<TenantSubscription>>(
        `/api/tenants/${tenantId}/subscription`,
    )

    return response.data.data
}

async function getEntitlements(tenantId: string): Promise<TenantSubscriptionEntitlements> {
    const response = await httpClient.get<ApiResponse<TenantSubscriptionEntitlements>>(
        `/api/tenants/${tenantId}/subscription/entitlements`,
    )

    return response.data.data
}

async function getAccess(tenantId: string): Promise<WorkspaceSubscriptionAccess> {
    const response = await httpClient.get<ApiResponse<WorkspaceSubscriptionAccess>>(
        `/api/tenants/${tenantId}/subscription/access`,
    )

    return response.data.data
}

async function getCheckoutConfiguration(tenantId: string): Promise<BillingCheckoutConfiguration> {
    const response = await httpClient.get<ApiResponse<BillingCheckoutConfiguration>>(
        `/api/tenants/${tenantId}/billing/checkout/configuration`,
    )

    return response.data.data
}

async function createCheckout(
    tenantId: string,
    input: BillingCheckoutInput,
): Promise<BillingCheckoutSession> {
    const response = await httpClient.post<ApiResponse<BillingCheckoutSession>>(
        `/api/tenants/${tenantId}/billing/checkout`,
        input,
    )

    return response.data.data
}

async function cancelSubscription(tenantId: string): Promise<BillingCancellationResponse> {
    const response = await httpClient.post<ApiResponse<BillingCancellationResponse>>(
        `/api/tenants/${tenantId}/billing/cancel`,
    )

    return response.data.data
}

export const tenantSubscriptionApi = {
    getSubscription,
    getCheckoutConfiguration,
    createCheckout,
    cancelSubscription,
    getEntitlements,
    getAccess,
}
