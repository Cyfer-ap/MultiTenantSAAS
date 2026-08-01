import {
    httpClient,
    publicHttpClient,
} from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    AcceptInvitationInput,
    AcceptInvitationResponse,
    CreatedInvitation,
    CreateInvitationInput,
    TenantInvitation,
    TenantInvitationsQueryParams,
} from '../types/invitations'

async function getInvitations(
    tenantId: string,
    params: TenantInvitationsQueryParams,
): Promise<PageResponse<TenantInvitation>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<TenantInvitation>>
    >(`/api/tenants/${tenantId}/user-invitations`, {
        params,
    })

    return response.data.data
}

async function createInvitation(
    tenantId: string,
    input: CreateInvitationInput,
): Promise<CreatedInvitation> {
    const response = await httpClient.post<
        ApiResponse<CreatedInvitation>
    >(
        `/api/tenants/${tenantId}/user-invitations`,
        input,
    )

    return response.data.data
}

async function revokeInvitation(
    tenantId: string,
    invitationId: string,
): Promise<TenantInvitation> {
    const response = await httpClient.patch<
        ApiResponse<TenantInvitation>
    >(
        `/api/tenants/${tenantId}/user-invitations/${invitationId}/revoke`,
    )

    return response.data.data
}

async function acceptInvitation(
    input: AcceptInvitationInput,
): Promise<AcceptInvitationResponse> {
    const response = await publicHttpClient.post<
        ApiResponse<AcceptInvitationResponse>
    >('/api/user-invitations/accept', input)

    return response.data.data
}

export const invitationsApi = {
    getInvitations,
    createInvitation,
    revokeInvitation,
    acceptInvitation,
}
