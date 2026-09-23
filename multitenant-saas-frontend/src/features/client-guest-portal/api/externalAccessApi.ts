import { httpClient, publicHttpClient } from '../../../api/httpClient'
import type { ApiResponse, PageResponse } from '../../../types/api'
import type {
    CreateExternalAccessGrantInput,
    ExternalAccessGrant,
    ExternalAccessGrantCreated,
    GuestApprovalDecision,
    GuestApprovalDecisionOutcome,
    GuestApprovalReviews,
    GuestComments,
    GuestComment,
    GuestExchangeResponse,
    GuestSession,
    GuestTasks,
} from '../types/externalAccess'

const grantBasePath = (tenantId: string, projectId: string) =>
    `/api/tenants/${tenantId}/projects/${projectId}/external-access-grants`

const guestBasePath = '/api/public/guest-portal'
const guestSessionHeader = 'X-Guest-Session'

async function listGrants(
    tenantId: string,
    projectId: string,
): Promise<PageResponse<ExternalAccessGrant>> {
    const response = await httpClient.get<ApiResponse<PageResponse<ExternalAccessGrant>>>(
        grantBasePath(tenantId, projectId),
        { params: { page: 0, size: 100 } },
    )
    return response.data.data
}

async function createGrant(
    tenantId: string,
    projectId: string,
    input: CreateExternalAccessGrantInput,
): Promise<ExternalAccessGrantCreated> {
    const response = await httpClient.post<ApiResponse<ExternalAccessGrantCreated>>(
        grantBasePath(tenantId, projectId),
        input,
    )
    return response.data.data
}

async function revokeGrant(
    tenantId: string,
    projectId: string,
    grantId: string,
): Promise<ExternalAccessGrant> {
    const response = await httpClient.delete<ApiResponse<ExternalAccessGrant>>(
        `${grantBasePath(tenantId, projectId)}/${grantId}`,
    )
    return response.data.data
}

async function exchangeInvitation(invitationToken: string): Promise<GuestExchangeResponse> {
    const response = await publicHttpClient.post<ApiResponse<GuestExchangeResponse>>(
        `${guestBasePath}/exchange`,
        { invitationToken },
    )
    return response.data.data
}

async function getGuestSession(sessionToken: string): Promise<GuestSession> {
    const response = await publicHttpClient.get<ApiResponse<GuestSession>>(
        `${guestBasePath}/session`,
        { headers: { [guestSessionHeader]: sessionToken } },
    )
    return response.data.data
}

async function getGuestTasks(sessionToken: string): Promise<GuestTasks> {
    const response = await publicHttpClient.get<ApiResponse<GuestTasks>>(`${guestBasePath}/tasks`, {
        headers: { [guestSessionHeader]: sessionToken },
    })
    return response.data.data
}

async function getGuestTaskComments(sessionToken: string, taskId: string): Promise<GuestComments> {
    const response = await publicHttpClient.get<ApiResponse<GuestComments>>(
        `${guestBasePath}/tasks/${taskId}/comments`,
        { headers: { [guestSessionHeader]: sessionToken } },
    )
    return response.data.data
}

async function createGuestTaskComment(
    sessionToken: string,
    taskId: string,
    body: string,
): Promise<GuestComment> {
    const response = await publicHttpClient.post<ApiResponse<GuestComment>>(
        `${guestBasePath}/tasks/${taskId}/comments`,
        { body },
        { headers: { [guestSessionHeader]: sessionToken } },
    )
    return response.data.data
}

async function getGuestApprovals(sessionToken: string): Promise<GuestApprovalReviews> {
    const response = await publicHttpClient.get<ApiResponse<GuestApprovalReviews>>(
        `${guestBasePath}/approvals`,
        { headers: { [guestSessionHeader]: sessionToken } },
    )
    return response.data.data
}

async function decideGuestApproval(
    sessionToken: string,
    requestId: string,
    outcome: GuestApprovalDecisionOutcome,
    comment: string | null,
): Promise<GuestApprovalDecision> {
    const response = await publicHttpClient.post<ApiResponse<GuestApprovalDecision>>(
        `${guestBasePath}/approvals/${requestId}/decision`,
        { outcome, comment },
        { headers: { [guestSessionHeader]: sessionToken } },
    )
    return response.data.data
}

export const externalAccessApi = {
    listGrants,
    createGrant,
    revokeGrant,
    exchangeInvitation,
    getGuestSession,
    getGuestTasks,
    getGuestTaskComments,
    createGuestTaskComment,
    getGuestApprovals,
    decideGuestApproval,
}
