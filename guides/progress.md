# Multi-Tenant SaaS Platform — Current Progress

> Historical snapshot: this file predates authorization v2, subscriptions,
> central subscription read-only enforcement, and PostgreSQL Step 39.
> It is no longer authoritative. Use `guides/README.md` and the focused
> current guides listed there for the current implementation state.

```text
Repository: Cyfer-ap/MultiTenantSAAS
Baseline commit reviewed: ae1fa4cbb5133ae0b3bcd2596379e1ab64f36be1
Snapshot date: 2026-08-02
Current stage: Fixed-role full-stack MVP complete
Next stage: Hierarchical, scoped authorization
```

## 1. Repository layout

```text
MultiTenantSAAS/
├── guides/
├── multitenant-saas/
│   ├── pom.xml
│   └── src/
├── multitenant-saas-frontend/
│   ├── package.json
│   └── src/
└── readme.md
```

## 2. Backend stack

```text
Java 21
Spring Boot 4.0.6
Maven
Spring Web MVC
Spring Data JPA
Hibernate
Spring Security
OAuth2 Resource Server / JWT
Flyway
H2 file database for local development
Jakarta Validation
Actuator
Springdoc OpenAPI
Jackson 3
JUnit 5
MockMvc
```

Jackson 3 is used:

```java
tools.jackson.databind.json.JsonMapper
```

Do not introduce Jackson 2 imports such as:

```java
com.fasterxml.jackson.databind.ObjectMapper
```

## 3. Shared backend infrastructure

Implemented:

```text
Stable success response wrapper
Stable API error contract
Machine-readable error codes
Global exception handling
Pagination utilities
Sorting validation
Central CORS configuration
Local and test profiles
Externalized secrets
Structured security-level 401/403 responses
OpenAPI/Swagger
Actuator health
Flyway validation
```

Standard error shape:

```json
{
  "success": false,
  "message": "Readable error message",
  "errorCode": "ACCESS_DENIED",
  "status": 403,
  "path": "/api/example",
  "details": null,
  "timestamp": "..."
}
```

## 4. Tenant and user model

Current tenancy strategy:

```text
Shared application
Shared database
Shared schema
Tenant foreign keys
Tenant-aware security checks
Tenant-scoped repository methods
Cross-tenant integration tests
```

Tenant statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Current tenant roles:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

User statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Implemented:

```text
Public and system-admin tenant onboarding
Tenant listing, search, filtering, sorting, and pagination
Tenant update and status management
Tenant-scoped user CRUD/status/role operations
Strong password validation
Account lockout and unlock
Self-management protections
Last-active-admin protection
Tenant-scoped email uniqueness and normalization
```

The fixed tenant roles remain active until authorization v2 is introduced incrementally.

## 5. Tenant authentication and sessions

Implemented endpoints:

```text
POST /api/tenants/{tenantId}/auth/login
GET  /api/auth/me
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/logout-all
POST /api/auth/change-password
POST /api/tenants/{tenantId}/auth/forgot-password
POST /api/auth/reset-password
```

Implemented behavior:

```text
JWT access tokens
Hashed refresh-token storage
Refresh-token rotation
Single refresh-token revocation
All-refresh-token revocation
Hashed one-time reset tokens
Strong password validation
Login lockout
Audit events
```

### Immediate multi-device invalidation

Tenant users now have:

```text
sessionVersion
```

The current value is included in tenant JWTs and validated by `TenantSessionJwtValidator`.

The value is incremented after:

```text
Logout all devices
Password change
Password reset
```

Consequences:

```text
All previous tenant refresh tokens are revoked.
All previous tenant access tokens fail validation.
Other browsers return to sign-in on their next protected API request.
```

Normal single-session logout revokes only the submitted refresh token. System-admin refresh tokens are not implemented.

## 6. Business modules

### Invitations

```text
Create/list/read/revoke/accept
Search/filter/sort/pagination
Secure random raw token
Hashed token storage
Configurable expiration
One-time acceptance
Replacement invitation revocation
User creation on acceptance
Audit events
```

### Projects

Statuses:

```text
PLANNING
ACTIVE
ON_HOLD
COMPLETED
ARCHIVED
```

Implemented:

```text
Create/list/read/update/status/archive
Search/filter/sort/pagination
Tenant isolation
Role-aware writes
Archived-project immutability
Automatic creator project-lead membership
Audit events
```

### Project memberships

Roles:

```text
PROJECT_LEAD
MEMBER
```

Implemented:

```text
Add/list/read/change role/remove
Search/filter/sort/pagination
Active same-tenant user validation
Duplicate prevention
Last-project-lead protection
Archived-project immutability
Audit events
```

### Project tasks

Statuses:

```text
TODO
IN_PROGRESS
BLOCKED
COMPLETED
CANCELLED
```

Priorities:

```text
LOW
MEDIUM
HIGH
URGENT
```

Implemented:

```text
Create/list/read/update
Search/filter/sort/pagination
Status transitions
Assignment/unassignment
Completion timestamp handling
Cancellation
Cancelled-task immutability
Archived-project history
Assignee membership validation
Project-lead authority
Assigned-member status authority
Audit events
```

