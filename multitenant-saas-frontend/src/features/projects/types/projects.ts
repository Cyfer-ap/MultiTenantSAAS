export type ProjectStatus = 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'

export type ProjectSortField = 'createdAt' | 'updatedAt' | 'name' | 'status'

export type SortDirection = 'asc' | 'desc'

export interface TenantProject {
    id: string
    tenantId: string
    name: string
    description: string | null
    status: ProjectStatus
    createdByUserId: string
    createdByUserName: string
    createdByUserEmail: string
    createdAt: string
    updatedAt: string
}

export interface TenantProjectsQueryParams {
    page: number
    size: number
    sortBy: ProjectSortField
    sortDir: SortDirection
    status?: ProjectStatus
    search?: string
}

export interface ProjectDetailsInput {
    name: string
    description: string | null
}

export interface UpdateProjectStatusInput {
    status: Exclude<ProjectStatus, 'ARCHIVED'>
}

export type ProjectMemberRole = 'PROJECT_LEAD' | 'MEMBER'

export type ProjectMemberSortField = 'assignedAt' | 'updatedAt' | 'role'

export interface ProjectMember {
    membershipId: string
    projectId: string
    userId: string
    fullName: string
    email: string
    tenantRole: 'TENANT_ADMIN' | 'TENANT_MANAGER' | 'TENANT_USER'
    userStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'
    projectRole: ProjectMemberRole
    assignedByUserId: string
    assignedByUserName: string
    assignedByUserEmail: string
    assignedAt: string
    updatedAt: string
}

export interface ProjectMembersQueryParams {
    page: number
    size: number
    sortBy: ProjectMemberSortField
    sortDir: SortDirection
    role?: ProjectMemberRole
    search?: string
}

export interface AddProjectMemberInput {
    userId: string
    role: ProjectMemberRole
}

export interface UpdateProjectMemberRoleInput {
    role: ProjectMemberRole
}
