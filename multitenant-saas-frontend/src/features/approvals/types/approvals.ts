export type ApprovalStatus = 'DRAFT' | 'ACTIVE' | 'PAUSED'
export type ApprovalRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED'
export type ApprovalStageStatus = 'WAITING' | 'PENDING' | 'APPROVED' | 'REJECTED'
export type ApprovalDecisionOutcome = 'APPROVED' | 'REJECTED'

export interface ApprovalStageInput {
    key: string
    name: string
    allowRequesterApproval: boolean
    reviewerUserIds: string[]
}

export interface ApprovalDefinitionInput {
    name: string
    description: string | null
    stages: ApprovalStageInput[]
}

export interface ApprovalStage {
    id: string
    key: string
    name: string
    position: number
    allowRequesterApproval: boolean
    reviewerUserIds: string[]
}

export interface ApprovalDefinition {
    id: string
    tenantId: string
    projectId: string
    createdByUserId: string
    name: string
    description: string | null
    status: ApprovalStatus
    definitionVersion: number
    stages: ApprovalStage[]
    createdAt: string
    updatedAt: string
}

export interface ApprovalDefinitionSummary {
    id: string
    name: string
    status: ApprovalStatus
    definitionVersion: number
    stageCount: number
    updatedAt: string
}

export interface ApprovalRequestStage {
    id: string
    key: string
    name: string
    position: number
    allowRequesterApproval: boolean
    status: ApprovalStageStatus
    reviewerUserIds: string[]
    decidedByUserId: string | null
    decisionComment: string | null
    decidedAt: string | null
}

export interface ApprovalRequest {
    id: string
    projectId: string
    definitionId: string
    definitionVersion: number
    workflowId: string
    workflowVersion: number
    workflowExecutionId: string
    workflowNodeKey: string
    taskId: string
    actorUserId: string
    status: ApprovalRequestStatus
    currentStageIndex: number
    stages: ApprovalRequestStage[]
    createdAt: string
    completedAt: string | null
}

export interface ApprovalRequestSummary {
    id: string
    definitionId: string
    workflowId: string
    taskId: string
    status: ApprovalRequestStatus
    currentStageIndex: number
    currentStageName: string | null
    createdAt: string
    completedAt: string | null
}

export interface ApprovalDecisionInput {
    outcome: ApprovalDecisionOutcome
    comment: string | null
}
