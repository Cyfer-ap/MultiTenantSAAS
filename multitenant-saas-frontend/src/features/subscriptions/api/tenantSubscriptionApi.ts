import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    TenantSubscription,
    TenantSubscriptionEntitlements,
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'

async function getSubscription(
    tenantId: string,
): Promise<TenantSubscription> {
    const response = await httpClient.get<
        ApiResponse<TenantSubscription>
    >(`/api/tenants/${tenantId}/subscription`)

    return response.data.data
}

async function getEntitlements(
    tenantId: string,
): Promise<TenantSubscriptionEntitlements> {
    const response = await httpClient.get<
        ApiResponse<TenantSubscriptionEntitlements>
    >(
        `/api/tenants/${tenantId}/subscription/entitlements`,
    )

    return response.data.data
}

async function getAccess(
    tenantId: string,
): Promise<WorkspaceSubscriptionAccess> {
    const response = await httpClient.get<
        ApiResponse<WorkspaceSubscriptionAccess>
    >(`/api/tenants/${tenantId}/subscription/access`)

    return response.data.data
}

export const tenantSubscriptionApi = {
    getSubscription,
    getEntitlements,
    getAccess,
}
