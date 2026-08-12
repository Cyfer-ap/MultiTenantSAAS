import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    CreateTenantUserInput,
    TenantUser,
    TenantUsersQueryParams,
    UpdateTenantUserInput,
    UpdateTenantUserRoleInput,
    UpdateTenantUserStatusInput,
} from '../types/users'

async function getUsers(
    tenantId: string,
    params: TenantUsersQueryParams,
): Promise<PageResponse<TenantUser>> {
    const response = await httpClient.get<ApiResponse<PageResponse<TenantUser>>>(
        `/api/tenants/${tenantId}/users`,
        {
            params,
        },
    )

    return response.data.data
}

async function createUser(tenantId: string, input: CreateTenantUserInput): Promise<TenantUser> {
    const response = await httpClient.post<ApiResponse<TenantUser>>(
        `/api/tenants/${tenantId}/users`,
        input,
    )

    return response.data.data
}

async function updateUser(
    tenantId: string,
    userId: string,
    input: UpdateTenantUserInput,
): Promise<TenantUser> {
    const response = await httpClient.put<ApiResponse<TenantUser>>(
        `/api/tenants/${tenantId}/users/${userId}`,
        input,
    )

    return response.data.data
}

async function updateUserRole(
    tenantId: string,
    userId: string,
    input: UpdateTenantUserRoleInput,
): Promise<TenantUser> {
    const response = await httpClient.patch<ApiResponse<TenantUser>>(
        `/api/tenants/${tenantId}/users/${userId}/role`,
        input,
    )

    return response.data.data
}

async function updateUserStatus(
    tenantId: string,
    userId: string,
    input: UpdateTenantUserStatusInput,
): Promise<TenantUser> {
    const response = await httpClient.patch<ApiResponse<TenantUser>>(
        `/api/tenants/${tenantId}/users/${userId}/status`,
        input,
    )

    return response.data.data
}

async function unlockUserLogin(tenantId: string, userId: string): Promise<TenantUser> {
    const response = await httpClient.patch<ApiResponse<TenantUser>>(
        `/api/tenants/${tenantId}/users/${userId}/unlock`,
    )

    return response.data.data
}

export const usersApi = {
    getUsers,
    createUser,
    updateUser,
    updateUserRole,
    updateUserStatus,
    unlockUserLogin,
}
