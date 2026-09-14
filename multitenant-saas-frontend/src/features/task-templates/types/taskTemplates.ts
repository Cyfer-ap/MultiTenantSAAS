import type { ProjectTaskPriority } from '../../projects/types/projectTasks'

export interface TaskTemplate {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    assigneeUserId: string | null
    name: string
    taskTitle: string
    taskDescription: string | null
    priority: ProjectTaskPriority
    dueOffsetMinutes: number | null
    createdAt: string
    updatedAt: string
}

export interface TaskTemplateInput {
    name: string
    taskTitle: string
    taskDescription: string | null
    priority: ProjectTaskPriority
    assigneeUserId: string | null
    dueOffsetMinutes: number | null
}

export interface TaskTemplateInstantiation {
    templateId: string
    taskId: string
    createdAt: string
}