## 7. Dashboards and audit logging

Implemented dashboards:

```text
Platform dashboard
Tenant dashboard
```

Tenant dashboard includes:

```text
Tenant identity and status
User totals by status
Project totals by status
Project-membership count
Task totals by status
Overdue task count
Task completion percentage
```

Tenant audit logs cover:

```text
Tenant events
User events
Login/session events
Password events
Invitation events
Project events
Project-member events
Task events
```

Platform audit logs are stored separately and cover platform/system-admin operations.

## 8. System administration

Implemented:

```text
System-admin bootstrap and login
Current system-admin endpoint
Own password change
Create/list/read/status/unlock system admins
Platform dashboard
Tenant administration
System-admin tenant onboarding
Tenant-user administration
Tenant audit-log access
Platform audit logs
```

Safety rules:

```text
A system admin cannot deactivate themselves.
At least one active system admin must remain.
Only active DB-backed system admins pass authorization.
```

## 9. Flyway state

Current migrations include:

```text
V1  baseline schema
V2  user invitations
V3  projects
V4  project members
V5  project tasks
V6  retained recovery migration
V7  audit-action column conversion
V8  app-user session version
```

Rules:

```text
Never edit an already-applied migration.
Add the next numbered migration.
Keep schema validation enabled.
Use database-neutral SQL where practical.
```

## 10. Backend verification

Current verified backend suite:

```text
47 tests
```

Coverage includes:

```text
Application startup
System authentication
Tenant onboarding
Tenant isolation
Token lifecycle
Immediate session invalidation
Password lifecycle
Invitation lifecycle
Project lifecycle
Membership lifecycle
Task lifecycle
Role restrictions
Cross-tenant restrictions
Stable security error responses
```

Command:

```powershell
cd multitenant-saas
.\mvnw.cmd clean test
```

## 11. Frontend stack and infrastructure

```text
React 19.2
React DOM 19.2
TypeScript 6
Vite 8
Material UI 9
TanStack Query 5
Axios
React Router 7
React Hook Form
Zod
Vitest 4
Testing Library
```

Implemented infrastructure:

```text
Global theme and CSS baseline
React Query provider
Browser routing
Tenant authentication provider
System-admin authentication provider
Typed environment configuration
Typed API response handling
Normalized API errors
Tenant and system-admin HTTP clients
Automatic tenant token refresh
Refresh request deduplication
Session restoration
Query-cache clearing on logout
Role-aware route guards and navigation
Responsive tenant and system shells
```

Tenant restoration behavior:

```text
Read local storage
Validate through GET /api/auth/me
Refresh automatically after access-token 401
Clear after 401/403 authentication failure
Preserve local session during temporary network/backend failure
```

## 12. Frontend routes and role access

Public tenant routes:

```text
/login
/register
/forgot-password
/reset-password
/accept-invitation
```

Authenticated tenant routes:

```text
/dashboard
/users
/invitations
/projects
/projects/:projectId
/audit-logs
/account
/account/change-password
```

System-admin routes:

```text
/system/login
/system/dashboard
/system/tenants
/system/admins
/system/audit-logs
/system/change-password
```

Current role routing:

```text
TENANT_ADMIN
    Dashboard, users, invitations, projects,
    audit logs, account settings.

TENANT_MANAGER
    Dashboard, users, projects, account settings.

TENANT_USER
    Projects, project details, account settings.
    Default authenticated route: /projects.
```

Backend authorization remains authoritative. Frontend route protection is a usability layer.

## 13. Frontend feature coverage

Implemented:

```text
Tenant login and system-admin login
Public onboarding
Forgot/reset password
Invitation acceptance
Tenant and platform dashboards
Tenant user management
Invitation management
Project, membership, and task management
Tenant and platform audit logs
Account settings
Tenant and system-admin password changes
Sign out all devices
Tenant and system-admin administration
```

Frontend tests cover API clients, sessions, routes, pages, dialogs, forms, query/mutation behavior, and production TypeScript compilation.

Commands:

```powershell
cd multitenant-saas-frontend
npm run lint
npm run test
npm run build
```

Vitest settings:

```text
jsdom
Shared setup file
15-second test timeout
15-second hook timeout
Maximum two workers
```

## 14. Deferred work

```text
System-admin refresh tokens
Email provider and production token delivery
PostgreSQL
Containerization
CI/CD
Object storage
Subscriptions and billing
Notifications
Background processing
Production observability
```

These are intentionally deferred while authorization architecture is expanded.

## 15. Next major phase

The next phase is:

```text
Hierarchical, scoped, permission-based authorization
```

Primary goals:

```text
Arbitrary organizational depth
Organizational units
User reporting relationships
Permission catalogue
Custom tenant roles
Scoped role assignments
Direct-report and subtree authority
Project/resource scopes
Temporary delegation
Effective-access explanation
```

See:

```text
guides/authorization_v2_plan.md
guides/Plan.txt
```
