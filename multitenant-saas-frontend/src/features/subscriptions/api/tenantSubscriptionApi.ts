import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    TenantSubscription,
} from '../types/subscriptions'

async function getSubscription(
    tenantId: string,
): Promise<TenantSubscription> {
    const response = await httpClient.get<
        ApiResponse<TenantSubscription>
    >(`/api/tenants/${tenantId}/subscription`)

    return response.data.data
}

export const tenantSubscriptionApi = {
    getSubscription,
}
