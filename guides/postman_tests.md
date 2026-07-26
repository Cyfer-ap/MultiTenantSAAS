# Multi-Tenant SaaS Backend — Postman Test Guide

Base URL:

```text
http://localhost:8081
```

All examples below use complete URLs.

## 1. Postman variables

```text
baseUrl = http://localhost:8081

systemAccessToken
systemAdminId

tenantId
tenantAdminUserId
tenantAccessToken
tenantRefreshToken

managerUserId
managerAccessToken
managerRefreshToken

normalUserId
normalUserAccessToken

invitationId
invitationToken

projectId
projectLeadUserId
projectMemberUserId

taskId
```

For JSON requests:

```http
Content-Type: application/json
```

For protected requests:

```http
Authorization: Bearer <accessToken>
```

Use locally configured system-admin credentials. Do not store real credentials in this guide.

## 2. Health and documentation

### Application health

```http
GET http://localhost:8081/api/health
```

Expected:

```text
200 OK
```

### Actuator health

```http
GET http://localhost:8081/actuator/health
```

### Swagger

```http
GET http://localhost:8081/swagger-ui.html
```

### OpenAPI JSON

```http
GET http://localhost:8081/v3/api-docs
```

## 3. System-admin authentication

### Login

```http
POST http://localhost:8081/api/system/auth/login
Content-Type: application/json
```

```json
{
  "email": "<local-system-admin-email>",
  "password": "<local-system-admin-password>"
}
```

Copy:

```text
data.accessToken -> systemAccessToken
```

### Current system admin

```http
GET http://localhost:8081/api/system/auth/me
Authorization: Bearer <systemAccessToken>
```

### Change system-admin password

```http
POST http://localhost:8081/api/system/auth/change-password
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

```json
{
  "currentPassword": "<current-password>",
  "newPassword": "NewSystemAdmin@123",
  "confirmPassword": "NewSystemAdmin@123"
}
```

## 4. System-admin management

### Create system admin

```http
POST http://localhost:8081/api/system/admins
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

```json
{
  "fullName": "Backup System Admin",
  "email": "backup.system@example.com",
  "password": "BackupSystem@123"
}
```

Copy returned system-admin ID to `systemAdminId`.

### List system admins

```http
GET http://localhost:8081/api/system/admins?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <systemAccessToken>
```

### Get system admin

```http
GET http://localhost:8081/api/system/admins/{systemAdminId}
Authorization: Bearer <systemAccessToken>
```

### Change status

```http
PATCH http://localhost:8081/api/system/admins/{systemAdminId}/status
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

```json
{
  "status": "SUSPENDED"
}
```

### Unlock system admin

```http
PATCH http://localhost:8081/api/system/admins/{systemAdminId}/unlock
Authorization: Bearer <systemAccessToken>
```

Safety checks:

```text
A system admin cannot deactivate themselves.
At least one active system admin must remain.
```

## 5. Tenant onboarding

### Public onboarding

```http
POST http://localhost:8081/api/onboarding/tenants
Content-Type: application/json
```

```json
{
  "tenantName": "Acme Corporation",
  "tenantSlug": "acme-corp",
  "adminFullName": "Acme Admin",
  "adminEmail": "admin@acme.com",
  "adminPassword": "Password@123"
}
```

Copy:

```text
data.tenant.id -> tenantId
data.adminUser.id -> tenantAdminUserId
```

### System-admin onboarding

```http
POST http://localhost:8081/api/system/onboarding/tenants
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

Use the same request body format.

### Direct creation is disabled

```http
POST http://localhost:8081/api/tenants
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

Expected:

```text
403 Forbidden
```

## 6. Tenant authentication and sessions

### Tenant login

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@acme.com",
  "password": "Password@123"
}
```

Copy:

```text
data.accessToken -> tenantAccessToken
data.refreshToken -> tenantRefreshToken
```

### Current user

```http
GET http://localhost:8081/api/auth/me
Authorization: Bearer <tenantAccessToken>
```

### Refresh token

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<tenantRefreshToken>"
}
```

Copy the rotated access and refresh tokens.
The previous refresh token must return 401 after rotation.

### Logout one session

```http
POST http://localhost:8081/api/auth/logout
Content-Type: application/json
```

```json
{
  "refreshToken": "<tenantRefreshToken>"
}
```

### Logout all sessions

```http
POST http://localhost:8081/api/auth/logout-all
Authorization: Bearer <tenantAccessToken>
```

### Change password

```http
POST http://localhost:8081/api/auth/change-password
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

