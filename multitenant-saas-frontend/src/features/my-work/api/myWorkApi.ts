import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { MyWorkOverview } from '../types/myWork'

async function getOverview(tenantId: string, limit = 50): Promise<MyWorkOverview> {
    const response = await httpClient.get<ApiResponse<MyWorkOverview>>(
        `/api/tenants/${tenantId}/my-work`,
        { params: { limit } },
    )
    return response.data.data
}

export const myWorkApi = {
    getOverview,
}
