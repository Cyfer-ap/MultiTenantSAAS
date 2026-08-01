import { httpClient } from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    CreateProjectTaskInput,
    ProjectTask,
    ProjectTaskDetailsInput,
    ProjectTasksQueryParams,
    UpdateProjectTaskAssigneeInput,
    UpdateProjectTaskStatusInput,
} from '../types/projectTasks'

function tasksPath(
    tenantId: string,
    projectId: string,
): string {
    return `/api/tenants/${tenantId}/projects/${projectId}/tasks`
}

async function getTasks(
    tenantId: string,
    projectId: string,
    params: ProjectTasksQueryParams,
): Promise<PageResponse<ProjectTask>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<ProjectTask>>
    >(tasksPath(tenantId, projectId), { params })

    return response.data.data
}

async function createTask(
    tenantId: string,
    projectId: string,
    input: CreateProjectTaskInput,
): Promise<ProjectTask> {
    const response = await httpClient.post<
        ApiResponse<ProjectTask>
    >(tasksPath(tenantId, projectId), input)

    return response.data.data
}

async function updateTask(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: ProjectTaskDetailsInput,
): Promise<ProjectTask> {
    const response = await httpClient.put<
        ApiResponse<ProjectTask>
    >(`${tasksPath(tenantId, projectId)}/${taskId}`, input)

    return response.data.data
}

async function updateTaskStatus(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: UpdateProjectTaskStatusInput,
): Promise<ProjectTask> {
    const response = await httpClient.patch<
        ApiResponse<ProjectTask>
    >(
        `${tasksPath(tenantId, projectId)}/${taskId}/status`,
        input,
    )

    return response.data.data
}

async function updateTaskAssignee(
    tenantId: string,
    projectId: string,
    taskId: string,
    input: UpdateProjectTaskAssigneeInput,
): Promise<ProjectTask> {
    const response = await httpClient.patch<
        ApiResponse<ProjectTask>
    >(
        `${tasksPath(tenantId, projectId)}/${taskId}/assignee`,
        input,
    )

    return response.data.data
}

async function cancelTask(
    tenantId: string,
    projectId: string,
    taskId: string,
): Promise<ProjectTask> {
    const response = await httpClient.delete<
        ApiResponse<ProjectTask>
    >(`${tasksPath(tenantId, projectId)}/${taskId}`)

    return response.data.data
}

export const projectTasksApi = {
    getTasks,
    createTask,
    updateTask,
    updateTaskStatus,
    updateTaskAssignee,
    cancelTask,
}
