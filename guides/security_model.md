# Multi-Tenant SaaS Backend — Security Model

This guide describes the current authentication, authorization, tenant isolation, project access, session security, and audit model.

## 1. Account types

The platform has two separate account types:

```text
Tenant users
System admins
```

Tenant users are stored in:

```text
APP_USERS
```

System admins are stored in:

```text
SYSTEM_ADMINS
```

They are intentionally separate because tenant users belong to one tenant while system admins operate at platform scope.

## 2. Tenant-user roles

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

### TENANT_ADMIN

Can manage their own tenant, users, invitations, projects, project memberships, and tasks.

Main abilities:

```text
Read/update own tenant
Change own tenant status under safety rules
Create and manage tenant users
Invite users
Read tenant audit logs
Read tenant dashboard
Create and manage projects
Manage project members
Manage project tasks
Unlock tenant users
```

Safety restrictions:

```text
Cannot change their own tenant role
Cannot suspend/deactivate their own user account
Cannot remove the last active tenant admin
```

### TENANT_MANAGER

Operational management role.

Main abilities:

```text
Read tenant users
Read tenant dashboard
Create/update/archive projects
Manage project members
Create/update/assign/cancel tasks
Read project and task data
```

A tenant manager does not receive tenant-admin user-management authority.

### TENANT_USER

Basic tenant user.

Main abilities:

```text
Authenticate
Read same-tenant projects
Read project-member lists
Read tasks only for projects where they are a member
Update status only for tasks assigned to them
Receive PROJECT_LEAD authority when promoted inside a project
```

## 3. Project-level roles

```text
PROJECT_LEAD
MEMBER
```

These roles are separate from tenant roles.

A TENANT_USER can be a PROJECT_LEAD for one project without becoming TENANT_MANAGER.

### PROJECT_LEAD

For that project:

```text
Read tasks
Create tasks
Update tasks
Assign/unassign tasks
Update task statuses
Cancel tasks
```

### MEMBER

For that project:

```text
Read tasks
Update status of a task only when assigned to that member
```

The project creator is automatically assigned as PROJECT_LEAD.

At least one PROJECT_LEAD must always remain.

## 4. System admin

System-admin authority:

```text
SYSTEM_ADMIN
```

System admins are DB-backed accounts, not tenant users.

Current abilities:

```text
Login and read current system-admin identity
Change own password
Read platform dashboard
List/read/update tenants
Create a tenant through system onboarding
Read and manage tenant users
Read tenant audit logs
Create/list/read/status-update/unlock system admins
Read platform audit logs
```

Safety restrictions:

```text
Cannot suspend/deactivate themselves
At least one active system admin must remain
Only active system admins pass authorization
```

System-admin refresh tokens are not implemented.

## 5. Authentication flows

### Tenant login

```text
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
```

Checks:

```text
Tenant exists
Tenant is ACTIVE
User exists inside that tenant
User is ACTIVE
Account is not currently locked
Password is correct
```

Returns:

```text
accessToken
refreshToken
```

### System-admin login

```text
POST http://localhost:8081/api/system/auth/login
```

Checks:

```text
System admin exists
System admin is ACTIVE
Account is not currently locked
Password is correct
```

Returns:

```text
accessToken
```

## 6. JWT claims

Tenant-user JWT:

```text
subject = userId
tenantId
email
fullName
role
```

System-admin JWT:

```text
subject = systemAdminId
email
fullName
role = SYSTEM_ADMIN
accountType = SYSTEM_ADMIN
```

## 7. DB-backed authorization

JWT identity is not accepted blindly.

Tenant authorization rechecks:

```text
JWT tenantId
JWT subject/userId
Tenant exists
Tenant is ACTIVE
User exists in that tenant
User is ACTIVE
Current database role
Requested tenant matches the token tenant
```

System-admin authorization rechecks:

```text
JWT role
JWT accountType
System-admin database record
System-admin ACTIVE status
```

Project/task authorization additionally rechecks:

```text
Project belongs to requested tenant
Current tenant user exists and is active
Project membership when required
Project role when required
Task assignee when status-only authority is used
```

This means an old access token stops passing protected operations after a live role/status change even though the JWT itself has not expired.

## 8. Tenant isolation

Tenant isolation is enforced through several layers:

```text
Tenant ID in route
Tenant ID in JWT
Live user lookup
Live tenant lookup
Method-security checks
Tenant-scoped repository methods
Cross-tenant integration tests
```

Important repository patterns:

```text
findByTenantIdAndId(tenantId, userId)
findByTenant_IdAndId(tenantId, projectId)
findByProject_Tenant_IdAndProject_IdAndId(tenantId, projectId, taskId)
```

Business records must never be fetched by an unscoped ID and returned before ownership is verified.

## 9. Project access model

| Operation | SYSTEM_ADMIN | TENANT_ADMIN | TENANT_MANAGER | TENANT_USER |
|---|---:|---:|---:|---:|
| List/read same-tenant projects | No by project API | Yes | Yes | Yes |
| Create project | No | Yes | Yes | No |
| Update/status/archive project | No | Yes | Yes | No |
| Read project members | No | Yes | Yes | Yes |
| Add/change/remove project members | No | Yes | Yes | No |

