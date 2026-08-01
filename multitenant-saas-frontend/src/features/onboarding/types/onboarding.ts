import type {
    TenantRole,
    UserStatus,
} from '../../auth/types/auth'

export type TenantStatus =
    | 'ACTIVE'
    | 'INACTIVE'
    | 'SUSPENDED'

export interface TenantOnboardingInput {
    tenantName: string
    tenantSlug: string
    adminFullName: string
    adminEmail: string
    adminPassword: string
}

export interface OnboardedTenant {
    id: string
    name: string
    slug: string
    status: TenantStatus
    createdAt: string
    updatedAt: string
}

export interface InitialTenantAdmin {
    id: string
    tenantId: string
    fullName: string
    email: string
    role: TenantRole
    status: UserStatus
    createdAt: string
    updatedAt: string
}

export interface TenantOnboardingResponse {
    tenant: OnboardedTenant
    adminUser: InitialTenantAdmin
    message: string
}
