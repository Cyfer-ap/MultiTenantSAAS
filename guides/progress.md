# Multi-Tenant SaaS Backend — Progress Notes

This guide explains the current state of the project in simple language. It is meant as a learning reference, not just a changelog.

---

## 1. Project Goal

The goal is to build a production-style SaaS backend step by step.

The project currently focuses on:

```text
Tenant management
Tenant isolation
Tenant-scoped users
JWT authentication
Refresh-token based sessions
Role-based authorization
System-admin platform access
Audit logging
Password reset
Clean REST API structure
```

The backend runs locally at:

```text
http://localhost:8081
```

---

## 2. Current Tech Stack

```text
Spring Boot 4
Java 21
Maven
Spring Web MVC
Spring Data JPA
Hibernate
H2 file database
Spring Security
OAuth2 Resource Server / JWT
Springdoc OpenAPI / Swagger UI
Validation
Actuator
```

---

## 3. Main Package Structure

```text
com.chacha.multitenantsaas
 ├── common
 ├── config
 ├── controller
 ├── dto
 ├── entity
 ├── exception
 ├── repository
 ├── security
 ├── service
 ├── validation
 └── MultitenantSaasApplication.java
```

Simple meaning:

```text
controller  -> receives HTTP requests
service     -> contains business logic
repository  -> talks to the database
entity      -> database table models
dto         -> request/response objects
security    -> authorization helpers and JWT-related code
config      -> Spring/security/OpenAPI configuration
exception   -> global error handling
common      -> shared response and utility classes
validation  -> reusable custom validation annotations
```

---

## 4. Local Development Setup

The server port is:

```text
8081
```

Local database:

```text
H2 file-based database
jdbc:h2:file:./data/multitenant_saas_db
```

H2 Console:

```text
http://localhost:8081/h2-console
```

Swagger UI:

```text
http://localhost:8081/swagger-ui.html
```

---

## 5. Common API Response

