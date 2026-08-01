export const auditActions = [
    'TENANT_ONBOARDED',
    'TENANT_UPDATED',
    'TENANT_STATUS_UPDATED',
    'TENANT_DEACTIVATED',
    'USER_CREATED',
    'USER_UPDATED',
    'USER_ROLE_UPDATED',
    'USER_STATUS_UPDATED',
    'USER_DEACTIVATED',
    'USER_LOGIN_UNLOCKED',
    'LOGIN_SUCCESS',
    'LOGIN_FAILED',
    'LOGOUT',
    'LOGOUT_ALL',
    'TOKEN_REFRESH',
    'PASSWORD_CHANGED',
    'PASSWORD_RESET_REQUESTED',
    'PASSWORD_RESET_COMPLETED',
    'PROJECT_CREATED',
    'PROJECT_UPDATED',
    'PROJECT_STATUS_UPDATED',
    'PROJECT_ARCHIVED',
    'PROJECT_MEMBER_ADDED',
    'PROJECT_MEMBER_ROLE_UPDATED',
    'PROJECT_MEMBER_REMOVED',
    'TASK_CREATED',
    'TASK_UPDATED',
    'TASK_STATUS_UPDATED',
    'TASK_ASSIGNEE_UPDATED',
    'TASK_CANCELLED',
] as const

export type AuditAction =
    (typeof auditActions)[number]

export type AuditActorType =
    | 'TENANT_USER'
    | 'SYSTEM_ADMIN'
    | 'SYSTEM'

export type AuditLogSortField =
    | 'createdAt'
    | 'action'
    | 'success'

export type SortDirection = 'asc' | 'desc'

export interface TenantAuditLog {
    id: string
    tenantId: string
    actorType: AuditActorType
    actorUserId: string | null
    actorUserEmail: string | null
    actorSystemAdminId: string | null
    actorSystemAdminEmail: string | null
    targetUserId: string | null
    targetUserEmail: string | null
    action: AuditAction
    success: boolean
    message: string
    createdAt: string
}

export interface TenantAuditLogsQueryParams {
    page: number
    size: number
    sortBy: AuditLogSortField
    sortDir: SortDirection
    action?: AuditAction
    success?: boolean
}
