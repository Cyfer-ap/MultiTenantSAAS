import { httpClient } from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    TenantUser,
    TenantUsersQueryParams,
} from '../types/users'

async function getUsers(
    tenantId: string,
    params: TenantUsersQueryParams,
): Promise<PageResponse<TenantUser>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<TenantUser>>
    >(`/api/tenants/${tenantId}/users`, {
        params,
    })

    return response.data.data
}

export const usersApi = {
    getUsers,
}
