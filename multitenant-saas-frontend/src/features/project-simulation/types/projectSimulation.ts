export interface ProjectSimulationBaselineTask {
    taskId: string
    title: string
    status: string
    assigneeUserId: string | null
    assigneeName: string | null
    dueAt: string | null
}

export interface ProjectSimulationBaselineDependency {
    blockingTaskId: string
    dependentTaskId: string
}

export interface ProjectSimulationBaseline {
    projectId: string
    generatedAt: string
    tasks: ProjectSimulationBaselineTask[]
    dependencies: ProjectSimulationBaselineDependency[]
}

export interface ProjectSimulationTaskOverride {
    taskId: string
    dueAt: string | null
    clearDueAt: boolean
    assigneeUserId: string | null
    clearAssignee: boolean
}

export type ProjectSimulationDependencyChangeType = 'ADD' | 'REMOVE'

export interface ProjectSimulationDependencyChange {
    type: ProjectSimulationDependencyChangeType
    blockingTaskId: string
    dependentTaskId: string
}

export interface ProjectSimulationRequest {
    taskOverrides: ProjectSimulationTaskOverride[]
    dependencyChanges: ProjectSimulationDependencyChange[]
}

export interface ProjectSimulationSummary {
    tasksEvaluated: number
    directTaskChanges: number
    dependencyChanges: number
    downstreamAffectedTasks: number
    baselineDependencyConflicts: number
    simulatedDependencyConflicts: number
    newDependencyConflicts: number
    resolvedDependencyConflicts: number
    reassignedTasks: number
}

export interface ProjectSimulationTaskImpact {
    taskId: string
    title: string
    directChange: boolean
    dependencyChanged: boolean
    downstreamAffected: boolean
    currentDueAt: string | null
    simulatedDueAt: string | null
    dueDateShiftHours: number | null
    currentAssigneeUserId: string | null
    currentAssigneeName: string | null
    simulatedAssigneeUserId: string | null
    simulatedAssigneeName: string | null
    hasSimulatedDependencyConflict: boolean
}

export interface ProjectSimulationDependencyConflict {
    blockingTaskId: string
    blockingTaskTitle: string
    dependentTaskId: string
    dependentTaskTitle: string
    changeType: 'NEW' | 'RESOLVED' | 'UNCHANGED'
    baselineBlockingDueAt: string | null
    baselineDependentDueAt: string | null
    simulatedBlockingDueAt: string | null
    simulatedDependentDueAt: string | null
}

export interface ProjectSimulationWorkloadImpact {
    userId: string | null
    displayName: string
    baselineOpenTasks: number
    simulatedOpenTasks: number
    delta: number
}

export interface ProjectSimulationResponse {
    projectId: string
    generatedAt: string
    summary: ProjectSimulationSummary
    taskImpacts: ProjectSimulationTaskImpact[]
    dependencyConflicts: ProjectSimulationDependencyConflict[]
    workloadImpacts: ProjectSimulationWorkloadImpact[]
    notes: string[]
}
