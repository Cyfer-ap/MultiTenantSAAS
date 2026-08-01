import type {
    TenantRole,
    UserStatus,
} from '../../auth/types/auth'

export type UserSortField =
    | 'createdAt'
    | 'fullName'
    | 'email'
    | 'role'
    | 'status'

export type SortDirection = 'asc' | 'desc'

export interface TenantUser {
    id: string
    tenantId: string
    fullName: string
    email: string
    role: TenantRole
    status: UserStatus
    createdAt: string
    updatedAt: string
}

export interface TenantUsersQueryParams {
    page: number
    size: number
    sortBy: UserSortField
    sortDir: SortDirection
    role?: TenantRole
    status?: UserStatus
    search?: string
}

export interface CreateTenantUserInput {
    fullName: string
    email: string
    password: string
    role: TenantRole
}

export interface UpdateTenantUserInput {
    fullName: string
    email: string
}

export interface UpdateTenantUserRoleInput {
    role: TenantRole
}

export interface UpdateTenantUserStatusInput {
    status: UserStatus
}
