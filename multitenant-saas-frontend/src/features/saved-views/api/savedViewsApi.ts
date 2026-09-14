import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    CreateSavedViewInput,
    SavedView,
    SavedViewTarget,
    UpdateSavedViewInput,
} from '../types/savedViews'

async function list(
    tenantId: string,
    target: SavedViewTarget,
    contextId: string | null,
): Promise<SavedView[]> {
    const response = await httpClient.get<ApiResponse<SavedView[]>>(
        `/api/tenants/${tenantId}/saved-views`,
        {
            params: {
                target,
                ...(contextId ? { contextId } : {}),
            },
        },
    )
    return response.data.data
}

async function create(tenantId: string, input: CreateSavedViewInput): Promise<SavedView> {
    const response = await httpClient.post<ApiResponse<SavedView>>(
        `/api/tenants/${tenantId}/saved-views`,
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    viewId: string,
    input: UpdateSavedViewInput,
): Promise<SavedView> {
    const response = await httpClient.put<ApiResponse<SavedView>>(
        `/api/tenants/${tenantId}/saved-views/${viewId}`,
        input,
    )
    return response.data.data
}

async function remove(tenantId: string, viewId: string): Promise<void> {
    await httpClient.delete(`/api/tenants/${tenantId}/saved-views/${viewId}`)
}

export const savedViewsApi = { list, create, update, remove }