### Forgot password

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/forgot-password
Content-Type: application/json
```

```json
{
  "email": "admin@acme.com"
}
```

Copy the local development reset token.

### Reset password

```http
POST http://localhost:8081/api/auth/reset-password
Content-Type: application/json
```

```json
{
  "resetToken": "<devResetToken>",
  "newPassword": "ResetPassword@123",
  "confirmPassword": "ResetPassword@123"
}
```

## 7. Tenant management

### List tenants as system admin

```http
GET http://localhost:8081/api/tenants?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <systemAccessToken>
```

### Get tenant

```http
GET http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <tenantAccessToken>
```

### Get by slug

```http
GET http://localhost:8081/api/tenants/slug/acme-corp
Authorization: Bearer <tenantAccessToken>
```

### Update tenant

```http
PUT http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "name": "Acme Corporation Updated",
  "slug": "acme-corp-updated"
}
```

### Update tenant status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "status": "ACTIVE"
}
```

Run suspension/deactivation tests last because they invalidate tenant access and refresh tokens.

## 8. Tenant users

### Create manager directly

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "fullName": "Acme Manager",
  "email": "manager@acme.com",
  "password": "Password@123",
  "role": "TENANT_MANAGER"
}
```

Copy returned ID to `managerUserId`.

### List users

```http
GET http://localhost:8081/api/tenants/{tenantId}/users?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

### Get user

```http
GET http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}
Authorization: Bearer <tenantAccessToken>
```

### Update user

```http
PUT http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "fullName": "Acme Manager Updated",
  "email": "manager.updated@acme.com"
}
```

### Update role

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/role
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "role": "TENANT_MANAGER"
}
```

### Update status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "status": "ACTIVE"
}
```

### Unlock user

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/unlock
Authorization: Bearer <tenantAccessToken>
```

### Deactivate user

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}
Authorization: Bearer <tenantAccessToken>
```

Admin-safety tests:

```text
Own role change -> 400
Own suspension/deactivation -> 400
Removing the last active tenant admin -> 400
```

## 9. User invitations

### Create invitation

```http
POST http://localhost:8081/api/tenants/{tenantId}/user-invitations
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "fullName": "Invited User",
  "email": "invited@acme.com",
  "role": "TENANT_USER"
}
```

Copy:

```text
data.invitationId -> invitationId
data.devInvitationToken -> invitationToken
```

### List invitations

```http
GET http://localhost:8081/api/tenants/{tenantId}/user-invitations?page=0&size=10&status=PENDING&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

### Get invitation

```http
GET http://localhost:8081/api/tenants/{tenantId}/user-invitations/{invitationId}
Authorization: Bearer <tenantAccessToken>
```

### Revoke invitation

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/user-invitations/{invitationId}/revoke
Authorization: Bearer <tenantAccessToken>
```

### Accept invitation

```http
POST http://localhost:8081/api/user-invitations/accept
Content-Type: application/json
```

```json
{
  "invitationToken": "<invitationToken>",
  "newPassword": "InvitedUser@123",
  "confirmPassword": "InvitedUser@123"
}
```

Reuse of the same token must return:

```text
401 Unauthorized
```

## 10. Projects

### Create project

```http
POST http://localhost:8081/api/tenants/{tenantId}/projects
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "name": "Customer Portal",
  "description": "Build the tenant-facing customer portal."
}
```

Copy:

```text
data.id -> projectId
```

Expected initial status:

```text
PLANNING
```

### List/search/filter projects

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects?page=0&size=10&search=customer&status=PLANNING&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

### Get project

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}
Authorization: Bearer <tenantAccessToken>
```

### Update project

```http
PUT http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "name": "Customer Management Portal",
  "description": "Updated project description."
}
```

### Update status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "status": "ACTIVE"
}
```

### Archive project

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}
Authorization: Bearer <tenantAccessToken>
```

Archived projects remain readable but cannot be modified.

## 11. Project members

The project creator is automatically added as `PROJECT_LEAD`.

### List members

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members?page=0&size=10
Authorization: Bearer <tenantAccessToken>
```

### Add member

```http
POST http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "userId": "{normalUserId}",
  "role": "MEMBER"
}
```

### Search/filter members

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members?page=0&size=10&search=user@acme.com&role=MEMBER
Authorization: Bearer <tenantAccessToken>
```

### Get member

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members/{normalUserId}
Authorization: Bearer <tenantAccessToken>
```

### Promote to project lead

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members/{normalUserId}/role
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "role": "PROJECT_LEAD"
}
```

### Remove member

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/members/{normalUserId}
Authorization: Bearer <tenantAccessToken>
```

Removing or demoting the last project lead must return 400.

## 12. Project tasks

### Create assigned task

```http
POST http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "title": "Implement authentication",
  "description": "Connect the frontend login and refresh flow.",
  "priority": "HIGH",
  "dueAt": "2026-08-15T12:00:00Z",
  "assigneeUserId": "{normalUserId}"
}
```

Copy:

```text
data.id -> taskId
```

Initial status:

```text
TODO
```

### Create unassigned task

```http
POST http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "title": "Write project documentation",
  "description": "Document the API contract.",
  "priority": "MEDIUM",
  "dueAt": null,
  "assigneeUserId": null
}
```

