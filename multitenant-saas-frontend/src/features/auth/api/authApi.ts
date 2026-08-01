import {
    httpClient,
    publicHttpClient,
} from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    ChangePasswordInput,
    ChangePasswordResponse,
    CurrentUserResponse,
    LoginRequest,
    LoginResponse,
    LogoutRequest,
    LogoutResponse,
    RefreshTokenRequest,
    TokenRefreshResponse,
} from '../types/auth'

async function login(
    tenantId: string,
    request: LoginRequest,
): Promise<LoginResponse> {
    const response =
        await publicHttpClient.post<
            ApiResponse<LoginResponse>
        >(
            `/api/tenants/${encodeURIComponent(
                tenantId,
            )}/auth/login`,
            request,
        )

    return response.data.data
}

async function refreshToken(
    refreshTokenValue: string,
): Promise<TokenRefreshResponse> {
    const request: RefreshTokenRequest = {
        refreshToken: refreshTokenValue,
    }

    const response =
        await publicHttpClient.post<
            ApiResponse<TokenRefreshResponse>
        >('/api/auth/refresh', request)

    return response.data.data
}

async function getCurrentUser(): Promise<CurrentUserResponse> {
    const response =
        await httpClient.get<
            ApiResponse<CurrentUserResponse>
        >('/api/auth/me')

    return response.data.data
}

async function logout(
    refreshTokenValue: string,
): Promise<void> {
    const request: LogoutRequest = {
        refreshToken: refreshTokenValue,
    }

    await publicHttpClient.post(
        '/api/auth/logout',
        request,
    )
}

async function logoutAllDevices(): Promise<LogoutResponse> {
    const response =
        await httpClient.post<
            ApiResponse<LogoutResponse>
        >('/api/auth/logout-all')

    return response.data.data
}

async function changePassword(
    request: ChangePasswordInput,
): Promise<ChangePasswordResponse> {
    const response =
        await httpClient.post<
            ApiResponse<ChangePasswordResponse>
        >('/api/auth/change-password', request)

    return response.data.data
}

export const authApi = {
    login,
    refreshToken,
    getCurrentUser,
    logout,
    logoutAllDevices,
    changePassword,
}
