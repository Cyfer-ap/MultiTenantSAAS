import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    ConvertWhiteboardNodeToTaskInput,
    ConvertWhiteboardNodeToTaskResponse,
    Whiteboard,
    WhiteboardCreateInput,
    WhiteboardSummary,
    WhiteboardUpdateInput,
} from '../types/whiteboards'

async function list(tenantId: string, projectId: string): Promise<PageResponse<WhiteboardSummary>> {
    const response = await httpClient.get<ApiResponse<PageResponse<WhiteboardSummary>>>(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards`,
        { params: { page: 0, size: 100 } },
    )
    return response.data.data
}

async function get(tenantId: string, projectId: string, boardId: string): Promise<Whiteboard> {
    const response = await httpClient.get<ApiResponse<Whiteboard>>(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards/${boardId}`,
    )
    return response.data.data
}

async function create(
    tenantId: string,
    projectId: string,
    input: WhiteboardCreateInput,
): Promise<Whiteboard> {
    const response = await httpClient.post<ApiResponse<Whiteboard>>(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards`,
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    projectId: string,
    boardId: string,
    input: WhiteboardUpdateInput,
): Promise<Whiteboard> {
    const response = await httpClient.put<ApiResponse<Whiteboard>>(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards/${boardId}`,
        input,
    )
    return response.data.data
}

async function remove(
    tenantId: string,
    projectId: string,
    boardId: string,
    expectedVersion: number,
): Promise<void> {
    await httpClient.delete(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards/${boardId}`,
        {
            params: { expectedVersion },
        },
    )
}

async function convertNodeToTask(
    tenantId: string,
    projectId: string,
    boardId: string,
    nodeKey: string,
    input: ConvertWhiteboardNodeToTaskInput,
): Promise<ConvertWhiteboardNodeToTaskResponse> {
    const response = await httpClient.post<ApiResponse<ConvertWhiteboardNodeToTaskResponse>>(
        `/api/tenants/${tenantId}/projects/${projectId}/whiteboards/${boardId}/nodes/${nodeKey}/convert-to-task`,
        input,
    )
    return response.data.data
}

export const whiteboardsApi = {
    list,
    get,
    create,
    update,
    remove,
    convertNodeToTask,
}
