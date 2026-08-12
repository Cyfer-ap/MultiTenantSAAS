import type { AuthorizationScopeType, CurrentAuthorizationContext } from '../types/authorization'
import { authorizationPermissionCodes } from '../types/authorization'

export const dashboardRequiredTenantPermissions = [
    authorizationPermissionCodes.TENANT_READ,
    authorizationPermissionCodes.USER_READ,
    authorizationPermissionCodes.PROJECT_READ,
    authorizationPermissionCodes.PROJECT_TASK_READ,
] as const

export function hasTenantPermission(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCode: string,
): boolean {
    return Boolean(context?.tenantPermissionCodes.includes(permissionCode))
}

export function hasAllTenantPermissions(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCodes: readonly string[],
): boolean {
    return permissionCodes.every((permissionCode) => hasTenantPermission(context, permissionCode))
}

export function hasAnyTenantPermission(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCodes: readonly string[],
): boolean {
    return permissionCodes.some((permissionCode) => hasTenantPermission(context, permissionCode))
}

export function hasScopedPermission(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCode: string,
    scopeType: AuthorizationScopeType,
    scopeTargetId: string | null,
): boolean {
    if (hasTenantPermission(context, permissionCode)) {
        return true
    }

    if (!context) {
        return false
    }

    return context.grants.some(
        (grant) =>
            grant.scopeType === scopeType &&
            grant.scopeTargetId === scopeTargetId &&
            grant.permissionCodes.includes(permissionCode),
    )
}

export function hasProjectPermission(
    context: CurrentAuthorizationContext | null | undefined,
    permissionCode: string,
    projectId: string,
): boolean {
    return hasScopedPermission(context, permissionCode, 'PROJECT', projectId)
}

export function getDefaultAuthorizedPath(context: CurrentAuthorizationContext): string {
    if (hasAllTenantPermissions(context, dashboardRequiredTenantPermissions)) {
        return '/dashboard'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.PROJECT_READ)) {
        return '/projects'
    }

    if (
        hasAllTenantPermissions(context, [
            authorizationPermissionCodes.USER_READ,
            authorizationPermissionCodes.USER_CREATE,
        ])
    ) {
        return '/invitations'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.USER_READ)) {
        return '/users'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.ORGANIZATION_UNIT_READ)) {
        return '/organization'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.AUTHORIZATION_MANAGE)) {
        return '/authorization'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.SUBSCRIPTION_READ)) {
        return '/subscription'
    }

    if (hasTenantPermission(context, authorizationPermissionCodes.AUDIT_READ)) {
        return '/audit-logs'
    }

    const firstReadableProject = context.grants.find(
        (grant) =>
            grant.scopeType === 'PROJECT' &&
            grant.scopeTargetId !== null &&
            grant.permissionCodes.includes(authorizationPermissionCodes.PROJECT_READ),
    )

    if (firstReadableProject?.scopeTargetId) {
        return `/projects/${firstReadableProject.scopeTargetId}`
    }

    return '/account'
}
