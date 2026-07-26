# Multi-Tenant SaaS Backend — Progress Notes

This document describes the current implementation state of the Spring Boot backend.

## 1. Project identity

Repository:

```text
https://github.com/Cyfer-ap/MultiTenantSAAS
```

Local application folder:

```text
D:\Projects\multitenant-saas\multitenant-saas
```

Repository layout:

```text
MultiTenantSAAS/
├── guides/
├── readme.md
└── multitenant-saas/
    ├── pom.xml
    ├── mvnw
    ├── mvnw.cmd
    └── src/
```

Local base URL:

```text
http://localhost:8081
```

## 2. Current technology stack

```text
Java 21
Spring Boot 4.0.6
Maven
Spring Web MVC
Spring Data JPA
Hibernate
Spring Security
OAuth2 Resource Server / JWT
H2 file database for local development
Flyway
Validation
Actuator
Springdoc OpenAPI / Swagger
Jackson 3
JUnit 5
MockMvc
```

Spring Boot 4 uses Jackson 3 in this project:

```java
tools.jackson.databind.json.JsonMapper
```

Do not add Jackson 2 imports such as:

```java
com.fasterxml.jackson.databind.ObjectMapper
```

## 3. Main source structure

```text
multitenant-saas/src/main/java/com/chacha/multitenantsaas/
├── common/
├── config/
├── controller/
├── dto/
├── entity/
├── exception/
├── repository/
├── security/
├── service/
├── validation/
└── MultitenantSaasApplication.java
```

Supporting folders:

```text
multitenant-saas/src/main/resources/
├── application.properties
└── db/migration/

multitenant-saas/src/test/
├── java/com/chacha/multitenantsaas/
└── resources/application-test.properties
```

## 4. Shared API infrastructure

Implemented:

```text
common/ApiResponse.java
common/ApiErrorResponse.java
common/ErrorCode.java
common/PaginationUtils.java
common/SortingUtils.java
dto/PageResponse.java
exception/GlobalExceptionHandler.java
```

Success responses use a shared wrapper.

Errors use a stable contract:

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

Security-level 401 and 403 responses also use the same contract through:

```text
security/JwtAuthenticationEntryPoint.java
security/JwtAccessDeniedHandler.java
```

## 5. Configuration and local environment

Completed:

```text
Centralized CORS configuration
Environment-based JWT secret
Externalized bootstrap credentials
Local/test profiles
JWT secret validation
H2 console restricted to local development
Ignored local properties and environment files
```

Important classes:

```text
config/CorsProperties.java
config/CorsConfig.java
config/SecurityConfig.java
security/JwtConfig.java
```

Local-only configuration belongs in:

```text
src/main/resources/application-local.properties
```

## 6. Tenant module

Tenant statuses:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Implemented tenant endpoints:

```text
GET    /api/tenants
GET    /api/tenants/{id}
GET    /api/tenants/slug/{slug}
PUT    /api/tenants/{id}
PATCH  /api/tenants/{id}/status
DELETE /api/tenants/{id}
```

The old endpoint is disabled:

```text
POST /api/tenants
```

Tenant creation is performed through onboarding so the tenant and initial administrator are created atomically.

Implemented:

```text
Tenant search
Status filtering
Sorting
Pagination
Soft deletion through INACTIVE status
Cross-tenant authorization
Refresh-token revocation when tenant status blocks access
```

## 7. Tenant onboarding

Public onboarding:

```text
POST /api/onboarding/tenants
```

System-admin onboarding:

```text
POST /api/system/onboarding/tenants
```

Onboarding creates:

```text
Tenant
Initial TENANT_ADMIN
Tenant onboarding audit log
```

## 8. Tenant users

Tenant roles:

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

Implemented endpoints:

```text
POST   /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users/{userId}
PUT    /api/tenants/{tenantId}/users/{userId}
PATCH  /api/tenants/{tenantId}/users/{userId}/role
PATCH  /api/tenants/{tenantId}/users/{userId}/status
PATCH  /api/tenants/{tenantId}/users/{userId}/unlock
DELETE /api/tenants/{tenantId}/users/{userId}
```

