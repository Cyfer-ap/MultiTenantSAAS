import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { GlobalSearchResponse } from '../types/search'

async function search(tenantId: string, query: string, limit = 12): Promise<GlobalSearchResponse> {
    const response = await httpClient.get<ApiResponse<GlobalSearchResponse>>(
        `/api/tenants/${tenantId}/search`,
        {
            params: {
                q: query,
                limit,
            },
        },
    )

    return response.data.data
}

export const globalSearchApi = {
    search,
}
