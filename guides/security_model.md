# Multi-Tenant SaaS Platform — Security Model

This document describes the currently implemented authentication, session, authorization, tenant-isolation, and audit model.

```text
Repository baseline reviewed:
ae1fa4cbb5133ae0b3bcd2596379e1ab64f36be1
```

## 1. Security boundaries

The platform separates:

```text
Platform scope
Tenant scope
Project scope
Task/assignee relationships
```

Two account families exist:

```text
Tenant users
System administrators
```

Tenant users are stored in `APP_USERS`; system administrators are stored separately in `SYSTEM_ADMINS`.

## 2. Current roles

Tenant roles:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Project roles:

```text
PROJECT_LEAD
MEMBER
```

Project roles are independent from tenant roles. A `TENANT_USER` may be a `PROJECT_LEAD` without becoming a tenant manager.

### Tenant administrator

Current authority includes:

```text
Tenant management under safety constraints
Tenant-user management
Invitations
Tenant dashboard
Tenant audit logs
Project, membership, and task management
Account and session security
```

Safety constraints:

```text
Cannot remove the last active tenant admin
Cannot change own role through the ordinary administrative flow
Cannot suspend/deactivate own account through the ordinary flow
```

### Tenant manager

Current authority includes:

```text
Tenant dashboard
Tenant-user operational access
Project, membership, and task management
Account and session security
```

Tenant managers do not receive tenant-admin invitation or audit-log authority.

### Tenant user

Current authority includes:

```text
Authentication
Projects and project details
Project-member visibility
Task visibility when project membership permits
Assigned-task status updates
Project-lead authority where assigned
Account and session security
```

## 3. System administrators

System-admin authority:

```text
SYSTEM_ADMIN
```

Implemented authority includes:

```text
Platform login and current-admin lookup
Own password change
Platform dashboard
Tenant administration and onboarding
Tenant-user administration
System-admin administration
Tenant audit-log access
Platform audit logs
```

Safety constraints:

```text
A system admin cannot deactivate themselves.
At least one active system admin must remain.
Authorization rechecks the active database record.
```

System-admin refresh tokens are not currently implemented.

## 4. Tenant authentication

Tenant login:

```text
POST /api/tenants/{tenantId}/auth/login
```

Checks:

```text
Tenant exists and is ACTIVE
User exists in that tenant and is ACTIVE
Account is not locked
Password hash matches
```

Successful login returns an access JWT, a refresh token, identity data, role data, and expiration metadata.

## 5. System-admin authentication

System-admin login:

```text
POST /api/system/auth/login
```

Checks:

```text
System admin exists and is ACTIVE
Account is not locked
Password hash matches
```

Successful login returns an access JWT.

## 6. JWT claims

Tenant JWT claims include:

```text
sub = userId
tenantId
email
fullName
role
sessionVersion
iss
iat
exp
```

System-admin JWT claims include:

```text
sub = systemAdminId
email
fullName
role = SYSTEM_ADMIN
accountType = SYSTEM_ADMIN
iss
iat
exp
```

## 7. JWT validation

Cryptographic validation uses HMAC SHA-256 with a secret of at least 32 bytes.

Tenant tokens receive an additional database-backed validation step through:

```text
TenantSessionJwtValidator
```

The validator:

```text
Parses tenantId and userId
Reads the token sessionVersion
Loads the user within the token tenant
Compares the token version with APP_USERS.session_version
Rejects missing users and revoked session versions
```

Tokens created before migration V8 are interpreted as version zero so existing sessions remain compatible until a revocation increments the stored version.

System-admin tokens bypass tenant session-version validation.

## 8. Tenant session lifecycle

### Refresh

```text
POST /api/auth/refresh
```

Refresh tokens:

```text
Use secure randomness
Are stored only as SHA-256 hashes
Rotate after successful refresh
Cannot be reused after rotation
Expire according to configuration
```

### Single-session logout

```text
POST /api/auth/logout
```

This revokes the submitted refresh token. It does not increment the global user session version, so the related access JWT may remain valid until expiry unless another live authorization rule rejects it.

### Logout all devices

```text
POST /api/auth/logout-all
```

This operation:

```text
Increments APP_USERS.session_version
Revokes every active refresh token for the user
Audits the event
```

All previously issued tenant access tokens become invalid on their next protected request.

### Password change and reset

Password change and password reset:

```text
Replace the password hash
Increment session_version
Revoke all active refresh tokens
Audit the event
```

Every previously issued tenant access and refresh token is invalidated.

## 9. Frontend session behavior

The tenant frontend:

