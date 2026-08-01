export type ProjectStatus =
    | 'PLANNING'
    | 'ACTIVE'
    | 'ON_HOLD'
    | 'COMPLETED'
    | 'ARCHIVED'

export type ProjectSortField =
    | 'createdAt'
    | 'updatedAt'
    | 'name'
    | 'status'

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
