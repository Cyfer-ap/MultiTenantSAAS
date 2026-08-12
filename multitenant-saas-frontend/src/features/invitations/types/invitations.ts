import type { TenantRole, UserStatus } from '../../auth/types/auth'

export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REVOKED'

export type InvitationSortField =
    'createdAt' | 'fullName' | 'email' | 'role' | 'status' | 'expiresAt'

export type SortDirection = 'asc' | 'desc'

export interface TenantInvitation {
    invitationId: string
    tenantId: string
    fullName: string
    email: string
    role: TenantRole
    status: InvitationStatus
    active: boolean
    expired: boolean
    expiresAt: string
    createdAt: string
    acceptedAt: string | null
    revokedAt: string | null
    invitedByUserId: string | null
    invitedByUserEmail: string | null
    invitedBySystemAdminId: string | null
    invitedBySystemAdminEmail: string | null
}

export interface TenantInvitationsQueryParams {
    page: number
    size: number
    sortBy: InvitationSortField
    sortDir: SortDirection
    status?: InvitationStatus
    role?: TenantRole
    search?: string
}

export interface CreateInvitationInput {
    fullName: string
    email: string
    role: TenantRole
}

export interface CreatedInvitation {
    invitationId: string
    tenantId: string
    fullName: string
    email: string
    role: TenantRole
    status: InvitationStatus
    expiresAt: string
    devInvitationToken: string | null
    message: string
}

export interface AcceptInvitationInput {
    invitationToken: string
    newPassword: string
    confirmPassword: string
}

export interface AcceptedInvitationUser {
    id: string
    tenantId: string
    fullName: string
    email: string
    role: TenantRole
    status: UserStatus
    createdAt: string
    updatedAt: string
}

export interface AcceptInvitationResponse {
    user: AcceptedInvitationUser
    message: string
}
