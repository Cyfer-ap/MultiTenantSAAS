# Authorization V2 — Hierarchical and Scoped Access-Control Design

This document defines the next major backend and frontend phase.

## 1. Problem statement

The fixed-role model cannot represent organizations such as:

```text
Tenant
├── Manager A
│   ├── Assistant Manager A1
│   │   ├── Manager A1.1
│   │   │   └── Members
│   │   └── Members
│   └── Assistant Manager A2
└── Manager B
    └── Additional nested management levels
```

Required properties:

```text
Arbitrary organizational depth
Variable numbers of managers and sub-managers
Different authority scopes for users with the same role
Direct-report authority
Full-subtree authority
Project-specific authority
Temporary delegated authority
Tenant-defined roles
Auditable authorization decisions
```

Do not model this with fixed columns or numbered role names.

## 2. Authorization model

Authorization v2 combines:

```text
RBAC
    Defines which actions a role permits.

Organizational hierarchy
    Defines reporting and unit relationships.

Scoped assignments
    Defines where the role applies.

Relationship-based rules
    Define access from project membership, task assignment,
    reporting relationships, or ownership.

Contextual rules
    Enforce status, expiration, protected-rank,
    and resource-state constraints.
```

Conceptual decision:

```text
Allow when:
same tenant
AND permission is effective
AND assignment scope contains target
AND required relationship is satisfied
AND safety/context constraints pass
```

## 3. Core domain concepts

### OrganizationalUnit

```text
id
tenantId
name
code
type
parentUnitId
status
createdAt
updatedAt
```

Suggested types:

```text
COMPANY
DIVISION
DEPARTMENT
TEAM
SUBTEAM
BRANCH
CUSTOM
```

Types are descriptive and must not impose maximum depth.

### OrganizationalUnitClosure

```text
tenantId
ancestorUnitId
descendantUnitId
depth
```

Use:

```text
Adjacency list for the canonical parent relation
Closure table for fast ancestor/descendant checks
```

A self row exists with depth zero.

### UserOrganizationAssignment

```text
id
tenantId
userId
organizationalUnitId
reportsToAssignmentId
positionTitle
primaryAssignment
status
validFrom
validUntil
createdBy
createdAt
updatedAt
```

This supports primary and secondary assignments, direct reporting, temporal assignment, and matrix organizations.

### Permission

```text
id
code
category
description
sensitive
assignable
```

Permission codes are platform-defined and stable.

Examples:

```text
ORG_UNIT_READ
ORG_UNIT_CREATE
ORG_UNIT_UPDATE
ORG_UNIT_MOVE
ORG_ASSIGN_USER
ORG_SET_REPORTING_LINE

USER_READ
USER_CREATE
USER_UPDATE
USER_CHANGE_STATUS
USER_ASSIGN_ROLE

ROLE_READ
ROLE_CREATE
ROLE_UPDATE
ROLE_ASSIGN

INVITATION_CREATE
INVITATION_READ
INVITATION_REVOKE

PROJECT_READ
PROJECT_CREATE
PROJECT_UPDATE
PROJECT_ARCHIVE
PROJECT_MEMBER_MANAGE

TASK_READ
TASK_CREATE
TASK_UPDATE
TASK_ASSIGN
TASK_CHANGE_STATUS
TASK_CANCEL

AUDIT_LOG_READ
SECURITY_SESSION_REVOKE
```

### AuthorizationRole

```text
id
tenantId nullable for platform-defined roles
name
description
roleKind
protectedRole
assignable
status
createdAt
updatedAt
```

Role kinds:

```text
SYSTEM
TENANT_CUSTOM
```

### RolePermission

```text
roleId
permissionId
```

### UserRoleAssignment

```text
id
tenantId
userId
roleId
scopeType
scopeId
validFrom
validUntil
assignedByUserId
revokedAt
revokedByUserId
createdAt
```

Initial scope types:

```text
TENANT
ORGANIZATIONAL_UNIT
ORGANIZATIONAL_SUBTREE
DIRECT_REPORTS
PROJECT
SELF
```

### PermissionDelegation

```text
id
tenantId
delegatedByUserId
delegatedToUserId
roleId or delegated permission set
scopeType
scopeId
validFrom
validUntil
allowRedelegation
revokedAt
createdAt
```

Delegation is a later phase after role assignments are stable.

## 4. Protected roles and compatibility

Keep protected roles that preserve existing behavior:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Initial compatibility mapping:

