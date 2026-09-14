import type { ProjectStatus } from '../../projects/types/projects'
import type { ProjectTaskPriority } from '../../projects/types/projectTasks'

export interface ProjectTemplateTaskSnapshot {
    id: string
    position: number
    title: string
    description: string | null
    priority: ProjectTaskPriority
    dueOffsetMinutes: number | null
}

export interface ProjectTemplate {
    id: string
    tenantId: string
    createdByUserId: string
    name: string
    projectNameSeed: string
    projectDescription: string | null
    initialStatus: ProjectStatus
    tasks: ProjectTemplateTaskSnapshot[]
    createdAt: string
    updatedAt: string
}

export interface ProjectTemplateTaskInput {
    title: string
    description: string | null
    priority: ProjectTaskPriority
    dueOffsetMinutes: number | null
}

export interface ProjectTemplateInput {
    name: string
    projectNameSeed: string
    projectDescription: string | null
    initialStatus: Exclude<ProjectStatus, 'ARCHIVED'>
    tasks: ProjectTemplateTaskInput[]
}

export interface ProjectTemplateInstantiation {
    templateId: string
    projectId: string
    tasksCreated: number
    createdAt: string
}