Implemented behavior:

```text
Tenant-scoped email uniqueness
Email normalization
Search/filter/sort/pagination
Strong passwords
Soft deletion
Role/status changes revoke refresh tokens
User lockout/unlock
DB-backed authorization
Self-management protection
Last-active-admin protection
```

## 9. Tenant authentication and sessions

Tenant login:

```text
POST /api/tenants/{tenantId}/auth/login
```

Current user:

```text
GET /api/auth/me
```

Session endpoints:

```text
POST /api/auth/refresh
POST /api/auth/logout
POST /api/auth/logout-all
POST /api/auth/change-password
```

Password recovery:

```text
POST /api/tenants/{tenantId}/auth/forgot-password
POST /api/auth/reset-password
```

Implemented security properties:

```text
Access tokens are JWTs
Refresh tokens are secure random tokens
Refresh tokens are stored only as SHA-256 hashes
Refresh-token rotation is implemented
Logout revokes one refresh token
Logout-all revokes every active refresh token for the user
Password changes and resets revoke refresh tokens
```

Access-token note:

```text
Logout revokes refresh tokens.
An already-issued access JWT remains valid until expiration unless live DB authorization
rejects the user or tenant because of a role/status change.
```

## 10. Strong password validation

Reusable annotation:

```text
validation/StrongPassword.java
```

Rules:

```text
8–100 characters
At least one uppercase letter
At least one lowercase letter
At least one number
At least one special character
No spaces
```

Applied to onboarding, user creation, invitation acceptance, password change, and password reset new passwords.

## 11. Account lockout

Configured behavior:

```text
Maximum failed attempts: 5
Default lock duration: 15 minutes
```

Implemented for:

```text
Tenant users
System admins
```

Fields:

```text
failedLoginAttempts
lockedUntil
```

Unlock endpoints exist for tenant users and system admins.

## 12. System-admin module

System admins are separate from tenant users.

Stored in:

```text
SYSTEM_ADMINS
```

System-admin auth:

```text
POST /api/system/auth/login
GET  /api/system/auth/me
POST /api/system/auth/change-password
```

System-admin management:

```text
POST  /api/system/admins
GET   /api/system/admins
GET   /api/system/admins/{systemAdminId}
PATCH /api/system/admins/{systemAdminId}/status
PATCH /api/system/admins/{systemAdminId}/unlock
```

Safety rules:

```text
A system admin cannot suspend/deactivate themselves
At least one active system admin must remain
One active system admin can unlock another
```

Current system-admin access includes:

```text
Platform dashboard
Tenant listing and tenant details
Tenant updates/status operations
System-admin tenant onboarding
Tenant-user read/write management
Tenant audit-log access
System-admin management
Platform audit logs
```

System-admin refresh tokens are not implemented.

## 13. Platform audit logging

Separate table:

```text
PLATFORM_AUDIT_LOGS
```

Current actions:

```text
SYSTEM_ADMIN_CREATED
SYSTEM_ADMIN_STATUS_UPDATED
SYSTEM_ADMIN_LOGIN_UNLOCKED
```

Endpoint:

```text
GET /api/system/audit-logs
```

Supports action/success filters, actor/target search, sorting, and pagination.

## 14. User invitation flow

Migration:

```text
V2__create_user_invitations.sql
```

Endpoints:

```text
POST  /api/tenants/{tenantId}/user-invitations
GET   /api/tenants/{tenantId}/user-invitations
GET   /api/tenants/{tenantId}/user-invitations/{invitationId}
PATCH /api/tenants/{tenantId}/user-invitations/{invitationId}/revoke
POST  /api/user-invitations/accept
```

Implemented behavior:

```text
Raw invitation token is generated securely
Only the token hash is stored
Invitation expiration is configurable
Pending invitation can be revoked
Creating a replacement invitation revokes the previous pending invitation
User account is created only after acceptance
Acceptance chooses a strong password
Invitation token is one-time use
Inviter identity is preserved for audit logging
```

The raw token is returned only as `devInvitationToken` in local development.

## 15. Project module

Migration:

```text
V3__create_projects.sql
```

