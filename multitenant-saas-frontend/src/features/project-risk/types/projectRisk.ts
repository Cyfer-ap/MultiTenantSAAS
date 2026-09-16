export type ProjectRiskSeverity = 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'

export type ProjectRiskSignalType =
    | 'OVERDUE_TASK'
    | 'BLOCKED_TASK'
    | 'STALE_TASK'
    | 'UNASSIGNED_CRITICAL_TASK'
    | 'DEPENDENCY_BOTTLENECK'

export interface ProjectRiskSummary {
    totalTasks: number
    openTasks: number
    overdueTasks: number
    blockedTasks: number
    staleTasks: number
    unassignedCriticalTasks: number
    dependencyBottlenecks: number
}

export interface ProjectRiskSignal {
    type: ProjectRiskSignalType
    severity: ProjectRiskSeverity
    taskId: string
    taskTitle: string
    explanation: string
    affectedTaskCount: number
    relatedTaskIds: string[]
    dueAt: string | null
    updatedAt: string | null
}

export interface ProjectRiskResponse {
    projectId: string
    generatedAt: string
    riskLevel: ProjectRiskSeverity
    staleAfterDays: number
    summary: ProjectRiskSummary
    signals: ProjectRiskSignal[]
    limitations: string[]
}
