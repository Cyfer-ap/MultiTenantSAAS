import { httpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type { FormDefinition, FormInput, FormSubmission, FormSummary } from '../types/forms'

const basePath = (tenantId: string, projectId: string) =>
    `/api/tenants/${tenantId}/projects/${projectId}/forms`

async function list(tenantId: string, projectId: string): Promise<PageResponse<FormSummary>> {
    const response = await httpClient.get<ApiResponse<PageResponse<FormSummary>>>(
        basePath(tenantId, projectId),
        { params: { page: 0, size: 100 } },
    )
    return response.data.data
}

async function get(tenantId: string, projectId: string, formId: string): Promise<FormDefinition> {
    const response = await httpClient.get<ApiResponse<FormDefinition>>(
        `${basePath(tenantId, projectId)}/${formId}`,
    )
    return response.data.data
}

async function create(
    tenantId: string,
    projectId: string,
    input: FormInput,
): Promise<FormDefinition> {
    const response = await httpClient.post<ApiResponse<FormDefinition>>(
        basePath(tenantId, projectId),
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    projectId: string,
    formId: string,
    input: FormInput,
): Promise<FormDefinition> {
    const response = await httpClient.put<ApiResponse<FormDefinition>>(
        `${basePath(tenantId, projectId)}/${formId}`,
        input,
    )
    return response.data.data
}

async function activate(
    tenantId: string,
    projectId: string,
    formId: string,
): Promise<FormDefinition> {
    const response = await httpClient.post<ApiResponse<FormDefinition>>(
        `${basePath(tenantId, projectId)}/${formId}/activate`,
    )
    return response.data.data
}

async function pause(tenantId: string, projectId: string, formId: string): Promise<FormDefinition> {
    const response = await httpClient.post<ApiResponse<FormDefinition>>(
        `${basePath(tenantId, projectId)}/${formId}/pause`,
    )
    return response.data.data
}

async function submit(
    tenantId: string,
    projectId: string,
    formId: string,
    values: Record<string, unknown>,
): Promise<FormSubmission> {
    const response = await httpClient.post<ApiResponse<FormSubmission>>(
        `${basePath(tenantId, projectId)}/${formId}/submissions`,
        { values },
    )
    return response.data.data
}

async function history(
    tenantId: string,
    projectId: string,
    formId: string,
): Promise<PageResponse<FormSubmission>> {
    const response = await httpClient.get<ApiResponse<PageResponse<FormSubmission>>>(
        `${basePath(tenantId, projectId)}/${formId}/submissions`,
        { params: { page: 0, size: 25 } },
    )
    return response.data.data
}

export const formsApi = { list, get, create, update, activate, pause, submit, history }