Main files:

```text
entity/Project.java
entity/ProjectStatus.java
repository/ProjectRepository.java
service/ProjectService.java
controller/ProjectController.java
```

Endpoints:

```text
POST   /api/tenants/{tenantId}/projects
GET    /api/tenants/{tenantId}/projects
GET    /api/tenants/{tenantId}/projects/{projectId}
PUT    /api/tenants/{tenantId}/projects/{projectId}
PATCH  /api/tenants/{tenantId}/projects/{projectId}/status
DELETE /api/tenants/{tenantId}/projects/{projectId}
```

Statuses:

```text
PLANNING
ACTIVE
ON_HOLD
COMPLETED
ARCHIVED
```

Permissions:

```text
TENANT_ADMIN   create/read/update/archive
TENANT_MANAGER create/read/update/archive
TENANT_USER    read only
```

Features:

```text
Tenant-scoped repository access
Search
Status filter
Sorting
Pagination
Soft deletion through ARCHIVED status
Archived-project immutability
```

## 16. Project membership module

Migration:

```text
V4__create_project_members.sql
```

Main files:

```text
entity/ProjectMember.java
entity/ProjectMemberRole.java
repository/ProjectMemberRepository.java
service/ProjectMemberService.java
controller/ProjectMemberController.java
```

Roles:

```text
PROJECT_LEAD
MEMBER
```

Endpoints:

```text
POST   /api/tenants/{tenantId}/projects/{projectId}/members
GET    /api/tenants/{tenantId}/projects/{projectId}/members
GET    /api/tenants/{tenantId}/projects/{projectId}/members/{userId}
PATCH  /api/tenants/{tenantId}/projects/{projectId}/members/{userId}/role
DELETE /api/tenants/{tenantId}/projects/{projectId}/members/{userId}
```

Implemented behavior:

```text
Project creator becomes the initial PROJECT_LEAD
Only active users from the same tenant can be assigned
Duplicate membership is rejected
At least one project lead must remain
Archived project memberships are immutable
Search, role filtering, sorting, and pagination
```

## 17. Project task module

Migration:

```text
V5__create_project_tasks.sql
```

Main files:

```text
entity/ProjectTask.java
entity/ProjectTaskStatus.java
entity/ProjectTaskPriority.java
repository/ProjectTaskRepository.java
service/ProjectTaskService.java
controller/ProjectTaskController.java
security/ProjectSecurityService.java
```

Task statuses:

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

Endpoints:

```text
POST   /api/tenants/{tenantId}/projects/{projectId}/tasks
GET    /api/tenants/{tenantId}/projects/{projectId}/tasks
GET    /api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
PUT    /api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
PATCH  /api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/status
PATCH  /api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/assignee
DELETE /api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
```

Task authorization:

```text
TENANT_ADMIN / TENANT_MANAGER:
- full task management

PROJECT_LEAD:
- full task management for their project

Assigned project member:
- read task
- update task status

Other project member:
- read tasks only

Same-tenant user who is not a project member:
- cannot read project tasks
```

Task behavior:

```text
Assignee must be an active project member
Completing a task sets completedAt
Reopening a task clears completedAt
DELETE changes status to CANCELLED
Cancelled tasks are immutable
Archived-project tasks remain readable but cannot be modified
Search/filter/sort/pagination are supported
```

## 18. Dashboards

Platform dashboard:

```text
GET /api/dashboard/summary
```

Tenant dashboard:

```text
GET /api/tenant/dashboard/summary
```

The existing dashboards are implemented.

Next tenant-dashboard enhancement:

```text
Total projects
Active projects
Archived projects
Total project members
Total tasks
TODO tasks
IN_PROGRESS tasks
BLOCKED tasks
COMPLETED tasks
CANCELLED tasks
Overdue tasks
Completion percentage
```

## 19. Tenant audit logging

Current core actions include tenant, user, authentication, password, and invitation/user-creation events.

Business audit actions being integrated:

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

Current implementation work:

```text
ProjectService mutation methods record the JWT actor
ProjectMemberService mutation methods record actor and affected user
ProjectTaskService mutation methods record actor and assignee/affected user
Controllers pass @AuthenticationPrincipal Jwt to mutation methods
```

