import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { CurrentAuthorizationContext } from '../types/authorization'

async function getCurrentAuthorizationContext(
    tenantId: string,
): Promise<CurrentAuthorizationContext> {
    const response =
        await httpClient.get<
            ApiResponse<CurrentAuthorizationContext>
        >(
            `/api/tenants/${encodeURIComponent(
                tenantId,
            )}/authorization/me`,
        )

    return response.data.data
}

export const authorizationApi = {
    getCurrentAuthorizationContext,
}
