import { httpClient } from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    AddProjectMemberInput,
    ProjectMember,
    ProjectMembersQueryParams,
    UpdateProjectMemberRoleInput,
} from '../types/projects'

function membersPath(
    tenantId: string,
    projectId: string,
): string {
    return `/api/tenants/${tenantId}/projects/${projectId}/members`
}

async function getMembers(
    tenantId: string,
    projectId: string,
    params: ProjectMembersQueryParams,
): Promise<PageResponse<ProjectMember>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<ProjectMember>>
    >(membersPath(tenantId, projectId), { params })

    return response.data.data
}

async function addMember(
    tenantId: string,
    projectId: string,
    input: AddProjectMemberInput,
): Promise<ProjectMember> {
    const response = await httpClient.post<
        ApiResponse<ProjectMember>
    >(membersPath(tenantId, projectId), input)

    return response.data.data
}

async function updateMemberRole(
    tenantId: string,
    projectId: string,
    userId: string,
    input: UpdateProjectMemberRoleInput,
): Promise<ProjectMember> {
    const response = await httpClient.patch<
        ApiResponse<ProjectMember>
    >(
        `${membersPath(tenantId, projectId)}/${userId}/role`,
        input,
    )

    return response.data.data
}

async function removeMember(
    tenantId: string,
    projectId: string,
    userId: string,
): Promise<ProjectMember> {
    const response = await httpClient.delete<
        ApiResponse<ProjectMember>
    >(`${membersPath(tenantId, projectId)}/${userId}`)

    return response.data.data
}

export const projectMembersApi = {
    getMembers,
    addMember,
    updateMemberRole,
    removeMember,
}