System administrators manage tenants through platform/tenant management APIs, not through tenant business APIs.

## 10. Task access model

| Operation | Tenant admin/manager | Project lead | Assigned member | Other project member | Non-member tenant user |
|---|---:|---:|---:|---:|---:|
| List/read tasks | Yes | Yes | Yes | Yes | No |
| Create task | Yes | Yes | No | No | No |
| Update task | Yes | Yes | No | No | No |
| Assign task | Yes | Yes | No | No | No |
| Cancel task | Yes | Yes | No | No | No |
| Update task status | Yes | Yes | Yes | No | No |

Task assignees must be active members of the project.

## 11. Refresh-token security

Tenant refresh tokens:

```text
Are generated using secure randomness
Are stored only as SHA-256 hashes
Rotate on refresh
Become invalid after rotation
Can be revoked individually
Can be revoked for all user sessions
```

Automatic revocation:

```text
Password change
Password reset
User role change
User suspension/deactivation
Tenant suspension/deactivation
```

Logout note:

```text
Logout revokes refresh tokens.
Already-issued access JWTs remain cryptographically valid until expiry.
Live DB authorization may still block them immediately after role/status changes.
```

## 12. Password security

Passwords are stored using Spring Security password encoding.

New passwords use the shared strong-password validator:

```text
8–100 characters
Uppercase
Lowercase
Number
Special character
No spaces
```

Raw passwords are never stored.

## 13. Login lockout

Tenant users and system admins track:

```text
failedLoginAttempts
lockedUntil
```

Current default policy:

```text
5 failed attempts
15-minute lock
```

Administrators can unlock accounts through authorized endpoints.

## 14. Invitation-token security

Invitation tokens:

```text
Are generated securely
Are stored only as hashes
Expire
Can be revoked
Are one-time use
Create the user only after acceptance
```

A replacement pending invitation revokes the previous pending invitation for the same tenant/email.

The local development response may expose:

```text
devInvitationToken
```

Production must send the raw token through email and omit it from ordinary API responses.

## 15. Password-reset-token security

Password-reset tokens:

```text
Are generated securely
Are stored only as hashes
Expire
Are one-time use
Revoke active refresh tokens after password reset
```

The local development response may expose a development reset token.

## 16. Audit model

Tenant audit logs contain:

```text
tenant
actorType
actorUser or actorSystemAdmin
targetUser
action
success
message
createdAt
```

Actor types:

```text
TENANT_USER
SYSTEM_ADMIN
SYSTEM
```

Platform audit logs are separate and track system-admin management.

Business audit actions being completed:

```text
PROJECT_CREATED
PROJECT_UPDATED
PROJECT_STATUS_UPDATED
PROJECT_ARCHIVED
PROJECT_MEMBER_ADDED
PROJECT_MEMBER_ROLE_UPDATED
PROJECT_MEMBER_REMOVED
TASK_CREATED
TASK_UPDATED
TASK_STATUS_UPDATED
TASK_ASSIGNEE_UPDATED
TASK_CANCELLED
```

The audit action database column is being converted from an H2 enum to `VARCHAR(60)` through Flyway V7 so future action additions do not require H2 enum reconstruction.

## 17. Public endpoints

Public development/support endpoints:

```text
GET  http://localhost:8081/api/health
GET  http://localhost:8081/actuator/health
GET  http://localhost:8081/h2-console
GET  http://localhost:8081/swagger-ui.html
GET  http://localhost:8081/v3/api-docs
```

Public authentication/onboarding endpoints:

```text
POST http://localhost:8081/api/onboarding/tenants
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
POST http://localhost:8081/api/tenants/{tenantId}/auth/forgot-password
POST http://localhost:8081/api/system/auth/login
POST http://localhost:8081/api/auth/refresh
POST http://localhost:8081/api/auth/logout
POST http://localhost:8081/api/auth/reset-password
POST http://localhost:8081/api/user-invitations/accept
```

Everything else under `/api/**` is authenticated unless explicitly configured otherwise.

## 18. CORS and secrets

CORS is configured centrally.

Allowed origins are environment/profile controlled.

Secrets are externalized:

```text
JWT secret
System-admin bootstrap password
Environment-specific datasource values
```

Do not commit local secrets.

## 19. Standard security errors

```text
401 AUTHENTICATION_REQUIRED
401 AUTHENTICATION_FAILED
403 ACCESS_DENIED
```

These use the same structured API error body as controller/service errors.

## 20. Security tests

Integration coverage includes:

```text
Missing/invalid authentication
Cross-tenant user access
Cross-tenant invitation access
Cross-tenant project access
Cross-tenant project membership access
Cross-tenant task access
Tenant-role restrictions
Project-role restrictions
Assigned-member status authority
Refresh-token rotation and revocation
Invitation one-time use
Archived/cancelled immutability
```

Current suite size before adding audit-specific tests:

```text
43 tests
```
