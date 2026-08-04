export const authorizationPermissionCodes = {
    TENANT_READ: 'tenant.read',
    TENANT_UPDATE: 'tenant.update',

    USER_READ: 'user.read',
    USER_CREATE: 'user.create',
    USER_UPDATE: 'user.update',
    USER_STATUS_UPDATE: 'user.status.update',

    ORGANIZATION_UNIT_READ:
        'organization.unit.read',
    ORGANIZATION_UNIT_MANAGE:
        'organization.unit.manage',

    ORGANIZATION_ASSIGNMENT_READ:
        'organization.assignment.read',
    ORGANIZATION_ASSIGNMENT_MANAGE:
        'organization.assignment.manage',

    PROJECT_READ: 'project.read',
    PROJECT_CREATE: 'project.create',
    PROJECT_UPDATE: 'project.update',
    PROJECT_ARCHIVE: 'project.archive',
    PROJECT_MEMBER_MANAGE:
        'project.member.manage',
    PROJECT_TASK_READ: 'project.task.read',
    PROJECT_TASK_MANAGE: 'project.task.manage',

    AUDIT_READ: 'audit.read',
    AUTHORIZATION_MANAGE:
        'authorization.manage',
} as const

export type AuthorizationPermissionCode =
    typeof authorizationPermissionCodes[
        keyof typeof authorizationPermissionCodes
    ]

export type AuthorizationRoleSource =
    | 'SYSTEM'
    | 'TENANT'

export type AuthorizationScopeType =
    | 'TENANT'
    | 'ORGANIZATIONAL_UNIT'
    | 'ORGANIZATIONAL_SUBTREE'
    | 'DIRECT_REPORTS'
    | 'PROJECT'
    | 'SELF'

export interface CurrentAuthorizationGrant {
    assignmentId: string
    roleId: string
    roleCode: string
    roleName: string
    roleSource: AuthorizationRoleSource
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    validFrom: string
    validUntil: string | null
    permissionCodes: string[]
}

export interface CurrentAuthorizationContext {
    tenantId: string
    userId: string
    fullName: string
    email: string
    evaluatedAt: string
    tenantPermissionCodes: string[]
    allPermissionCodes: string[]
    grants: CurrentAuthorizationGrant[]
}
