# Multi-Tenant SaaS Backend — Security Model

This guide explains how authentication, authorization, roles, and tenant isolation currently work.

---

## 1. Account Types

The project now has two account types:

```text
Tenant users
System admins
```

They are intentionally separate.

Tenant users are stored in:

```text
APP_USERS
```

System admins are stored in:

```text
SYSTEM_ADMINS
```

Reason:

```text
Tenant users belong to one tenant.
System admins manage the platform and should not belong to any tenant.
```

---

## 2. Tenant User Roles

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

### TENANT_ADMIN

Can manage their own tenant and users.

Current abilities:

```text
Read own tenant
Update own tenant
Update own tenant status
Deactivate own tenant
Create tenant users
List tenant users
Update tenant users
Update user role
Update user status
Deactivate users
Read audit logs
Read tenant dashboard
```

### TENANT_MANAGER

Can read tenant-level operational data.

Current abilities:

```text
List users inside own tenant
Read user details inside own tenant
Read tenant dashboard
```

### TENANT_USER

Basic authenticated tenant user.

Current abilities are intentionally limited.

---

## 3. System Admin

System admin role:

```text
SYSTEM_ADMIN
```

Current abilities:

```text
Login through system auth
Read system dashboard
List all tenants
Read any tenant by ID or slug
Read users inside any tenant
```

Current restrictions:

```text
Cannot create tenant users yet
Cannot update tenant users yet
Cannot deactivate tenant users yet
No refresh token yet
No system-admin audit log yet
```

This is intentional. System-admin writes should be added with proper system-admin actor tracking.

---

## 4. Authentication Flows

### Tenant user login

```text
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
```

Checks:

```text
Tenant exists
Tenant is ACTIVE
User exists inside tenant
User is ACTIVE
Password is valid
```

Returns:

```text
accessToken
refreshToken
```

### System admin login

```text
POST http://localhost:8081/api/system/auth/login
```

Checks:

```text
System admin exists
System admin is ACTIVE
Password is valid
```

Returns:

```text
accessToken
```

---

## 5. JWT Claims

Tenant user JWT includes:

```text
subject = userId
tenantId
email
fullName
role
```

System admin JWT includes:

```text
subject = systemAdminId
email
fullName
role = SYSTEM_ADMIN
accountType = SYSTEM_ADMIN
```

---

## 6. DB-Backed Authorization

The project does not rely only on old JWT claims.

For tenant authorization, the app checks live database state:

```text
Tenant exists
Tenant is ACTIVE
User exists
User is ACTIVE
User belongs to requested tenant
User's current DB role is allowed
```

For system-admin authorization, the app checks:

```text
JWT role is SYSTEM_ADMIN
JWT accountType is SYSTEM_ADMIN
System admin exists in DB
System admin status is ACTIVE
```

Why this matters:

```text
If a user is suspended, deactivated, or downgraded,
old access tokens stop passing authorization checks.
```

---

## 7. Refresh Token Security

Tenant refresh-token behavior:

```text
Refresh tokens are random secure tokens.
Only token hashes are stored.
Refresh tokens rotate on refresh.
Old refresh tokens are revoked.
Logout revokes one refresh token.
Logout-all revokes all active tokens for current user.
```

Automatic revocation rules:

```text
Password change revokes active refresh tokens.
Password reset revokes active refresh tokens.
User role change revokes that user's refresh tokens.
User suspension/deactivation revokes that user's refresh tokens.
Tenant suspension/deactivation revokes all tenant refresh tokens.
```

System-admin refresh tokens are not implemented yet.

---

## 8. Tenant Isolation Rules

Tenant isolation means one tenant must not access another tenant's data.

Examples:

```text
Tenant A admin can read Tenant A users.
Tenant A admin cannot read Tenant B users.
Tenant B manager cannot access Tenant A dashboard.
```

This is enforced by:

```text
Tenant ID in the URL
Tenant ID in JWT
DB lookup of the current user
DB role check
Tenant-scoped repository methods
```

Important repository pattern:

```text
findByTenantIdAndId(tenantId, userId)
```

This prevents fetching a user only by user ID without checking tenant ownership.

---

## 9. Tenant Admin Safety Guard

The tenant admin guard prevents accidental tenant lockout.

Rules:

```text
Admin cannot change their own role.
Admin cannot suspend/deactivate their own account.
The last active TENANT_ADMIN cannot be removed.
```

This protects the tenant from having no active admin.

---

## 10. Audit Model

Audit logs use:

```text
actorUser
targetUser
tenant
action
success
message
createdAt
```

Simple meaning:

```text
actorUser  = who did the action
targetUser = who was affected by the action
tenant     = tenant where the action happened
```

For tenant-level actions, `targetUser` can be null.

Example:

```text
Tenant admin suspends a manager

actorUser  = admin@tenant.com
targetUser = manager@tenant.com
action     = USER_STATUS_UPDATED
success    = true
```

---

## 11. Public vs Protected APIs

Public development/support APIs:

```text
GET  http://localhost:8081/api/health
GET  http://localhost:8081/actuator/health
GET  http://localhost:8081/h2-console
GET  http://localhost:8081/swagger-ui.html
GET  http://localhost:8081/v3/api-docs
```

Public auth APIs:

```text
POST http://localhost:8081/api/onboarding/tenants
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
POST http://localhost:8081/api/tenants/{tenantId}/auth/forgot-password
POST http://localhost:8081/api/auth/reset-password
POST http://localhost:8081/api/auth/refresh
POST http://localhost:8081/api/auth/logout
POST http://localhost:8081/api/system/auth/login
```

Everything under `/api/**` is protected unless explicitly made public.

---

## 12. Current Access Matrix

| API Area | SYSTEM_ADMIN | TENANT_ADMIN | TENANT_MANAGER | TENANT_USER |
|---|---:|---:|---:|---:|
| System dashboard | Yes | No | No | No |
| List all tenants | Yes | No | No | No |
| Read any tenant | Yes | No | No | No |
| Read own tenant | No | Yes | Yes/limited | Yes/limited |
| Read tenant users | Yes, any tenant | Yes, own tenant | Yes, own tenant | No |
| Create tenant users | No | Yes | No | No |
| Update tenant users | No | Yes | No | No |
| Audit logs | No | Yes, own tenant | No | No |
| Tenant dashboard | No | Yes | Yes | No |

Note: system-admin write access can be added later with a separate system-admin audit model.
