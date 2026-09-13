import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    AuthorizationAssignmentReferenceData,
    AuthorizationDelegation,
    AuthorizationDelegationReferenceData,
    AuthorizationExplainAccessResult,
    AuthorizationPermission,
    AuthorizationRole,
    AuthorizationUserRoleAssignment,
    CreateAuthorizationDelegationInput,
    CreateAuthorizationRoleInput,
    CreateAuthorizationUserRoleAssignmentInput,
    CurrentAuthorizationContext,
    ExplainAuthorizationAccessInput,
    ReplaceAuthorizationRolePermissionsInput,
} from '../types/authorization'

function authorizationBasePath(tenantId: string): string {
    return `/api/tenants/${encodeURIComponent(tenantId)}/authorization`
}

function delegationBasePath(tenantId: string): string {
    return `${authorizationBasePath(tenantId)}/delegations`
}

async function getCurrentAuthorizationContext(
    tenantId: string,
): Promise<CurrentAuthorizationContext> {
    const response = await httpClient.get<ApiResponse<CurrentAuthorizationContext>>(
        `${authorizationBasePath(tenantId)}/me`,
    )

    return response.data.data
}

async function getPermissions(tenantId: string): Promise<AuthorizationPermission[]> {
    const response = await httpClient.get<ApiResponse<AuthorizationPermission[]>>(
        `${authorizationBasePath(tenantId)}/permissions`,
    )

    return response.data.data
}

async function getRoles(tenantId: string, activeOnly = false): Promise<AuthorizationRole[]> {
    const response = await httpClient.get<ApiResponse<AuthorizationRole[]>>(
        `${authorizationBasePath(tenantId)}/roles`,
        {
            params: { activeOnly },
        },
    )

    return response.data.data
}

async function initializeDefaultRoles(tenantId: string): Promise<AuthorizationRole[]> {
    const response = await httpClient.post<ApiResponse<AuthorizationRole[]>>(
        `${authorizationBasePath(tenantId)}/roles/defaults/initialize`,
    )

    return response.data.data
}

async function createRole(
    tenantId: string,
    input: CreateAuthorizationRoleInput,
): Promise<AuthorizationRole> {
    const response = await httpClient.post<ApiResponse<AuthorizationRole>>(
        `${authorizationBasePath(tenantId)}/roles`,
        input,
    )

    return response.data.data
}

async function replaceRolePermissions(
    tenantId: string,
    roleId: string,
    input: ReplaceAuthorizationRolePermissionsInput,
): Promise<AuthorizationRole> {
    const response = await httpClient.put<ApiResponse<AuthorizationRole>>(
        `${authorizationBasePath(tenantId)}/roles/${encodeURIComponent(roleId)}/permissions`,
        input,
    )

    return response.data.data
}

async function deactivateRole(tenantId: string, roleId: string): Promise<AuthorizationRole> {
    const response = await httpClient.patch<ApiResponse<AuthorizationRole>>(
        `${authorizationBasePath(tenantId)}/roles/${encodeURIComponent(roleId)}/deactivate`,
    )

    return response.data.data
}

async function getAssignmentReferenceData(
    tenantId: string,
): Promise<AuthorizationAssignmentReferenceData> {
    const response = await httpClient.get<ApiResponse<AuthorizationAssignmentReferenceData>>(
        `${authorizationBasePath(tenantId)}/assignment-reference-data`,
    )

    return response.data.data
}

async function getUserAssignments(
    tenantId: string,
    userId: string,
): Promise<AuthorizationUserRoleAssignment[]> {
    const response = await httpClient.get<ApiResponse<AuthorizationUserRoleAssignment[]>>(
        `${authorizationBasePath(tenantId)}/assignments/users/${encodeURIComponent(userId)}`,
    )

    return response.data.data
}

async function createAssignment(
    tenantId: string,
    input: CreateAuthorizationUserRoleAssignmentInput,
): Promise<AuthorizationUserRoleAssignment> {
    const response = await httpClient.post<ApiResponse<AuthorizationUserRoleAssignment>>(
        `${authorizationBasePath(tenantId)}/assignments`,
        input,
    )

    return response.data.data
}

async function deactivateAssignment(
    tenantId: string,
    assignmentId: string,
): Promise<AuthorizationUserRoleAssignment> {
    const response = await httpClient.patch<ApiResponse<AuthorizationUserRoleAssignment>>(
        `${authorizationBasePath(tenantId)}/assignments/${encodeURIComponent(
            assignmentId,
        )}/deactivate`,
    )

    return response.data.data
}

async function getDelegationReferenceData(
    tenantId: string,
): Promise<AuthorizationDelegationReferenceData> {
    const response = await httpClient.get<ApiResponse<AuthorizationDelegationReferenceData>>(
        `${delegationBasePath(tenantId)}/reference-data`,
    )

    return response.data.data
}

async function getDelegations(tenantId: string): Promise<AuthorizationDelegation[]> {
    const response = await httpClient.get<ApiResponse<AuthorizationDelegation[]>>(
        delegationBasePath(tenantId),
    )

    return response.data.data
}

async function createDelegation(
    tenantId: string,
    input: CreateAuthorizationDelegationInput,
): Promise<AuthorizationDelegation> {
    const response = await httpClient.post<ApiResponse<AuthorizationDelegation>>(
        delegationBasePath(tenantId),
        input,
    )

    return response.data.data
}

async function revokeDelegation(
    tenantId: string,
    delegationId: string,
): Promise<AuthorizationDelegation> {
    const response = await httpClient.patch<ApiResponse<AuthorizationDelegation>>(
        `${delegationBasePath(tenantId)}/${encodeURIComponent(delegationId)}/revoke`,
    )

    return response.data.data
}

async function explainAccess(
    tenantId: string,
    input: ExplainAuthorizationAccessInput,
): Promise<AuthorizationExplainAccessResult> {
    const response = await httpClient.post<ApiResponse<AuthorizationExplainAccessResult>>(
        `${authorizationBasePath(tenantId)}/explain-access`,
        input,
    )

    return response.data.data
}

export const authorizationApi = {
    getCurrentAuthorizationContext,
    getPermissions,
    getRoles,
    initializeDefaultRoles,
    createRole,
    replaceRolePermissions,
    deactivateRole,
    getAssignmentReferenceData,
    getUserAssignments,
    createAssignment,
    deactivateAssignment,
    getDelegationReferenceData,
    getDelegations,
    createDelegation,
    revokeDelegation,
    explainAccess,
}
