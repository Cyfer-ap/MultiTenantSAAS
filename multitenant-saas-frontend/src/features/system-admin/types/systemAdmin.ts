import type { TenantOnboardingInput, TenantOnboardingResponse, TenantStatus } from '../../onboarding/types/onboarding'

export type SystemAdminStatus =
    | 'ACTIVE'
    | 'INACTIVE'
    | 'SUSPENDED'

export interface SystemAdminLoginInput {
    email: string
    password: string
}

export interface SystemAdminLoginResponse {
    systemAdminId: string
    fullName: string
    email: string
    role: 'SYSTEM_ADMIN'
    accessToken: string
    tokenType: string
    expiresInSeconds: number
    message: string
}

export interface ChangeSystemAdminPasswordInput {
    currentPassword: string
    newPassword: string
    confirmPassword: string
}

export interface CurrentSystemAdmin {
    systemAdminId: string
    fullName: string
    email: string
    role: 'SYSTEM_ADMIN'
    status: SystemAdminStatus
}

export interface SystemAdminSession {
    systemAdminId: string
    fullName: string
    email: string
    role: 'SYSTEM_ADMIN'
    accessToken: string
    tokenType: string
    accessTokenExpiresAt: number
}

export interface SystemDashboardSummary {
    totalTenants: number
    activeTenants: number
    inactiveTenants: number
    suspendedTenants: number
    totalUsers: number
    activeUsers: number
    inactiveUsers: number
    suspendedUsers: number
}

export type SystemTenantSortField =
    | 'createdAt'
    | 'name'
    | 'slug'
    | 'status'

export type SortDirection = 'asc' | 'desc'

export interface SystemTenant {
    id: string
    name: string
    slug: string
    status: TenantStatus
    createdAt: string
    updatedAt: string
}

export interface SystemTenantsQueryParams {
    page: number
    size: number
    sortBy: SystemTenantSortField
    sortDir: SortDirection
    status?: TenantStatus
    search?: string
}

export interface UpdateSystemTenantStatusInput {
    status: TenantStatus
}

export type SystemTenantOnboardingInput = TenantOnboardingInput
export type SystemTenantOnboardingResponse = TenantOnboardingResponse

export interface SystemAdminRecord {
    id: string
    fullName: string
    email: string
    status: SystemAdminStatus
    failedLoginAttempts: number
    lockedUntil: string | null
    createdAt: string
    updatedAt: string
}

export type SystemAdminSortField =
    | 'createdAt'
    | 'fullName'
    | 'email'
    | 'status'

export interface SystemAdminsQueryParams {
    page: number
    size: number
    sortBy: SystemAdminSortField
    sortDir: SortDirection
    status?: SystemAdminStatus
    search?: string
}

export interface CreateSystemAdminInput {
    fullName: string
    email: string
    password: string
}

export interface UpdateSystemAdminStatusInput {
    status: SystemAdminStatus
}

export const platformAuditActions = [
    'SYSTEM_ADMIN_CREATED',
    'SYSTEM_ADMIN_STATUS_UPDATED',
    'SYSTEM_ADMIN_LOGIN_UNLOCKED',
] as const

export type PlatformAuditAction =
    (typeof platformAuditActions)[number]

export type PlatformAuditSortField =
    | 'createdAt'
    | 'action'
    | 'success'

export interface PlatformAuditLog {
    id: string
    actorSystemAdminId: string | null
    actorSystemAdminEmail: string | null
    targetSystemAdminId: string | null
    targetSystemAdminEmail: string | null
    action: PlatformAuditAction
    success: boolean
    message: string
    createdAt: string
}

export interface PlatformAuditLogsQueryParams {
    page: number
    size: number
    sortBy: PlatformAuditSortField
    sortDir: SortDirection
    action?: PlatformAuditAction
    success?: boolean
    search?: string
}
