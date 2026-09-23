export type ExternalAccessCapability =
    | 'PROJECT_READ'
    | 'TASK_READ'
    | 'TASK_COMMENT_CREATE'
    | 'APPROVAL_REVIEW'

export type ExternalAccessGrantState = 'INVITED' | 'ACCEPTED' | 'EXPIRED' | 'REVOKED'

export interface ExternalAccessGrant {
    id: string
    projectId: string
    guestName: string
    guestEmail: string
    capabilities: ExternalAccessCapability[]
    state: ExternalAccessGrantState
    expiresAt: string
    acceptedAt: string | null
    revokedAt: string | null
    createdByUserId: string
    revokedByUserId: string | null
    createdAt: string
    updatedAt: string
}

export interface ExternalAccessGrantCreated {
    grant: ExternalAccessGrant
    invitationToken: string
}

export interface CreateExternalAccessGrantInput {
    guestName: string
    guestEmail: string
    expiresAt: string
    capabilities: ExternalAccessCapability[]
}

export interface GuestExchangeResponse {
    grantId: string
    projectId: string
    guestName: string
    sessionToken: string
    sessionExpiresAt: string
}

export interface GuestProject {
    id: string
    name: string
    description: string | null
    status: 'PLANNING' | 'ACTIVE' | 'ON_HOLD' | 'COMPLETED' | 'ARCHIVED'
    updatedAt: string
}

export interface GuestSession {
    grantId: string
    guestName: string
    guestEmail: string
    capabilities: ExternalAccessCapability[]
    grantExpiresAt: string
    sessionExpiresAt: string
    project: GuestProject
}

export interface GuestTask {
    id: string
    title: string
    description: string | null
    status: 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'COMPLETED' | 'CANCELLED'
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
    dueAt: string | null
    completedAt: string | null
    updatedAt: string
}

export interface GuestTasks {
    tasks: GuestTask[]
}

export interface GuestComment {
    id: string
    taskId: string
    grantId: string
    guestName: string
    guestEmail: string
    body: string
    createdAt: string
}

export interface GuestComments {
    comments: GuestComment[]
}

export interface GuestApprovalReview {
    requestId: string
    requestStageId: string
    taskId: string
    stageName: string
    createdAt: string
}

export interface GuestApprovalReviews {
    reviews: GuestApprovalReview[]
}

export type GuestApprovalDecisionOutcome = 'APPROVE' | 'REJECT'

export interface GuestApprovalDecision {
    requestId: string
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    currentStageIndex: number
    completedAt: string | null
}