```text
TENANT_ADMIN
    Platform-defined tenant role
    Tenant-wide scope
    Broad current permission set

TENANT_MANAGER
    Platform-defined tenant role
    Tenant-wide scope during compatibility period
    Current operational permission set

TENANT_USER
    Platform-defined tenant role
    Basic self/project relationship permissions
```

Migration strategy:

```text
1. Create permission and role tables.
2. Seed protected roles and permissions.
3. Create tenant-wide assignments matching APP_USERS.role.
4. Preserve APP_USERS.role temporarily.
5. Evaluate new assignments in compatibility/shadow mode.
6. Migrate modules one at a time.
7. Remove legacy dependence only after regression coverage is complete.
```

Do not delete the old role column in the first authorization-v2 migration.

## 5. Hierarchy invariants

The service layer must prevent:

```text
A unit parenting itself
A unit moving below its own descendant
Cross-tenant parent/child relations
Closure rows crossing tenants
Duplicate active unit codes within a tenant when codes are used
Active assignments to unavailable users/units
Reporting to an assignment from another tenant
Reporting cycles
A user reporting to themselves
```

Hierarchy mutations must be transactional.

## 6. Scope semantics

### TENANT

Permission applies to every tenant-owned resource permitted by the permission.

### ORGANIZATIONAL_UNIT

Permission applies to the selected unit itself, not automatically to descendants.

### ORGANIZATIONAL_SUBTREE

Permission applies to the selected unit and every descendant.

### DIRECT_REPORTS

Permission applies to users whose active assignment reports directly to the manager's active assignment.

### PROJECT

Permission applies only to the selected project and nested project resources.

### SELF

Permission applies only to the current user or personal account resources.

Scope semantics must be centralized and tested independently.

## 7. Effective-permission evaluation

Suggested input:

```text
principal
permissionCode
target descriptor
request context
```

Target descriptor may contain:

```text
tenantId
organizationalUnitId
targetUserId
projectId
taskId
resourceStatus
```

Evaluation sequence:

```text
1. Validate principal/account type.
2. Validate tenant and active account state.
3. Load active non-expired role assignments.
4. Resolve role permissions.
5. Resolve assignment scope against the target.
6. Evaluate relationship-based grants where supported.
7. Apply protected-action constraints.
8. Apply resource-state constraints.
9. Return allow/deny plus explanation metadata.
```

Avoid loading every assignment and traversing trees in application memory for every request. Use indexed queries and closure-table lookups.

## 8. Central services

### OrganizationHierarchyService

```text
Create/move/deactivate units
Maintain closure table
Read ancestors and descendants
Validate cycle-free mutations
```

### OrganizationAssignmentService

```text
Assign users to units
Manage reporting lines
Manage primary assignments
Validate temporal assignments
```

### RoleManagementService

```text
Create/update custom roles
Manage role permissions
Protect system roles
Validate assignability
```

### RoleAssignmentService

```text
Assign/revoke roles
Validate scope
Validate assigning authority
Enforce temporal rules
```

### AuthorizationService

```text
Evaluate permission against a target
Combine RBAC, scope, relationship, and context
Return structured decisions
```

### EffectiveAccessService

```text
List effective permissions
List assignments and scopes
Explain access
Support frontend capability payloads
```

## 9. Privilege-escalation controls

A user assigning authority must:

```text
Possess the role/permissions being assigned
Possess them over an equal or broader scope
Be permitted to assign roles
Be permitted to manage the target user
Not assign a protected role without protected authority
Not create an assignment beyond their own expiration
```

Delegation must additionally enforce:

```text
No broader scope
No longer duration than the delegator's authority
No redelegation unless explicitly allowed
Immediate revocation
```

## 10. Protected-target rules

Define explicit rules for:

```text
Tenant owner
Tenant administrator
Security administrator
Role administrator
System-defined roles
Last active high-authority administrator
Self-modification
Higher-authority target users
```

Do not infer security authority solely from organizational depth.

## 11. Resource relationships

Projects may receive organizational ownership:

```text
Project.owningOrganizationalUnitId
```

Project access may result from:

```text
Tenant-wide permission
Owning-unit permission
Owning-subtree permission
Project-scoped role
Project membership
Project-lead role
```

Task access may result from project access, task assignment, or task-management permission in scope.

Relationship grants and permission grants must have explicit precedence and be explainable.

## 12. Audit actions

Add audit actions such as:

