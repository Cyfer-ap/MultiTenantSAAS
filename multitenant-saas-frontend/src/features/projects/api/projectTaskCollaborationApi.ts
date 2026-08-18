import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    TaskActivity,
    TaskAttachment,
    TaskAttachmentDownloadResponse,
    TaskAttachmentUploadInput,
    TaskAttachmentUploadResponse,
    TaskCollaborationPageParams,
    TaskComment,
    TaskCommentInput,
} from '../types/taskCollaboration'

function taskPath(tenantId: string, projectId: string, taskId: string): string {
    return `/api/tenants/${tenantId}/projects/${projectId}/tasks/${taskId}`
}

async function getComments(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
): Promise<PageResponse<TaskComment>> {
    const response = await httpClient.get<ApiResponse<PageResponse<TaskComment>>>(
        `${taskPath(tenantId, projectId, taskId)}/comments`,
        { params },
    )
    return response.data.data
}

async function createComment(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: TaskCommentInput,
): Promise<TaskComment> {
    const response = await httpClient.post<ApiResponse<TaskComment>>(
        `${taskPath(tenantId, projectId, taskId)}/comments`,
        input,
    )
    return response.data.data
}

async function updateComment(
    tenantId: string,
    projectId: string,
    taskId: string,
    commentId: string,
    input: TaskCommentInput,
): Promise<TaskComment> {
    const response = await httpClient.put<ApiResponse<TaskComment>>(
        `${taskPath(tenantId, projectId, taskId)}/comments/${commentId}`,
        input,
    )
    return response.data.data
}

async function deleteComment(
    tenantId: string,
    projectId: string,
    taskId: string,
    commentId: string,
): Promise<TaskComment> {
    const response = await httpClient.delete<ApiResponse<TaskComment>>(
        `${taskPath(tenantId, projectId, taskId)}/comments/${commentId}`,
    )
    return response.data.data
}

async function getAttachments(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
): Promise<PageResponse<TaskAttachment>> {
    const response = await httpClient.get<ApiResponse<PageResponse<TaskAttachment>>>(
        `${taskPath(tenantId, projectId, taskId)}/attachments`,
        { params },
    )
    return response.data.data
}

async function initiateAttachmentUpload(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: TaskAttachmentUploadInput,
): Promise<TaskAttachmentUploadResponse> {
    const contentType = input.file.type || 'application/octet-stream'
    const response = await httpClient.post<ApiResponse<TaskAttachmentUploadResponse>>(
        `${taskPath(tenantId, projectId, taskId)}/attachments/uploads`,
        {
            filename: input.file.name,
            contentType,
            sizeBytes: input.file.size,
            commentId: input.commentId ?? null,
        },
    )
    return response.data.data
}

async function completeAttachmentUpload(
    tenantId: string,
    projectId: string,
    taskId: string,
    attachmentId: string,
): Promise<TaskAttachment> {
    const response = await httpClient.post<ApiResponse<TaskAttachment>>(
        `${taskPath(tenantId, projectId, taskId)}/attachments/${attachmentId}/complete`,
    )
    return response.data.data
}

async function uploadTaskAttachment(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: TaskAttachmentUploadInput,
): Promise<TaskAttachment> {
    let initiated: TaskAttachmentUploadResponse | null = null
    let objectUploaded = false

    try {
        initiated = await initiateAttachmentUpload(tenantId, projectId, taskId, input)

        const uploadResponse = await fetch(initiated.uploadUrl, {
            method: 'PUT',
            headers: initiated.requiredHeaders,
            body: input.file,
        })

        if (!uploadResponse.ok) {
            throw new Error(`Attachment upload failed with status ${uploadResponse.status}.`)
        }

        objectUploaded = true
        return await completeAttachmentUpload(
            tenantId,
            projectId,
            taskId,
            initiated.attachment.id,
        )
    } catch (error) {
        if (initiated) {
            if (objectUploaded) {
                try {
                    return await completeAttachmentUpload(
                        tenantId,
                        projectId,
                        taskId,
                        initiated.attachment.id,
                    )
                } catch {
                    // Completion is retried once because the first response can be lost after
                    // the backend has already committed the AVAILABLE state.
                }
            }

            try {
                await deleteAttachment(tenantId, projectId, taskId, initiated.attachment.id)
            } catch {
                // The backend cleanup job handles any remaining stale PENDING/deferred object.
            }
        }

        throw error
    }
}

async function getAttachmentDownload(
    tenantId: string,
    projectId: string,
    taskId: string,
    attachmentId: string,
): Promise<TaskAttachmentDownloadResponse> {
    const response = await httpClient.get<ApiResponse<TaskAttachmentDownloadResponse>>(
        `${taskPath(tenantId, projectId, taskId)}/attachments/${attachmentId}/download`,
    )
    return response.data.data
}

async function deleteAttachment(
    tenantId: string,
    projectId: string,
    taskId: string,
    attachmentId: string,
): Promise<TaskAttachment> {
    const response = await httpClient.delete<ApiResponse<TaskAttachment>>(
        `${taskPath(tenantId, projectId, taskId)}/attachments/${attachmentId}`,
    )
    return response.data.data
}

async function getActivity(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
): Promise<PageResponse<TaskActivity>> {
    const response = await httpClient.get<ApiResponse<PageResponse<TaskActivity>>>(
        `${taskPath(tenantId, projectId, taskId)}/activity`,
        { params },
    )
    return response.data.data
}

export const projectTaskCollaborationApi = {
    getComments,
    createComment,
    updateComment,
    deleteComment,
    getAttachments,
    initiateAttachmentUpload,
    completeAttachmentUpload,
    uploadTaskAttachment,
    getAttachmentDownload,
    deleteAttachment,
    getActivity,
}
