import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    TaskActivity,
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
    getActivity,
}
