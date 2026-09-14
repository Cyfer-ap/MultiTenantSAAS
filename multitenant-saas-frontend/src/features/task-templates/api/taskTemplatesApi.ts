import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    TaskTemplate,
    TaskTemplateInput,
    TaskTemplateInstantiation,
} from '../types/taskTemplates'

const basePath = (tenantId: string, projectId: string) =>
    `/api/tenants/${tenantId}/projects/${projectId}/task-templates`

async function list(tenantId: string, projectId: string): Promise<PageResponse<TaskTemplate>> {
    const response = await httpClient.get<ApiResponse<PageResponse<TaskTemplate>>>(
        basePath(tenantId, projectId),
        { params: { page: 0, size: 50, sort: 'name,asc' } },
    )
    return response.data.data
}

async function create(
    tenantId: string,
    projectId: string,
    input: TaskTemplateInput,
): Promise<TaskTemplate> {
    const response = await httpClient.post<ApiResponse<TaskTemplate>>(
        basePath(tenantId, projectId),
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    projectId: string,
    templateId: string,
    input: TaskTemplateInput,
): Promise<TaskTemplate> {
    const response = await httpClient.put<ApiResponse<TaskTemplate>>(
        `${basePath(tenantId, projectId)}/${templateId}`,
        input,
    )
    return response.data.data
}

async function remove(tenantId: string, projectId: string, templateId: string): Promise<void> {
    await httpClient.delete(`${basePath(tenantId, projectId)}/${templateId}`)
}

async function instantiate(
    tenantId: string,
    projectId: string,
    templateId: string,
): Promise<TaskTemplateInstantiation> {
    const response = await httpClient.post<ApiResponse<TaskTemplateInstantiation>>(
        `${basePath(tenantId, projectId)}/${templateId}/instantiate`,
    )
    return response.data.data
}

export const taskTemplatesApi = { list, create, update, remove, instantiate }
