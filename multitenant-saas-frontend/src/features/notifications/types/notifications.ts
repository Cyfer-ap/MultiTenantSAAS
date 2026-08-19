export type NotificationType =
    | 'TASK_ASSIGNED'
    | 'TASK_STATUS_CHANGED'
    | 'TASK_COMMENT_ADDED'
    | 'TASK_COMMENT_REPLIED'
    | 'TASK_COMMENT_MENTIONED'
    | 'PROJECT_MEMBERSHIP_CHANGED'
    | 'WORKSPACE_INVITATION'
    | 'SECURITY_ALERT'

export interface Notification {
    id: string
    tenantId: string
    recipientUserId: string
    type: NotificationType
    title: string
    body: string
    targetUrl: string | null
    readAt: string | null
    createdAt: string
}

export interface NotificationUnreadCount {
    unreadCount: number
}

export interface NotificationMarkAllReadResult {
    markedReadCount: number
}