```text
ORG_UNIT_CREATED
ORG_UNIT_UPDATED
ORG_UNIT_MOVED
ORG_UNIT_STATUS_UPDATED
ORG_USER_ASSIGNED
ORG_USER_UNASSIGNED
REPORTING_LINE_UPDATED
ROLE_CREATED
ROLE_UPDATED
ROLE_STATUS_UPDATED
ROLE_PERMISSION_ADDED
ROLE_PERMISSION_REMOVED
ROLE_ASSIGNED
ROLE_ASSIGNMENT_REVOKED
DELEGATION_CREATED
DELEGATION_REVOKED
AUTHORIZATION_ACCESS_REVIEWED
```

Avoid indiscriminately recording every denied request in the ordinary audit table. Sensitive denials can later use a dedicated security event stream.

## 13. Database indexing

Plan indexes for:

```text
organizational_units(tenant_id, parent_unit_id)
organizational_units(tenant_id, status)
organizational_unit_closure(tenant_id, ancestor_id, descendant_id)
organizational_unit_closure(tenant_id, descendant_id, ancestor_id)
user_organization_assignments(tenant_id, user_id, status)
user_organization_assignments(tenant_id, organizational_unit_id, status)
user_organization_assignments(reports_to_assignment_id)
authorization_roles(tenant_id, status)
role_permissions(role_id, permission_id)
user_role_assignments(tenant_id, user_id, revoked_at)
user_role_assignments(tenant_id, role_id, scope_type, scope_id)
```

Validate exact indexes against real queries.

## 14. API phases

Organization phase candidates:

```text
/api/tenants/{tenantId}/organization/units
/api/tenants/{tenantId}/organization/units/{unitId}
/api/tenants/{tenantId}/organization/tree
/api/tenants/{tenantId}/organization/assignments
/api/tenants/{tenantId}/organization/reporting-lines
```

Authorization phase candidates:

```text
/api/tenants/{tenantId}/authorization/permissions
/api/tenants/{tenantId}/authorization/roles
/api/tenants/{tenantId}/authorization/role-assignments
/api/tenants/{tenantId}/authorization/effective-access
```

Freeze final endpoint naming before frontend implementation.

## 15. Frontend phases

### Organization UI

```text
Organization tree
Unit search and details
Unit creation/edit/move/status
User assignment
Reporting-line management
```

### Role UI

```text
Role list and builder
Permission matrix
Protected-role indicators
Role clone and status
```

### Assignment UI

```text
User role assignments
Scope selector
Unit/subtree/project selector
Validity period
Revocation
```

### Effective-access UI

```text
Effective capability list
Role/source
Scope
Expiration
Explanation path
```

## 16. Test matrix

Required backend scenarios:

```text
Same role, different unit scopes
Same role, different subtree scopes
Direct report allowed
Indirect descendant denied under DIRECT_REPORTS
Indirect descendant allowed under ORGANIZATIONAL_SUBTREE
User outside subtree denied
Unit move changes access
Expired assignment denied
Revoked assignment denied
Cross-tenant hierarchy denied
Cross-tenant assignment denied
Reporting cycle rejected
Unit cycle rejected
Lower authority cannot assign broader authority
Project membership still grants intended access
Legacy fixed-role behavior remains valid
```

Required frontend scenarios:

```text
Tree renders arbitrary depth
Restricted unit actions hidden
Scope selector validates targets
Capability-based routes/actions render correctly
Expired/revoked access disappears after refresh
Explain-access output is understandable
```

## 17. Implementation order

```text
1. Permission catalogue design
2. Organizational-unit schema
3. Closure-table maintenance
4. User organizational assignments
5. Reporting relationships
6. Organization APIs
7. Organization frontend
8. Role and role-permission schema
9. Protected-role seeding
10. Scoped role assignments
11. Authorization evaluation engine
12. Compatibility mapping
13. User-management migration
14. Project/task migration
15. Audit-log and invitation migration
16. Delegation
17. Explain access
```

## 18. First backend deliverable

The first implementation slice should include only:

```text
OrganizationalUnit
OrganizationalUnitClosure
Unit create/read/update/move/deactivate
Tree/subtree queries
Cycle prevention
Tenant isolation
Flyway migration
Integration tests
```

Do not include custom roles, delegation, subscriptions, or billing in the first slice.

## 19. Definition of done

```text
Hierarchy depth is arbitrary.
Authority scope is explicit.
Roles contain explicit permissions.
Same-role users can have different scopes.
Direct reports and subtrees behave differently.
Project relationships coexist with organizational authority.
Delegation cannot escalate access.
Every sensitive assignment is audited.
Effective access can be explained.
Cross-tenant access remains impossible.
The fixed-role MVP regression suite still passes.
```
