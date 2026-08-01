import type { SortDirection } from './projects'

export type ProjectTaskStatus =
    | 'TODO'
    | 'IN_PROGRESS'
    | 'BLOCKED'
    | 'COMPLETED'
    | 'CANCELLED'

export type ProjectTaskPriority =
    | 'LOW'
    | 'MEDIUM'
    | 'HIGH'
    | 'URGENT'

export type ProjectTaskSortField =
    | 'createdAt'
    | 'updatedAt'
    | 'title'
    | 'status'
    | 'priority'
    | 'dueAt'

export interface ProjectTask {
    id: string
    tenantId: string
    projectId: string
    title: string
    description: string | null
    status: ProjectTaskStatus
    priority: ProjectTaskPriority
    assigneeUserId: string | null
    assigneeName: string | null
    assigneeEmail: string | null
    createdByUserId: string
    createdByUserName: string
    createdByUserEmail: string
    dueAt: string | null
    completedAt: string | null
    createdAt: string
    updatedAt: string
}

export interface ProjectTasksQueryParams {
    page: number
    size: number
    sortBy: ProjectTaskSortField
    sortDir: SortDirection
    status?: ProjectTaskStatus
    priority?: ProjectTaskPriority
    assigneeUserId?: string
    search?: string
}

export interface ProjectTaskDetailsInput {
    title: string
    description: string | null
    priority: ProjectTaskPriority
    dueAt: string | null
}

export interface CreateProjectTaskInput
    extends ProjectTaskDetailsInput {
    assigneeUserId: string | null
}

export interface UpdateProjectTaskStatusInput {
    status: Exclude<ProjectTaskStatus, 'CANCELLED'>
}

export interface UpdateProjectTaskAssigneeInput {
    assigneeUserId: string | null
}
