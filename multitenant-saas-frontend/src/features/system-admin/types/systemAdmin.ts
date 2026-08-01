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
