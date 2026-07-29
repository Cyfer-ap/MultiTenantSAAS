import type { TenantRole } from '../types/auth'

export const allTenantRoles = [
    'TENANT_ADMIN',
    'TENANT_MANAGER',
    'TENANT_USER',
] as const satisfies readonly TenantRole[]

export const tenantManagementRoles = [
    'TENANT_ADMIN',
    'TENANT_MANAGER',
] as const satisfies readonly TenantRole[]

export const tenantAdminRoles = [
    'TENANT_ADMIN',
] as const satisfies readonly TenantRole[]

export function hasAllowedTenantRole(
    role: TenantRole,
    allowedRoles: readonly TenantRole[],
): boolean {
    return allowedRoles.includes(role)
}

export function getDefaultAuthenticatedPath(
    role: TenantRole,
): string {
    return role === 'TENANT_USER'
        ? '/projects'
        : '/dashboard'
}
