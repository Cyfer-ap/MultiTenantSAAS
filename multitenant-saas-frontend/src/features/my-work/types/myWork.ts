export type MyWorkAttention = 'OVERDUE' | 'BLOCKED' | 'DUE_SOON' | 'IN_PROGRESS' | 'ASSIGNED'

export interface MyWorkItem {
    taskId: string
    projectId: string
    title: string
    projectName: string
    status: 'TODO' | 'IN_PROGRESS' | 'BLOCKED'
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
    dueAt: string | null
    updatedAt: string
    attention: MyWorkAttention
    targetUrl: string
}

export interface MyWorkSummary {
    totalOpen: number
    overdue: number
    dueSoon: number
    blocked: number
    inProgress: number
}

export interface MyWorkOverview {
    generatedAt: string
    dueSoonHours: number
    summary: MyWorkSummary
    items: MyWorkItem[]
}
