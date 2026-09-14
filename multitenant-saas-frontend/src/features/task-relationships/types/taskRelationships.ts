import type { ProjectTaskPriority, ProjectTaskStatus } from '../../projects/types/projectTasks'

export interface TaskReference {
    id: string
    title: string
    status: ProjectTaskStatus
    priority: ProjectTaskPriority
}

export interface TaskLabel {
    id: string
    name: string
    color: string | null
}

export interface TaskRelationships {
    parent: TaskReference | null
    children: TaskReference[]
    blockers: TaskReference[]
    dependents: TaskReference[]
    labels: TaskLabel[]
    childrenTruncated: boolean
    blockersTruncated: boolean
    dependentsTruncated: boolean
}

export interface TaskLabelInput {
    name: string
    color: string | null
}
