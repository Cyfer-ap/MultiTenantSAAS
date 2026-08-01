import { httpClient } from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    ProjectDetailsInput,
    TenantProject,
    TenantProjectsQueryParams,
    UpdateProjectStatusInput,
} from '../types/projects'

async function getProjects(
    tenantId: string,
    params: TenantProjectsQueryParams,
): Promise<PageResponse<TenantProject>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<TenantProject>>
    >(`/api/tenants/${tenantId}/projects`, {
        params,
    })

    return response.data.data
}

async function createProject(
    tenantId: string,
    input: ProjectDetailsInput,
): Promise<TenantProject> {
    const response = await httpClient.post<
        ApiResponse<TenantProject>
    >(`/api/tenants/${tenantId}/projects`, input)

    return response.data.data
}

async function updateProject(
    tenantId: string,
    projectId: string,
    input: ProjectDetailsInput,
): Promise<TenantProject> {
    const response = await httpClient.put<
        ApiResponse<TenantProject>
    >(
        `/api/tenants/${tenantId}/projects/${projectId}`,
        input,
    )

    return response.data.data
}

async function updateProjectStatus(
    tenantId: string,
    projectId: string,
    input: UpdateProjectStatusInput,
): Promise<TenantProject> {
    const response = await httpClient.patch<
        ApiResponse<TenantProject>
    >(
        `/api/tenants/${tenantId}/projects/${projectId}/status`,
        input,
    )

    return response.data.data
}

async function archiveProject(
    tenantId: string,
    projectId: string,
): Promise<TenantProject> {
    const response = await httpClient.delete<
        ApiResponse<TenantProject>
    >(
        `/api/tenants/${tenantId}/projects/${projectId}`,
    )

    return response.data.data
}

export const projectsApi = {
    getProjects,
    createProject,
    updateProject,
    updateProjectStatus,
    archiveProject,
}
