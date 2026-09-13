export const authorizationPermissionCodes = {
    TENANT_READ: 'tenant.read',
    TENANT_UPDATE: 'tenant.update',

    USER_READ: 'user.read',
    USER_CREATE: 'user.create',
    USER_UPDATE: 'user.update',
    USER_STATUS_UPDATE: 'user.status.update',

    ORGANIZATION_UNIT_READ: 'organization.unit.read',
    ORGANIZATION_UNIT_MANAGE: 'organization.unit.manage',

    ORGANIZATION_ASSIGNMENT_READ: 'organization.assignment.read',
    ORGANIZATION_ASSIGNMENT_MANAGE: 'organization.assignment.manage',

    PROJECT_READ: 'project.read',
    PROJECT_CREATE: 'project.create',
    PROJECT_UPDATE: 'project.update',
    PROJECT_ARCHIVE: 'project.archive',
    PROJECT_MEMBER_MANAGE: 'project.member.manage',
    PROJECT_TASK_READ: 'project.task.read',
    PROJECT_TASK_MANAGE: 'project.task.manage',

    AUDIT_READ: 'audit.read',
    AUTHORIZATION_MANAGE: 'authorization.manage',
    AUTHORIZATION_DELEGATE: 'authorization.delegate',

    SUBSCRIPTION_READ: 'subscription.read',
} as const

export type AuthorizationPermissionCode =
    (typeof authorizationPermissionCodes)[keyof typeof authorizationPermissionCodes]

export type AuthorizationRoleSource = 'SYSTEM' | 'TENANT'

export type AuthorizationScopeType =
    | 'TENANT'
    | 'ORGANIZATIONAL_UNIT'
    | 'ORGANIZATIONAL_SUBTREE'
    | 'DIRECT_REPORTS'
    | 'PROJECT'
    | 'SELF'

export interface AuthorizationAssignmentUserOption {
    id: string
    fullName: string
    email: string
}

export interface AuthorizationAssignmentScopeTargetOption {
    id: string
    label: string
    description: string
    ownerUserId: string | null
}

export interface AuthorizationAssignmentReferenceData {
    users: AuthorizationAssignmentUserOption[]
    organizationalUnits: AuthorizationAssignmentScopeTargetOption[]
    projects: AuthorizationAssignmentScopeTargetOption[]
    directReportsAnchors: AuthorizationAssignmentScopeTargetOption[]
}

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

export type AuthorizationEntityStatus = 'ACTIVE' | 'INACTIVE'

export interface AuthorizationPermission {
    id: string
    tenantId: string | null
    code: string
    name: string
    description: string | null
    category: string
    source: AuthorizationRoleSource
    status: AuthorizationEntityStatus
    createdAt: string
    updatedAt: string
}

export interface AuthorizationRole {
    id: string
    tenantId: string
    code: string
    name: string
    description: string | null
    source: AuthorizationRoleSource
    status: AuthorizationEntityStatus
    permissions: AuthorizationPermission[]
    createdAt: string
    updatedAt: string
}

export interface AuthorizationUserRoleAssignment {
    id: string
    tenantId: string
    userId: string
    userFullName: string
    userEmail: string
    roleId: string
    roleCode: string
    roleName: string
    roleSource: AuthorizationRoleSource
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    status: AuthorizationEntityStatus
    validFrom: string
    validUntil: string | null
    createdByUserId: string | null
    createdByUserEmail: string | null
    createdAt: string
    updatedAt: string
}

export interface CreateAuthorizationRoleInput {
    code: string
    name: string
    description: string | null
    permissionIds: string[]
}

export interface ReplaceAuthorizationRolePermissionsInput {
    permissionIds: string[]
}

export interface CreateAuthorizationUserRoleAssignmentInput {
    userId: string
    roleId: string
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    validFrom: string | null
    validUntil: string | null
}

export type AuthorizationDelegationStatus = 'ACTIVE' | 'REVOKED'

export interface AuthorizationDelegationParentAssignmentOption {
    id: string
    roleId: string
    roleCode: string
    roleName: string
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    validFrom: string
    validUntil: string | null
    permissionCodes: string[]
}

export interface AuthorizationDelegationReferenceData {
    users: AuthorizationAssignmentUserOption[]
    roles: AuthorizationRole[]
    parentAssignments: AuthorizationDelegationParentAssignmentOption[]
    organizationalUnits: AuthorizationAssignmentScopeTargetOption[]
    projects: AuthorizationAssignmentScopeTargetOption[]
}

export interface AuthorizationDelegation {
    id: string
    tenantId: string
    delegatorUserId: string
    delegatorEmail: string
    delegateUserId: string
    delegateEmail: string
    parentAssignmentId: string
    delegatedAssignmentId: string
    roleId: string
    roleCode: string
    roleName: string
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    status: AuthorizationDelegationStatus
    validFrom: string
    validUntil: string
    createdAt: string
    revokedAt: string | null
    revokedByUserId: string | null
    revokedByEmail: string | null
}

export interface CreateAuthorizationDelegationInput {
    delegateUserId: string
    parentAssignmentId: string
    roleId: string
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    validFrom: string | null
    validUntil: string
}

export type AuthorizationAccessContextType =
    | 'TENANT'
    | 'USER'
    | 'PROJECT'
    | 'ORGANIZATIONAL_UNIT'
    | 'ORGANIZATIONAL_SUBTREE'
    | 'DIRECT_REPORTS_ANCHOR'

export type AuthorizationAccessDecisionReason =
    | 'GRANTED_BY_ROLE_ASSIGNMENT'
    | 'INVALID_INPUT'
    | 'INVALID_PERMISSION_CODE'
    | 'TENANT_UNAVAILABLE'
    | 'SUBJECT_UNAVAILABLE'
    | 'NO_EFFECTIVE_GRANT'
    | 'SCOPE_NOT_SATISFIED'
    | 'DELEGATION_SOURCE_UNAVAILABLE'

export type AuthorizationGrantSource = 'DIRECT' | 'DELEGATED'

export interface AuthorizationMatchedGrant {
    assignmentId: string
    roleId: string
    roleCode: string
    scopeType: AuthorizationScopeType
    scopeTargetId: string | null
    validFrom: string
    validUntil: string | null
    grantSource: AuthorizationGrantSource
    delegationId: string | null
    parentAssignmentId: string | null
    delegatorUserId: string | null
    delegatorEmail: string | null
}

export interface ExplainAuthorizationAccessInput {
    userId: string
    permissionCode: string
    contextType: AuthorizationAccessContextType
    targetId: string | null
    effectiveAt: string | null
}

export interface AuthorizationExplainAccessResult {
    tenantId: string
    userId: string
    permissionCode: string
    contextType: AuthorizationAccessContextType
    targetId: string | null
    granted: boolean
    reason: AuthorizationAccessDecisionReason
    evaluatedAt: string
    matchedGrant: AuthorizationMatchedGrant | null
}