Most custom APIs use the same response wrapper:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "..."
}
```

This is handled by:

```text
common/ApiResponse.java
```

For paginated APIs, the project uses:

```text
common/PageResponse.java
```

This keeps list APIs consistent.

---

## 6. Tenant Module

A tenant represents one customer, company, organization, or workspace.

Example:

```text
Tenant A -> Acme Corporation
Tenant B -> Nova Labs
```

Main tenant fields:

```text
id
name
slug
status
createdAt
updatedAt
```

Tenant statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Important tenant features already implemented:

```text
Tenant onboarding
Tenant lookup by ID
Tenant lookup by slug
Tenant update
Tenant status update
Tenant soft delete / deactivation
Tenant listing for system admin
Tenant search/filter/sort/pagination
```

Current tenant creation flow:

```text
Use POST http://localhost:8081/api/onboarding/tenants
```

The old direct tenant creation endpoint is disabled:

```text
POST http://localhost:8081/api/tenants
```

This was disabled because a real SaaS tenant should be created with its first tenant admin user.

---

## 7. Tenant Onboarding

Tenant onboarding creates two things together:

```text
1. Tenant
2. First TENANT_ADMIN user
```

Endpoint:

```text
POST http://localhost:8081/api/onboarding/tenants
```

Why this matters:

```text
A tenant without an admin is not useful.
An admin without a tenant breaks tenant isolation.
```

The onboarding endpoint also writes an audit log:

```text
TENANT_ONBOARDED
```

---

## 8. Tenant Users

Users are stored in:

```text
APP_USERS
```

Each tenant user belongs to exactly one tenant.

Current tenant-user roles:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Current user statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Important user features already implemented:

```text
Create user inside tenant
List tenant users
Get tenant user by ID
Update user details
Update user role
Update user status
Deactivate user
Tenant-scoped email uniqueness
Email normalization
Strong password validation
Soft delete by status
```

Email uniqueness is tenant-scoped:

```text
Same email cannot repeat inside the same tenant.
The same email may exist in different tenants.
```

---

## 9. Strong Password Validation

A reusable annotation was added:

```text
@StrongPassword
```

It is used for:

```text
Tenant onboarding admin password
Tenant user creation password
Change password new password
Reset password new password
```

Rule:

```text
8 to 100 characters
at least one uppercase letter
at least one lowercase letter
at least one number
at least one special character
no spaces
```

This avoids repeating the same password validation rules in multiple DTOs.

---

## 10. Tenant Authentication

Tenant users login through:

```text
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
```

The login process checks:

```text
Tenant exists
Tenant is ACTIVE
User exists inside that tenant
User is ACTIVE
Password is correct
```

Login returns:

```text
accessToken
refreshToken
```

Access tokens are JWTs. Refresh tokens are stored hashed in the database.

---

## 11. Refresh Tokens and Logout

Implemented endpoints:

```text
POST http://localhost:8081/api/auth/refresh
POST http://localhost:8081/api/auth/logout
POST http://localhost:8081/api/auth/logout-all
```

Important behavior:

```text
Refresh tokens are stored as SHA-256 hashes.
Raw refresh tokens are never stored.
Refresh-token rotation is implemented.
Logout revokes one refresh token.
Logout-all revokes all active refresh tokens for the current tenant user.
```

Security improvements implemented:

```text
User role change revokes that user's refresh tokens.
User suspension/deactivation revokes that user's refresh tokens.
Tenant suspension/deactivation revokes all refresh tokens under that tenant.
```

---

## 12. Password Change and Reset

Implemented endpoints:

```text
POST http://localhost:8081/api/auth/change-password
POST http://localhost:8081/api/tenants/{tenantId}/auth/forgot-password
POST http://localhost:8081/api/auth/reset-password
```

Behavior:

```text
Change password requires current password.
New password must match confirm password.
New password must be different from old password.
Password reset token is stored as a hash.
Password reset token expires after configured time.
Password reset revokes active refresh tokens.
```

In local development, the forgot-password response returns a development reset token. Later this should be sent by email instead.

---

## 13. Authorization and Tenant Isolation

The project now protects `/api/**` by default.

Public endpoints are only selected health/auth/development endpoints.

Tenant authorization is DB-backed. This means the app does not blindly trust old JWT role/status claims.

For tenant APIs, the app checks:

```text
JWT tenantId
JWT userId
Tenant exists
Tenant is ACTIVE
User exists in that tenant
User is ACTIVE
Current DB role is allowed
```

Why this matters:

```text
If a user's role is changed or the user is suspended,
old access tokens stop passing authorization checks.
```

---

## 14. Tenant Admin Safety Rules

A reusable guard service prevents tenant lockout.

Implemented rules:

```text
A tenant admin cannot change their own role.
A tenant admin cannot deactivate or suspend their own account.
The last active TENANT_ADMIN in a tenant cannot be removed.
```

This protects tenants from accidentally losing all admin access.

---

## 15. System Admin Module

System admins are separate from tenant users.

System admins are stored in:

```text
SYSTEM_ADMINS
```

They are not stored in `APP_USERS`, because `APP_USERS` always belong to a tenant.

Default local system admin:

```text
Email: system@saas.local
Password: SystemAdmin@123
```

System admin login:

```text
POST http://localhost:8081/api/system/auth/login
```

Current system-admin behavior:

```text
Can login and receive access token
Can access system dashboard
Can list all tenants
Can read tenant details
Can read tenant users
Cannot create/update tenant users yet
```

System admin currently has access token only. Refresh-token support for system admins can be added later as a separate design.

---

## 16. Dashboards

System dashboard:

```text
GET http://localhost:8081/api/dashboard/summary
```

Access:

```text
SYSTEM_ADMIN only
```

Tenant dashboard:

```text
GET http://localhost:8081/api/tenant/dashboard/summary
```

Access:

```text
TENANT_ADMIN
TENANT_MANAGER
```

---

## 17. Audit Logging

Audit logs track important security and management actions.

The project uses this design:

```text
actorUser  = who performed the action
targetUser = who was affected by the action
tenant     = tenant context
```

Examples:

```text
Tenant admin creates manager:
actorUser  = admin@tenant.com
targetUser = manager@tenant.com

Tenant admin changes user role:
actorUser  = admin@tenant.com
targetUser = user being changed

Tenant-level action:
actorUser  = tenant admin
targetUser = null
tenant     = affected tenant
```

Current audit actions include:

```text
TENANT_ONBOARDED
TENANT_UPDATED
TENANT_STATUS_UPDATED
TENANT_DEACTIVATED
USER_CREATED
USER_UPDATED
USER_ROLE_UPDATED
USER_STATUS_UPDATED
USER_DEACTIVATED
LOGIN_SUCCESS
LOGIN_FAILED
LOGOUT
LOGOUT_ALL
TOKEN_REFRESH
PASSWORD_CHANGED
PASSWORD_RESET_REQUESTED
PASSWORD_RESET_COMPLETED
```

Audit APIs:

```text
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs/users/{userId}
```

Access:

```text
TENANT_ADMIN only
```

---

## 18. Swagger/OpenAPI

Swagger UI is available at:

```text
http://localhost:8081/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8081/v3/api-docs
```

Swagger is useful for viewing available endpoints, but Postman is currently the main testing tool.

---

## 19. Current Access Model

```text
SYSTEM_ADMIN
- Platform-wide read access
- System dashboard
- Tenant listing
- Tenant detail reading
- Tenant user reading

TENANT_ADMIN
- Own tenant management
- Own tenant users management
- Audit logs
- Tenant dashboard

TENANT_MANAGER
- Own tenant user reading
- Tenant dashboard

TENANT_USER
- Basic authenticated user
```

---

## 20. Important Design Decisions

```text
Tenant creation uses onboarding instead of public direct creation.
System admins are separate from tenant users.
Refresh tokens are stored hashed.
Authorization checks live database state.
Soft delete uses status instead of physical delete.
Audit logs use actor/target model.
Strong password rules are reusable.
```

---

## 21. Current Limitations

Still not implemented:

```text
System-admin refresh tokens
System-admin CRUD management
System-admin audit logs
Plan/subscription module
Tenant invitation flow
Email sending for password reset
Flyway migrations
PostgreSQL migration
Automated unit/integration tests
```

---

## 22. Recommended Next Steps

Suggested next implementation order:

```text
1. System-admin audit logging
2. System-admin refresh tokens or short-session strategy
3. Plan and subscription entities
4. Subscription enforcement on tenant/user limits
5. Flyway migration setup
6. PostgreSQL migration
7. Automated tests
```
