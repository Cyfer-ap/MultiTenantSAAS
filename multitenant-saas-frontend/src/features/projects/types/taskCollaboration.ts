export type TaskActivityType =
    | 'TASK_CREATED'
    | 'TASK_UPDATED'
    | 'STATUS_CHANGED'
    | 'ASSIGNEE_CHANGED'
    | 'TASK_CANCELLED'
    | 'COMMENT_ADDED'
    | 'COMMENT_EDITED'
    | 'COMMENT_DELETED'
    | 'ATTACHMENT_ADDED'
    | 'ATTACHMENT_DELETED'

export interface TaskCommentMention {
    userId: string
    fullName: string
    email: string
}

export interface TaskComment {
    id: string
    taskId: string
    authorUserId: string
    authorName: string
    authorEmail: string
    body: string | null
    deleted: boolean
    editedAt: string | null
    deletedAt: string | null
    createdAt: string
    updatedAt: string
    mentions: TaskCommentMention[]
}

export interface TaskActivity {
    id: string
    taskId: string
    type: TaskActivityType
    actorUserId: string
    actorName: string
    actorEmail: string
    summary: string
    createdAt: string
}

export type TaskAttachmentStatus = 'PENDING' | 'AVAILABLE' | 'DELETED'

export interface TaskAttachment {
    id: string
    taskId: string
    commentId: string | null
    uploaderUserId: string
    uploaderName: string
    filename: string
    contentType: string
    sizeBytes: number
    status: TaskAttachmentStatus
    createdAt: string
    completedAt: string | null
    deletedAt: string | null
}

export interface TaskAttachmentUploadInput {
    file: File
    commentId?: string | null
}

export interface TaskAttachmentUploadResponse {
    attachment: TaskAttachment
    uploadUrl: string
    expiresAt: string
    requiredHeaders: Record<string, string>
}

export interface TaskAttachmentDownloadResponse {
    downloadUrl: string
    expiresAt: string
}

export interface TaskCollaborationPageParams {
    page: number
    size: number
    sortDir: 'asc' | 'desc'
}

export interface TaskCommentInput {
    body: string
    mentionedUserIds: string[]
}
