import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    RecurringWorkInput,
    RecurringWorkOccurrence,
    RecurringWorkRule,
} from '../types/recurringWork'

const basePath = (tenantId: string, projectId: string) =>
    `/api/tenants/${tenantId}/projects/${projectId}/recurring-work`

async function list(tenantId: string, projectId: string): Promise<PageResponse<RecurringWorkRule>> {
    const response = await httpClient.get<ApiResponse<PageResponse<RecurringWorkRule>>>(
        basePath(tenantId, projectId),
        { params: { page: 0, size: 50, sort: 'createdAt,desc' } },
    )
    return response.data.data
}

async function create(
    tenantId: string,
    projectId: string,
    input: RecurringWorkInput,
): Promise<RecurringWorkRule> {
    const response = await httpClient.post<ApiResponse<RecurringWorkRule>>(
        basePath(tenantId, projectId),
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    projectId: string,
    definitionId: string,
    input: RecurringWorkInput,
): Promise<RecurringWorkRule> {
    const response = await httpClient.put<ApiResponse<RecurringWorkRule>>(
        `${basePath(tenantId, projectId)}/${definitionId}`,
        input,
    )
    return response.data.data
}

async function pause(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<RecurringWorkRule> {
    const response = await httpClient.post<ApiResponse<RecurringWorkRule>>(
        `${basePath(tenantId, projectId)}/${definitionId}/pause`,
    )
    return response.data.data
}

async function resume(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<RecurringWorkRule> {
    const response = await httpClient.post<ApiResponse<RecurringWorkRule>>(
        `${basePath(tenantId, projectId)}/${definitionId}/resume`,
    )
    return response.data.data
}

async function occurrences(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<PageResponse<RecurringWorkOccurrence>> {
    const response = await httpClient.get<ApiResponse<PageResponse<RecurringWorkOccurrence>>>(
        `${basePath(tenantId, projectId)}/${definitionId}/occurrences`,
        { params: { page: 0, size: 50, sort: 'scheduledFor,desc' } },
    )
    return response.data.data
}

export const recurringWorkApi = { list, create, update, pause, resume, occurrences }
