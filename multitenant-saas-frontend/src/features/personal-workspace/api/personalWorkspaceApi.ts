import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    PersonalResourceType,
    PersonalWorkspaceItem,
    PersonalWorkspaceOverview,
} from '../types/personalWorkspace'

async function getOverview(tenantId: string, limit = 12): Promise<PersonalWorkspaceOverview> {
    const response = await httpClient.get<ApiResponse<PersonalWorkspaceOverview>>(
        `/api/tenants/${tenantId}/personal-workspace`,
        { params: { limit } },
    )
    return response.data.data
}

async function favorite(
    tenantId: string,
    type: PersonalResourceType,
    resourceId: string,
): Promise<PersonalWorkspaceItem> {
    const response = await httpClient.put<ApiResponse<PersonalWorkspaceItem>>(
        `/api/tenants/${tenantId}/personal-workspace/favorites/${type}/${resourceId}`,
    )
    return response.data.data
}

async function unfavorite(
    tenantId: string,
    type: PersonalResourceType,
    resourceId: string,
): Promise<void> {
    await httpClient.delete(
        `/api/tenants/${tenantId}/personal-workspace/favorites/${type}/${resourceId}`,
    )
}

async function recordRecent(
    tenantId: string,
    type: PersonalResourceType,
    resourceId: string,
): Promise<PersonalWorkspaceItem> {
    const response = await httpClient.post<ApiResponse<PersonalWorkspaceItem>>(
        `/api/tenants/${tenantId}/personal-workspace/recent/${type}/${resourceId}`,
    )
    return response.data.data
}

export const personalWorkspaceApi = {
    getOverview,
    favorite,
    unfavorite,
    recordRecent,
}