```text
Stores the tenant session in localStorage
Restores it through GET /api/auth/me
Adds the access token to protected requests
Refreshes after an access-token 401
Deduplicates concurrent refresh attempts
Clears the session if refresh returns 401 or 403
Clears tenant query data after sign-out
Preserves the stored session during temporary network/backend failures
```

After logout-all from another browser:

```text
The old access JWT receives 401.
The refresh token also receives 401.
The frontend clears local authentication.
The user returns to sign-in.
```

System-admin sessions use separate storage and API clients.

## 10. DB-backed authorization

JWT role claims are not the sole source of truth.

Tenant authorization rechecks:

```text
Token tenant ID and user ID
Tenant database record and ACTIVE status
User database record and ACTIVE status
Current database role
Requested tenant ownership
```

Project/task checks additionally evaluate:

```text
Project tenant ownership
Project membership
Project role
Task assignee relationship
Resource status
```

System-admin authorization rechecks:

```text
SYSTEM_ADMIN claim
accountType claim
System-admin database record
ACTIVE status
```

## 11. Tenant isolation

Tenant isolation is enforced through:

```text
Tenant IDs in routes
Tenant IDs in JWTs
Tenant-scoped repository methods
Live tenant/user lookup
Method security
Service ownership checks
Cross-tenant integration tests
```

Required pattern:

```text
Load a business record using tenant identity and record identity together.
```

Unsafe pattern:

```text
Load by an unscoped record ID, expose data, and verify ownership later.
```

## 12. Project access matrix

| Operation | Tenant admin | Tenant manager | Tenant user |
|---|---:|---:|---:|
| List/read tenant projects | Yes | Yes | Yes |
| Create project | Yes | Yes | No |
| Update/status/archive project | Yes | Yes | No |
| Read project members | Yes | Yes | Yes |
| Add/change/remove members | Yes | Yes | No |

## 13. Task access matrix

| Operation | Tenant admin/manager | Project lead | Assigned member | Other member | Non-member |
|---|---:|---:|---:|---:|---:|
| List/read tasks | Yes | Yes | Yes | Yes | No |
| Create task | Yes | Yes | No | No | No |
| Update task | Yes | Yes | No | No | No |
| Assign task | Yes | Yes | No | No | No |
| Cancel task | Yes | Yes | No | No | No |
| Update task status | Yes | Yes | Yes | No | No |

Assignees must be active members of the same project.

## 14. Password and token security

Strong-password policy:

```text
8–100 characters
Uppercase and lowercase
Number
Special character
No spaces
```

Passwords are stored only as encoded hashes.

Invitation and password-reset tokens:

```text
Use secure random raw values
Are stored only as hashes
Expire
Are one-time use
Can be revoked or consumed
```

Development profiles may expose raw tokens for testing. Production delivery must use an external communication channel.

## 15. Account lockout

Tenant users and system admins track:

```text
failedLoginAttempts
lockedUntil
```

Default policy:

```text
5 failed attempts
15-minute lock
```

Authorized administrators can unlock accounts.

## 16. Security error contract

Security failures use the shared JSON error contract.

Primary error codes:

```text
401 AUTHENTICATION_REQUIRED
401 AUTHENTICATION_FAILED
403 ACCESS_DENIED
```

The OAuth2 resource-server authentication entry point and access-denied handler are explicitly configured so invalid bearer tokens return the same contract as service/controller failures.

## 17. Audit model

Tenant audit records contain:

```text
Tenant
Actor type
Tenant-user actor or system-admin actor
Target user
Action
Success
Message
Created timestamp
```

Platform audit records are stored separately.

Sensitive events include login/session, password, tenant, user, invitation, project, membership, task, and system-admin operations.

## 18. Current verification

The backend suite covers:

```text
Missing/invalid authentication
Stable 401/403 JSON responses
Tenant isolation and cross-tenant denial
Refresh-token rotation and reuse denial
Single logout
Logout-all access/refresh invalidation
Password-change access/refresh invalidation
Invitation one-time use
Project-role restrictions
Task-assignee restrictions
Archived/cancelled immutability
```

Current verified suite:

```text
47 tests
```

## 19. Authorization-v2 direction

The next server-side authorization model will add:

```text
Permission catalogue
Protected and tenant-defined roles
Organizational units
Arbitrary organizational depth
Reporting relationships
Scoped role assignments
Direct-report scope
Organizational-subtree scope
Project scope
Temporary delegation
Effective-access explanation
```

Planned decision model:

```text
same tenant
AND required permission exists
AND assignment scope contains the target
AND required relationship is satisfied
AND contextual safety constraints pass
```

See `guides/authorization_v2_plan.md`.
