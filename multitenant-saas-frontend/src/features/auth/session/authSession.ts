import type {
    AuthSession,
    CurrentUserResponse,
    LoginResponse,
    TokenRefreshResponse,
} from '../types/auth'

function calculateExpirationTime(expiresInSeconds: number): number {
    return Date.now() + Math.max(expiresInSeconds, 0) * 1_000
}

export function createAuthSession(response: LoginResponse): AuthSession {
    return {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        accessTokenExpiresAt: calculateExpirationTime(response.expiresInSeconds),
        tenantId: response.tenantId,
        userId: response.userId,
        fullName: response.fullName,
        email: response.email,
        role: response.role,
    }
}

export function applyTokenRefresh(
    session: AuthSession,
    response: TokenRefreshResponse,
): AuthSession {
    return {
        ...session,
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        accessTokenExpiresAt: calculateExpirationTime(response.expiresInSeconds),
    }
}

export function applyCurrentUser(session: AuthSession, user: CurrentUserResponse): AuthSession {
    return {
        ...session,
        tenantId: user.tenantId,
        userId: user.userId,
        fullName: user.fullName,
        email: user.email,
        role: user.role,
    }
}
