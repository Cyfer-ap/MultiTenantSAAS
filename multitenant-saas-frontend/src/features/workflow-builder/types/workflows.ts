export type WorkflowStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED'

export type WorkflowNodeType = 'TRIGGER' | 'CONDITION' | 'ACTION'

export type WorkflowOperation =
    | 'TRIGGER_TASK_CREATED'
    | 'TRIGGER_TASK_STATUS_CHANGED'
    | 'TRIGGER_FORM_SUBMITTED'
    | 'CONDITION_TASK_PRIORITY_EQUALS'
    | 'CONDITION_TASK_STATUS_EQUALS'
    | 'ACTION_SET_TASK_PRIORITY'
    | 'ACTION_SET_TASK_STATUS'
    | 'ACTION_REQUEST_APPROVAL'

export type WorkflowEdgeBranch = 'DEFAULT' | 'TRUE' | 'FALSE' | 'APPROVED' | 'REJECTED'

export interface WorkflowNode {
    id: string
    key: string
    type: WorkflowNodeType
    operation: WorkflowOperation
    configuration: Record<string, string>
    x: number
    y: number
}

export interface WorkflowEdge {
    id: string
    sourceKey: string
    targetKey: string
    branch: WorkflowEdgeBranch
}

export interface WorkflowDefinition {
    id: string
    tenantId: string
    createdByUserId: string
    name: string
    description: string | null
    status: WorkflowStatus
    definitionVersion: number
    nodes: WorkflowNode[]
    edges: WorkflowEdge[]
    createdAt: string
    updatedAt: string
}

export interface WorkflowNodeInput {
    key: string
    type: WorkflowNodeType
    operation: WorkflowOperation
    configuration: Record<string, string>
    x: number
    y: number
}

export interface WorkflowEdgeInput {
    sourceKey: string
    targetKey: string
    branch: WorkflowEdgeBranch
}

export interface WorkflowInput {
    name: string
    description: string | null
    nodes: WorkflowNodeInput[]
    edges: WorkflowEdgeInput[]
}

export type WorkflowExecutionStatus =
    'RUNNING' | 'WAITING_APPROVAL' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'

export interface WorkflowExecution {
    id: string
    workflowId: string
    workflowVersion: number
    triggerOperation: WorkflowOperation
    sourceEntityType: string | null
    sourceEntityId: string | null
    status: WorkflowExecutionStatus
    explanation: string | null
    errorMessage: string | null
    startedAt: string
    completedAt: string | null
}
