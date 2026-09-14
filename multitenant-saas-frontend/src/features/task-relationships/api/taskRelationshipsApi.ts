import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type { TaskLabel, TaskLabelInput, TaskRelationships } from '../types/taskRelationships'

function projectPath(tenantId: string, projectId: string): string {
    return `/api/tenants/${tenantId}/projects/${projectId}`
}

function relationshipsPath(tenantId: string, projectId: string, taskId: string): string {
    return `${projectPath(tenantId, projectId)}/tasks/${taskId}/relationships`
}

async function getRelationships(
    tenantId: string,
    projectId: string,
    taskId: string,
): Promise<TaskRelationships> {
    const response = await httpClient.get<ApiResponse<TaskRelationships>>(
        relationshipsPath(tenantId, projectId, taskId),
    )
    return response.data.data
}

async function updateParent(
    tenantId: string,
    projectId: string,
    taskId: string,
    parentTaskId: string | null,
): Promise<TaskRelationships> {
    const response = await httpClient.put<ApiResponse<TaskRelationships>>(
        `${projectPath(tenantId, projectId)}/tasks/${taskId}/parent`,
        { parentTaskId },
    )
    return response.data.data
}

async function addDependency(
    tenantId: string,
    projectId: string,
    taskId: string,
    blockingTaskId: string,
): Promise<TaskRelationships> {
    const response = await httpClient.post<ApiResponse<TaskRelationships>>(
        `${projectPath(tenantId, projectId)}/tasks/${taskId}/dependencies`,
        { blockingTaskId },
    )
    return response.data.data
}

async function removeDependency(
    tenantId: string,
    projectId: string,
    taskId: string,
    blockingTaskId: string,
): Promise<TaskRelationships> {
    const response = await httpClient.delete<ApiResponse<TaskRelationships>>(
        `${projectPath(tenantId, projectId)}/tasks/${taskId}/dependencies/${blockingTaskId}`,
    )
    return response.data.data
}

async function getLabels(tenantId: string, projectId: string): Promise<TaskLabel[]> {
    const response = await httpClient.get<ApiResponse<TaskLabel[]>>(
        `${projectPath(tenantId, projectId)}/task-labels`,
    )
    return response.data.data
}

async function createLabel(
    tenantId: string,
    projectId: string,
    input: TaskLabelInput,
): Promise<TaskLabel> {
    const response = await httpClient.post<ApiResponse<TaskLabel>>(
        `${projectPath(tenantId, projectId)}/task-labels`,
        input,
    )
    return response.data.data
}

async function updateLabel(
    tenantId: string,
    projectId: string,
    labelId: string,
    input: TaskLabelInput,
): Promise<TaskLabel> {
    const response = await httpClient.put<ApiResponse<TaskLabel>>(
        `${projectPath(tenantId, projectId)}/task-labels/${labelId}`,
        input,
    )
    return response.data.data
}

async function deleteLabel(tenantId: string, projectId: string, labelId: string): Promise<void> {
    await httpClient.delete<ApiResponse<null>>(
        `${projectPath(tenantId, projectId)}/task-labels/${labelId}`,
    )
}

async function assignLabel(
    tenantId: string,
    projectId: string,
    taskId: string,
    labelId: string,
): Promise<TaskRelationships> {
    const response = await httpClient.put<ApiResponse<TaskRelationships>>(
        `${projectPath(tenantId, projectId)}/tasks/${taskId}/labels/${labelId}`,
    )
    return response.data.data
}

async function unassignLabel(
    tenantId: string,
    projectId: string,
    taskId: string,
    labelId: string,
): Promise<TaskRelationships> {
    const response = await httpClient.delete<ApiResponse<TaskRelationships>>(
        `${projectPath(tenantId, projectId)}/tasks/${taskId}/labels/${labelId}`,
    )
    return response.data.data
}

export const taskRelationshipsApi = {
    getRelationships,
    updateParent,
    addDependency,
    removeDependency,
    getLabels,
    createLabel,
    updateLabel,
    deleteLabel,
    assignLabel,
    unassignLabel,
}