## 20. Flyway migration state

Migration directory:

```text
src/main/resources/db/migration
```

Current stable migrations:

```text
V1__baseline_schema.sql
V2__create_user_invitations.sql
V3__create_projects.sql
V4__create_project_members.sql
V5__create_project_tasks.sql
```

Current migration issue and recovery:

```text
V6 was applied while empty, so the database stored checksum 0.
SQL was later added to V6, producing a checksum mismatch.
```

Correct recovery:

```text
Keep V6__expand_tenant_audit_actions.sql as a zero-byte file.

Create:
V7__convert_audit_action_to_varchar.sql

With:
ALTER TABLE audit_logs
    ALTER COLUMN action SET DATA TYPE VARCHAR(60);

ALTER TABLE audit_logs
    ALTER COLUMN action SET NOT NULL;
```

Do not repair the checksum merely to hide the edit.
Do not edit applied migrations.
Do not delete the persistent database as the first response.

## 21. Automated integration tests

Test profile:

```text
src/test/resources/application-test.properties
```

Test classes:

```text
MultitenantSaasApplicationTests.java
integration/SystemAuthIntegrationTest.java
integration/TenantOnboardingAndIsolationIntegrationTest.java
integration/TokenLifecycleIntegrationTest.java
integration/UserInvitationIntegrationTest.java
integration/ProjectIntegrationTest.java
integration/ProjectMemberIntegrationTest.java
integration/ProjectTaskIntegrationTest.java
```

Current coverage includes:

```text
Standardized authentication errors
System-admin login
Tenant onboarding
Duplicate tenant protection
Cross-tenant isolation
Tenant login
Refresh-token rotation
Logout and logout-all
Password change
Invitation create/list/read/revoke/accept
One-time invitation tokens
Project lifecycle and permissions
Project membership lifecycle and permissions
Project task lifecycle and permissions
Assignment validation
Archived/cancelled immutability
```

Current test count before adding audit-specific tests:

```text
43
```

Run:

```powershell
.\mvnw.cmd clean test
```

## 22. Current status

Completed:

```text
Backend foundation
Standard API/error contracts
Tenant module
Tenant users
JWT authentication
Refresh sessions
Password change/reset
DB-backed tenant isolation
System-admin module
System-admin management
Account lockout
Tenant and platform audit infrastructure
User invitations
Flyway foundation
Environment profiles and CORS
Project module
Project membership module
Project task module
43 integration tests
```

In progress:

```text
Business audit logging for projects, project members, and tasks
V6/V7 Flyway recovery
```

## 23. Immediate next step

Do not start another module until the current migration and audit work is verified.

Required order:

```text
1. Restore V6 to a zero-byte file.
2. Create and apply V7 audit action VARCHAR migration.
3. Confirm ProjectService audit integration.
4. Confirm ProjectMemberService audit integration.
5. Confirm ProjectTaskService audit integration.
6. Confirm corresponding controllers pass Jwt.
7. Run .\mvnw.cmd clean test.
8. Verify business actions through tenant audit-log APIs.
```

After that:

```text
1. Add project/task metrics to the tenant dashboard.
2. Add dashboard integration tests.
3. Verify and freeze OpenAPI DTOs, status codes, enums, pagination, and errors.
4. Start the frontend.
```

## 24. Frontend readiness

The backend is close to the frontend boundary.

Backend work that should be completed before frontend:

```text
Finish business audit logging
Extend tenant dashboard metrics
Run complete tests
Freeze MVP API contract
Verify OpenAPI
```

Frontend implementation order:

```text
1. App shell and routing
2. Authentication/session handling
3. Role-aware navigation
4. System dashboard
5. Tenant dashboard
6. Tenant users and invitations
7. Projects
8. Project members
9. Tasks
10. Audit logs
```

Work that can wait until after the frontend begins:

```text
Email provider
Redis
Background jobs
PostgreSQL production tuning
Docker/deployment
Billing
Rate limiting
File storage
Advanced observability
Custom permission sets
```
