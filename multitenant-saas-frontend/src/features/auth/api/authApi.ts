import { httpClient, publicHttpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ChangePasswordInput,
    ChangePasswordResponse,
    CurrentUserResponse,
    LoginRequest,
    LoginResponse,
    LogoutResponse,
    TokenRefreshResponse,
    WorkspaceDiscoveryStartRequest,
    WorkspaceDiscoveryStartResponse,
    WorkspaceDiscoveryVerifyRequest,
    WorkspaceDiscoveryVerifyResponse,
} from '../types/auth'

const CSRF_HEADER = 'X-CSRF-Token'

async function login(tenantId: string, request: LoginRequest): Promise<LoginResponse> {
    const response = await publicHttpClient.post<ApiResponse<LoginResponse>>(
        `/api/tenants/${encodeURIComponent(tenantId)}/auth/login`,
        request,
    )

    return response.data.data
}

async function startWorkspaceDiscovery(
    request: WorkspaceDiscoveryStartRequest,
): Promise<WorkspaceDiscoveryStartResponse> {
    const response = await publicHttpClient.post<ApiResponse<WorkspaceDiscoveryStartResponse>>(
        '/api/auth/workspaces/start',
        request,
    )

    return response.data.data
}

async function verifyWorkspaceDiscovery(
    request: WorkspaceDiscoveryVerifyRequest,
): Promise<WorkspaceDiscoveryVerifyResponse> {
    const response = await publicHttpClient.post<ApiResponse<WorkspaceDiscoveryVerifyResponse>>(
        '/api/auth/workspaces/verify',
        request,
    )

    return response.data.data
}

async function refreshToken(csrfToken: string): Promise<TokenRefreshResponse> {
    const response = await publicHttpClient.post<ApiResponse<TokenRefreshResponse>>(
        '/api/auth/refresh',
        undefined,
        {
            headers: {
                [CSRF_HEADER]: csrfToken,
            },
        },
    )

    return response.data.data
}

async function getCurrentUser(): Promise<CurrentUserResponse> {
    const response = await httpClient.get<ApiResponse<CurrentUserResponse>>('/api/auth/me')

    return response.data.data
}

async function logout(csrfToken: string): Promise<void> {
    await publicHttpClient.post('/api/auth/logout', undefined, {
        headers: {
            [CSRF_HEADER]: csrfToken,
        },
    })
}

async function logoutAllDevices(): Promise<LogoutResponse> {
    const response = await httpClient.post<ApiResponse<LogoutResponse>>('/api/auth/logout-all')

    return response.data.data
}

async function changePassword(request: ChangePasswordInput): Promise<ChangePasswordResponse> {
    const response = await httpClient.post<ApiResponse<ChangePasswordResponse>>(
        '/api/auth/change-password',
        request,
    )

    return response.data.data
}

export const authApi = {
    login,
    startWorkspaceDiscovery,
    verifyWorkspaceDiscovery,
    refreshToken,
    getCurrentUser,
    logout,
    logoutAllDevices,
    changePassword,
}
