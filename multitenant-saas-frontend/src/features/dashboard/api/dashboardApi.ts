import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { TenantDashboardSummary } from '../types/dashboard'

async function getSummary(): Promise<TenantDashboardSummary> {
    const response = await httpClient.get<ApiResponse<TenantDashboardSummary>>(
        '/api/tenant/dashboard/summary',
    )

    return response.data.data
}

export const dashboardApi = {
    getSummary,
}
