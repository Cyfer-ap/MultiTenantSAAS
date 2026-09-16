import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    ApprovalDecisionInput,
    ApprovalDefinition,
    ApprovalDefinitionInput,
    ApprovalDefinitionSummary,
    ApprovalRequest,
    ApprovalRequestSummary,
} from '../types/approvals'

const basePath = (tenantId: string, projectId: string) =>
    `/api/tenants/${tenantId}/projects/${projectId}/approvals`

const definitionsPath = (tenantId: string, projectId: string) =>
    `${basePath(tenantId, projectId)}/definitions`

async function listDefinitions(
    tenantId: string,
    projectId: string,
): Promise<PageResponse<ApprovalDefinitionSummary>> {
    const response = await httpClient.get<ApiResponse<PageResponse<ApprovalDefinitionSummary>>>(
        definitionsPath(tenantId, projectId),
        { params: { page: 0, size: 100 } },
    )
    return response.data.data
}

async function getDefinition(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<ApprovalDefinition> {
    const response = await httpClient.get<ApiResponse<ApprovalDefinition>>(
        `${definitionsPath(tenantId, projectId)}/${definitionId}`,
    )
    return response.data.data
}

async function createDefinition(
    tenantId: string,
    projectId: string,
    input: ApprovalDefinitionInput,
): Promise<ApprovalDefinition> {
    const response = await httpClient.post<ApiResponse<ApprovalDefinition>>(
        definitionsPath(tenantId, projectId),
        input,
    )
    return response.data.data
}

async function updateDefinition(
    tenantId: string,
    projectId: string,
    definitionId: string,
    input: ApprovalDefinitionInput,
): Promise<ApprovalDefinition> {
    const response = await httpClient.put<ApiResponse<ApprovalDefinition>>(
        `${definitionsPath(tenantId, projectId)}/${definitionId}`,
        input,
    )
    return response.data.data
}

async function activateDefinition(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<ApprovalDefinition> {
    const response = await httpClient.post<ApiResponse<ApprovalDefinition>>(
        `${definitionsPath(tenantId, projectId)}/${definitionId}/activate`,
    )
    return response.data.data
}

async function pauseDefinition(
    tenantId: string,
    projectId: string,
    definitionId: string,
): Promise<ApprovalDefinition> {
    const response = await httpClient.post<ApiResponse<ApprovalDefinition>>(
        `${definitionsPath(tenantId, projectId)}/${definitionId}/pause`,
    )
    return response.data.data
}

async function inbox(
    tenantId: string,
    projectId: string,
): Promise<PageResponse<ApprovalRequestSummary>> {
    const response = await httpClient.get<ApiResponse<PageResponse<ApprovalRequestSummary>>>(
        `${basePath(tenantId, projectId)}/inbox`,
        { params: { page: 0, size: 25 } },
    )
    return response.data.data
}

async function history(
    tenantId: string,
    projectId: string,
): Promise<PageResponse<ApprovalRequestSummary>> {
    const response = await httpClient.get<ApiResponse<PageResponse<ApprovalRequestSummary>>>(
        `${basePath(tenantId, projectId)}/requests`,
        { params: { page: 0, size: 25 } },
    )
    return response.data.data
}

async function getRequest(
    tenantId: string,
    projectId: string,
    requestId: string,
): Promise<ApprovalRequest> {
    const response = await httpClient.get<ApiResponse<ApprovalRequest>>(
        `${basePath(tenantId, projectId)}/requests/${requestId}`,
    )
    return response.data.data
}

async function decide(
    tenantId: string,
    projectId: string,
    requestId: string,
    input: ApprovalDecisionInput,
): Promise<ApprovalRequest> {
    const response = await httpClient.post<ApiResponse<ApprovalRequest>>(
        `${basePath(tenantId, projectId)}/requests/${requestId}/decision`,
        {
            outcome: input.outcome === 'APPROVED' ? 'APPROVE' : 'REJECT',
            comment: input.comment,
        },
    )
    return response.data.data
}

export const approvalsApi = {
    listDefinitions,
    getDefinition,
    createDefinition,
    updateDefinition,
    activateDefinition,
    pauseDefinition,
    inbox,
    history,
    getRequest,
    decide,
}
