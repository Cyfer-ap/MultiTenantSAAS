import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type { WorkflowDefinition, WorkflowExecution, WorkflowInput } from '../types/workflows'

const basePath = (tenantId: string) => `/api/tenants/${tenantId}/workflows`

async function list(tenantId: string): Promise<PageResponse<WorkflowDefinition>> {
    const response = await httpClient.get<ApiResponse<PageResponse<WorkflowDefinition>>>(
        basePath(tenantId),
        { params: { page: 0, size: 100 } },
    )
    return response.data.data
}

async function get(tenantId: string, workflowId: string): Promise<WorkflowDefinition> {
    const response = await httpClient.get<ApiResponse<WorkflowDefinition>>(
        `${basePath(tenantId)}/${workflowId}`,
    )
    return response.data.data
}

async function create(tenantId: string, input: WorkflowInput): Promise<WorkflowDefinition> {
    const response = await httpClient.post<ApiResponse<WorkflowDefinition>>(
        basePath(tenantId),
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    workflowId: string,
    input: WorkflowInput,
): Promise<WorkflowDefinition> {
    const response = await httpClient.put<ApiResponse<WorkflowDefinition>>(
        `${basePath(tenantId)}/${workflowId}`,
        input,
    )
    return response.data.data
}

async function activate(tenantId: string, workflowId: string): Promise<WorkflowDefinition> {
    const response = await httpClient.post<ApiResponse<WorkflowDefinition>>(
        `${basePath(tenantId)}/${workflowId}/activate`,
    )
    return response.data.data
}

async function pause(tenantId: string, workflowId: string): Promise<WorkflowDefinition> {
    const response = await httpClient.post<ApiResponse<WorkflowDefinition>>(
        `${basePath(tenantId)}/${workflowId}/pause`,
    )
    return response.data.data
}

async function executions(
    tenantId: string,
    workflowId: string,
): Promise<PageResponse<WorkflowExecution>> {
    const response = await httpClient.get<ApiResponse<PageResponse<WorkflowExecution>>>(
        `${basePath(tenantId)}/${workflowId}/executions`,
        { params: { page: 0, size: 25 } },
    )
    return response.data.data
}

export const workflowsApi = { list, get, create, update, activate, pause, executions }
