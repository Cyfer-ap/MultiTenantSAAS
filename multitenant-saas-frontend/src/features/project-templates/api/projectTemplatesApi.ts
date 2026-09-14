import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    ProjectTemplate,
    ProjectTemplateInput,
    ProjectTemplateInstantiation,
} from '../types/projectTemplates'

const basePath = (tenantId: string) => `/api/tenants/${tenantId}/project-templates`

async function list(tenantId: string): Promise<PageResponse<ProjectTemplate>> {
    const response = await httpClient.get<ApiResponse<PageResponse<ProjectTemplate>>>(
        basePath(tenantId),
        { params: { page: 0, size: 50, sort: 'name,asc' } },
    )
    return response.data.data
}

async function create(tenantId: string, input: ProjectTemplateInput): Promise<ProjectTemplate> {
    const response = await httpClient.post<ApiResponse<ProjectTemplate>>(basePath(tenantId), input)
    return response.data.data
}

async function update(
    tenantId: string,
    templateId: string,
    input: ProjectTemplateInput,
): Promise<ProjectTemplate> {
    const response = await httpClient.put<ApiResponse<ProjectTemplate>>(
        `${basePath(tenantId)}/${templateId}`,
        input,
    )
    return response.data.data
}

async function remove(tenantId: string, templateId: string): Promise<void> {
    await httpClient.delete(`${basePath(tenantId)}/${templateId}`)
}

async function instantiate(
    tenantId: string,
    templateId: string,
    projectName: string | null,
): Promise<ProjectTemplateInstantiation> {
    const response = await httpClient.post<ApiResponse<ProjectTemplateInstantiation>>(
        `${basePath(tenantId)}/${templateId}/instantiate`,
        projectName?.trim() ? { projectName: projectName.trim() } : {},
    )
    return response.data.data
}

export const projectTemplatesApi = { list, create, update, remove, instantiate }
