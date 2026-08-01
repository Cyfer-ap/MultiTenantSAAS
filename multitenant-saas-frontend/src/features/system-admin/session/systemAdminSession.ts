import type {
    CurrentSystemAdmin,
    SystemAdminLoginResponse,
    SystemAdminSession,
} from '../types/systemAdmin'

export function createSystemAdminSession(
    response: SystemAdminLoginResponse,
): SystemAdminSession {
    return {
        systemAdminId: response.systemAdminId,
        fullName: response.fullName,
        email: response.email,
        role: response.role,
        accessToken: response.accessToken,
        tokenType: response.tokenType,
        accessTokenExpiresAt:
            Date.now() +
            Math.max(response.expiresInSeconds, 0) * 1_000,
    }
}

export function applyCurrentSystemAdmin(
    session: SystemAdminSession,
    admin: CurrentSystemAdmin,
): SystemAdminSession {
    return {
        ...session,
        systemAdminId: admin.systemAdminId,
        fullName: admin.fullName,
        email: admin.email,
        role: admin.role,
    }
}
