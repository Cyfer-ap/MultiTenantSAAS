export type TenantRole = 'TENANT_ADMIN' | 'TENANT_MANAGER' | 'TENANT_USER'

export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED'

export interface LoginRequest {
    email: string
    password: string
    workspaceGrantId: string
    keepSignedIn: boolean
}

export interface LoginInput extends LoginRequest {
    tenantId: string
}

export interface WorkspaceLoginOption {
    tenantId: string
    name: string
    slug: string
}

export interface WorkspaceDiscoveryStartRequest {
    email: string
}

export interface WorkspaceDiscoveryStartResponse {
    verificationRequired: boolean
    challengeId: string | null
    workspaces: WorkspaceLoginOption[]
    workspaceGrantId: string | null
    expiresInSeconds: number
    message: string
}

export interface WorkspaceDiscoveryVerifyRequest {
    challengeId: string
    code: string
    trustBrowser: boolean
}

export interface WorkspaceDiscoveryVerifyResponse {
    workspaces: WorkspaceLoginOption[]
    workspaceGrantId: string | null
    trustedBrowserToken: string | null
    message: string
}

export interface TokenPairResponse {
    accessToken: string
    refreshToken: string | null
    csrfToken: string
    tokenType: string
    expiresInSeconds: number
    persistentSession: boolean
}

export interface LoginResponse extends TokenPairResponse {
    tenantId: string
    userId: string
    fullName: string
    email: string
    role: TenantRole
    message: string
}

export type TokenRefreshResponse = TokenPairResponse

export interface LogoutResponse {
    message: string
}

export interface ChangePasswordInput {
    currentPassword: string
    newPassword: string
    confirmPassword: string
}

export interface ChangePasswordResponse {
    message: string
}

export interface CurrentUserResponse {
    tenantId: string
    tenantName: string
    tenantSlug: string
    userId: string
    fullName: string
    email: string
    role: TenantRole
    status: UserStatus
}

export interface AuthSession {
    accessToken: string
    csrfToken: string
    tokenType: string
    accessTokenExpiresAt: number
    persistentSession: boolean
    tenantId: string
    userId: string
    fullName: string
    email: string
    role: TenantRole
}