### List/search/filter tasks

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks?page=0&size=10&search=authentication&status=TODO&priority=HIGH&assigneeUserId={normalUserId}&sortBy=dueAt&sortDir=asc
Authorization: Bearer <tenantAccessToken>
```

### Get task

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
Authorization: Bearer <tenantAccessToken>
```

### Update task

```http
PUT http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "title": "Implement secure authentication",
  "description": "Updated task details.",
  "priority": "URGENT",
  "dueAt": "2026-08-20T12:00:00Z"
}
```

### Update task status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "status": "IN_PROGRESS"
}
```

Completing:

```json
{
  "status": "COMPLETED"
}
```

Expected:

```text
completedAt is non-null
```

Reopening a completed task clears `completedAt`.

### Reassign task

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}/assignee
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "assigneeUserId": "{projectMemberUserId}"
}
```

Unassign:

```json
{
  "assigneeUserId": null
}
```

An active tenant user who is not a project member must be rejected with 400.

### Cancel task

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/projects/{projectId}/tasks/{taskId}
Authorization: Bearer <tenantAccessToken>
```

Expected status:

```text
CANCELLED
```

Cancelled tasks cannot be modified.

## 13. Tenant audit logs

### List audit logs

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

### Filter user action

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?action=USER_CREATED&success=true&page=0&size=20
Authorization: Bearer <tenantAccessToken>
```

### Filter business action after V7/audit integration

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?action=PROJECT_CREATED&page=0&size=20
Authorization: Bearer <tenantAccessToken>
```

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?action=TASK_STATUS_UPDATED&page=0&size=20
Authorization: Bearer <tenantAccessToken>
```

### User-specific audit logs

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs/users/{normalUserId}?page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

## 14. Platform audit logs

```http
GET http://localhost:8081/api/system/audit-logs?page=0&size=20&sortBy=createdAt&sortDir=desc
Authorization: Bearer <systemAccessToken>
```

Optional filters:

```text
action
success
search
```

## 15. Dashboards

### Platform dashboard

```http
GET http://localhost:8081/api/dashboard/summary
Authorization: Bearer <systemAccessToken>
```

Tenant token expected:

```text
403 Forbidden
```

### Tenant dashboard

```http
GET http://localhost:8081/api/tenant/dashboard/summary
Authorization: Bearer <tenantAccessToken>
```

Allowed:

```text
TENANT_ADMIN
TENANT_MANAGER
```

## 16. Standard error-contract checks

### Missing token

```http
GET http://localhost:8081/api/system/admins
```

Expected:

```text
401
errorCode = AUTHENTICATION_REQUIRED
```

### Invalid enum

```http
GET http://localhost:8081/api/tenants/{tenantId}/projects?status=UNKNOWN
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
400
errorCode = INVALID_PARAMETER
```

### Malformed JSON

```http
POST http://localhost:8081/api/system/auth/login
Content-Type: application/json
```

```json
{
  "email": "broken"
```

Expected:

```text
400
errorCode = MALFORMED_REQUEST
```

### Validation failure

```http
POST http://localhost:8081/api/tenants/{tenantId}/projects
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

```json
{
  "name": "",
  "description": ""
}
```

Expected:

```text
400
errorCode = VALIDATION_FAILED
```

### Duplicate resource

Create the same invitation/user/project membership twice where uniqueness applies.

Expected:

```text
409
errorCode = RESOURCE_ALREADY_EXISTS
```

## 17. Isolation and role checks

Required negative tests:

```text
Tenant A token with Tenant B user route -> 403
Tenant A token with Tenant B invitation route -> 403
Tenant A token with Tenant B project route -> 403
Tenant A token with Tenant B project-member route -> 403
Tenant A token with Tenant B task route -> 403
TENANT_USER creating a project -> 403
TENANT_USER managing membership -> 403
Non-project member reading tasks -> 403
Project member editing task body -> 403
Unassigned member changing task status -> 403
Assigned member changing task status -> 200
PROJECT_LEAD managing tasks -> 200
```

## 18. Flyway verification

```sql
SELECT *
FROM "flyway_schema_history"
ORDER BY "installed_rank";
```

Current audit migration recovery:

```text
V6 must remain zero bytes because checksum 0 is already recorded.
V7 contains the actual audit ACTION conversion.
```

Verify the column:

```sql
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'AUDIT_LOGS'
  AND COLUMN_NAME = 'ACTION';
```

Expected after V7:

```text
DATA_TYPE = CHARACTER VARYING
CHARACTER_MAXIMUM_LENGTH = 60
```

## 19. Suggested test order

```text
1. Start local profile
2. Health checks
3. System-admin login
4. Tenant onboarding
5. Tenant-admin login
6. Invitation create and accept
7. Project create
8. Project membership
9. Task lifecycle
10. Tenant audit logs
11. Platform audit logs
12. Dashboard checks
13. Error contract checks
14. Cross-tenant checks
15. Refresh/logout/password checks
16. User/tenant suspension and deactivation last
```
