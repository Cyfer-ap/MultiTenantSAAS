import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    CreateOrganizationalUnitInput,
    CreateOrganizationAssignmentInput,
    MoveOrganizationalUnitInput,
    OrganizationAssignment,
    OrganizationAssignmentUserOption,
    OrganizationalUnit,
    OrganizationalUnitTree,
    UpdateOrganizationalUnitInput,
    UpdateOrganizationalUnitStatusInput,
} from '../types/organization'

async function getTree(tenantId: string): Promise<OrganizationalUnitTree[]> {
    const response = await httpClient.get<ApiResponse<OrganizationalUnitTree[]>>(
        `/api/tenants/${tenantId}/organization/units/tree`,
    )

    return response.data.data
}

async function createUnit(
    tenantId: string,
    input: CreateOrganizationalUnitInput,
): Promise<OrganizationalUnit> {
    const response = await httpClient.post<ApiResponse<OrganizationalUnit>>(
        `/api/tenants/${tenantId}/organization/units`,
        input,
    )

    return response.data.data
}

async function updateUnit(
    tenantId: string,
    unitId: string,
    input: UpdateOrganizationalUnitInput,
): Promise<OrganizationalUnit> {
    const response = await httpClient.put<ApiResponse<OrganizationalUnit>>(
        `/api/tenants/${tenantId}/organization/units/${unitId}`,
        input,
    )

    return response.data.data
}

async function moveUnit(
    tenantId: string,
    unitId: string,
    input: MoveOrganizationalUnitInput,
): Promise<OrganizationalUnit> {
    const response = await httpClient.patch<ApiResponse<OrganizationalUnit>>(
        `/api/tenants/${tenantId}/organization/units/${unitId}/move`,
        input,
    )

    return response.data.data
}

async function updateUnitStatus(
    tenantId: string,
    unitId: string,
    input: UpdateOrganizationalUnitStatusInput,
): Promise<OrganizationalUnit> {
    const response = await httpClient.patch<ApiResponse<OrganizationalUnit>>(
        `/api/tenants/${tenantId}/organization/units/${unitId}/status`,
        input,
    )

    return response.data.data
}

async function getUnitAssignments(
    tenantId: string,
    unitId: string,
): Promise<OrganizationAssignment[]> {
    const response = await httpClient.get<ApiResponse<OrganizationAssignment[]>>(
        `/api/tenants/${tenantId}/organization/assignments/units/${unitId}`,
    )

    return response.data.data
}

async function getAssignmentUserOptions(
    tenantId: string,
    unitId: string,
): Promise<OrganizationAssignmentUserOption[]> {
    const response = await httpClient.get<ApiResponse<OrganizationAssignmentUserOption[]>>(
        `/api/tenants/${tenantId}/organization/assignments/units/${unitId}/user-options`,
    )

    return response.data.data
}

async function createAssignment(
    tenantId: string,
    input: CreateOrganizationAssignmentInput,
): Promise<OrganizationAssignment> {
    const response = await httpClient.post<ApiResponse<OrganizationAssignment>>(
        `/api/tenants/${tenantId}/organization/assignments`,
        input,
    )

    return response.data.data
}

async function deactivateAssignment(
    tenantId: string,
    assignmentId: string,
): Promise<OrganizationAssignment> {
    const response = await httpClient.patch<ApiResponse<OrganizationAssignment>>(
        `/api/tenants/${tenantId}/organization/assignments/${assignmentId}/deactivate`,
    )

    return response.data.data
}

export const organizationApi = {
    getTree,
    createUnit,
    updateUnit,
    moveUnit,
    updateUnitStatus,
    getUnitAssignments,
    getAssignmentUserOptions,
    createAssignment,
    deactivateAssignment,
}
