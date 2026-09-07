import { httpClient } from '../../../api/httpClient'
import { systemHttpClient } from '../../../api/systemHttpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    TenantSubscriptionHistoryEntry,
    TenantSubscriptionHistoryPage,
} from '../types/subscriptionHistory'

interface SpringPagePayload<T> {
    content: T[]
    number?: number
    page?: number
    size: number
    totalElements: number
    totalPages: number
    first: boolean
    last: boolean
}

function normalizePage<T>(payload: SpringPagePayload<T>): PageResponse<T> {
    return {
        content: payload.content,
        page: payload.page ?? payload.number ?? 0,
        size: payload.size,
        totalElements: payload.totalElements,
        totalPages: payload.totalPages,
        first: payload.first,
        last: payload.last,
    }
}

async function getTenantHistory(
    tenantId: string,
    page: number,
    size: number,
): Promise<TenantSubscriptionHistoryPage> {
    const response = await httpClient.get<
        ApiResponse<SpringPagePayload<TenantSubscriptionHistoryEntry>>
    >(`/api/tenants/${tenantId}/subscription/history`, {
        params: { page, size },
    })

    return normalizePage(response.data.data)
}

async function getSystemTenantHistory(
    tenantId: string,
    page: number,
    size: number,
): Promise<TenantSubscriptionHistoryPage> {
    const response = await systemHttpClient.get<
        ApiResponse<SpringPagePayload<TenantSubscriptionHistoryEntry>>
    >(`/api/system/tenants/${tenantId}/subscription/history`, {
        params: { page, size },
    })

    return normalizePage(response.data.data)
}

export const subscriptionHistoryApi = {
    getTenantHistory,
    getSystemTenantHistory,
}
