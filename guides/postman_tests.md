# Multi-Tenant SaaS Backend — Postman Test Guide

Base URL:

```text
http://localhost:8081
```

Use these Postman variables:

```text
baseUrl = http://localhost:8081
systemAccessToken = <copy after system admin login>
tenantId = <copy after onboarding>
tenantAdminUserId = <copy after onboarding>
tenantAccessToken = <copy after tenant admin login>
tenantRefreshToken = <copy after tenant admin login>
managerUserId = <copy after creating manager>
managerAccessToken = <copy after manager login>
managerRefreshToken = <copy after manager login>
normalUserId = <copy after creating user>
```

For JSON requests:

```http
Content-Type: application/json
```

For protected APIs:

```http
Authorization: Bearer <accessToken>
```

---

## 1. Health Checks

### 1.1 Application health

```http
GET http://localhost:8081/api/health
```

Expected:

```text
200 OK
```

### 1.2 Actuator health

```http
GET http://localhost:8081/actuator/health
```

Expected:

```text
200 OK
```

---

## 2. System Admin Login

### 2.1 Login as system admin

```http
POST http://localhost:8081/api/system/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "system@saas.local",
  "password": "SystemAdmin@123"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.accessToken -> systemAccessToken
```

### 2.2 Invalid system admin password

```http
POST http://localhost:8081/api/system/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "system@saas.local",
  "password": "wrong"
}
```

Expected:

```text
401 Unauthorized
```

---

## 3. Tenant Onboarding

### 3.1 Onboard a tenant

```http
POST http://localhost:8081/api/onboarding/tenants
Content-Type: application/json
```

Body:

```json
{
  "tenantName": "Acme Corporation",
  "tenantSlug": "acme-corp",
  "adminFullName": "Acme Admin",
  "adminEmail": "admin@acme.com",
  "adminPassword": "Password@123"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.tenant.id -> tenantId
data.adminUser.id -> tenantAdminUserId
```

### 3.2 Invalid onboarding password

```http
POST http://localhost:8081/api/onboarding/tenants
Content-Type: application/json
```

Body:

```json
{
  "tenantName": "Bad Password Tenant",
  "tenantSlug": "bad-password-tenant",
  "adminFullName": "Bad Admin",
  "adminEmail": "badadmin@example.com",
  "adminPassword": "password123"
}
```

Expected:

```text
400 Bad Request
```

### 3.3 Direct tenant creation is disabled

```http
POST http://localhost:8081/api/tenants
Authorization: Bearer <systemAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "name": "Direct Tenant",
  "slug": "direct-tenant"
}
```

Expected:

```text
403 Forbidden
```

Use onboarding instead.

---

## 4. Tenant Admin Login

### 4.1 Login as tenant admin

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Example:

```http
POST http://localhost:8081/api/tenants/replace-with-tenant-id/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "admin@acme.com",
  "password": "Password@123"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.accessToken -> tenantAccessToken
data.refreshToken -> tenantRefreshToken
```

---

## 5. Current Tenant User

### 5.1 Get current logged-in tenant user

```http
GET http://localhost:8081/api/auth/me
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 6. Tenant Dashboard

### 6.1 Tenant admin dashboard

```http
GET http://localhost:8081/api/tenant/dashboard/summary
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 6.2 System admin cannot access tenant dashboard

```http
GET http://localhost:8081/api/tenant/dashboard/summary
Authorization: Bearer <systemAccessToken>
```

Expected:

```text
403 Forbidden
```

---

## 7. System Dashboard

### 7.1 System admin dashboard

```http
GET http://localhost:8081/api/dashboard/summary
Authorization: Bearer <systemAccessToken>
```

Expected:

```text
200 OK
```

### 7.2 Tenant admin cannot access system dashboard

```http
GET http://localhost:8081/api/dashboard/summary
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
403 Forbidden
```

---

## 8. Tenant Read APIs

### 8.1 System admin list all tenants

```http
GET http://localhost:8081/api/tenants?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <systemAccessToken>
```

Expected:

```text
200 OK
```

### 8.2 Tenant admin cannot list all tenants

```http
GET http://localhost:8081/api/tenants?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
403 Forbidden
```

### 8.3 Get tenant by ID as tenant admin

```http
GET http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 8.4 Get tenant by ID as system admin

```http
GET http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <systemAccessToken>
```

Expected:

```text
200 OK
```

### 8.5 Get tenant by slug

```http
GET http://localhost:8081/api/tenants/slug/acme-corp
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 9. Tenant Update APIs

### 9.1 Update tenant

```http
PUT http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "name": "Acme Corporation Updated",
  "slug": "acme-corp-updated"
}
```

Expected:

```text
200 OK
```

If you update the slug, remember to use the new slug in slug-based tests.

### 9.2 Update tenant status to ACTIVE

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "ACTIVE"
}
```

Expected:

```text
200 OK
```

### 9.3 Deactivate tenant

Run this only at the end of testing because it makes the tenant inactive.

```http
DELETE http://localhost:8081/api/tenants/{tenantId}
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 10. Tenant User APIs

### 10.1 Create manager

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Acme Manager",
  "email": "manager@acme.com",
  "password": "Password@123",
  "role": "TENANT_MANAGER"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.id -> managerUserId
```

### 10.2 Create normal user

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Acme User",
  "email": "user@acme.com",
  "password": "Password@123",
  "role": "TENANT_USER"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.id -> normalUserId
```

### 10.3 List tenant users as tenant admin

```http
GET http://localhost:8081/api/tenants/{tenantId}/users?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 10.4 List tenant users as system admin

```http
GET http://localhost:8081/api/tenants/{tenantId}/users?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <systemAccessToken>
```

Expected:

```text
200 OK
```

### 10.5 Get one tenant user

```http
GET http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 10.6 Update user details

```http
PUT http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Acme Manager Updated",
  "email": "manager.updated@acme.com"
}
```

Expected:

```text
200 OK
```

### 10.7 Update user role

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/role
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "role": "TENANT_USER"
}
```

Expected:

```text
200 OK
```

### 10.8 Update user status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "SUSPENDED"
}
```

Expected:

```text
200 OK
```

### 10.9 Reactivate user

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "ACTIVE"
}
```

Expected:

```text
200 OK
```

### 10.10 Deactivate user

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/users/{normalUserId}
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 11. Tenant Admin Safety Tests

### 11.1 Admin cannot change own role

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{tenantAdminUserId}/role
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "role": "TENANT_MANAGER"
}
```

Expected:

```text
400 Bad Request
```

### 11.2 Admin cannot deactivate own account

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/users/{tenantAdminUserId}
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
400 Bad Request
```

### 11.3 Last active tenant admin cannot be removed

If the tenant has only one active `TENANT_ADMIN`, try changing that admin's status to `SUSPENDED`.

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{tenantAdminUserId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "SUSPENDED"
}
```

Expected:

```text
400 Bad Request
```

---

## 12. Manager Login and Authorization

### 12.1 Login as manager

If you changed the manager email earlier, use the updated email.

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "manager.updated@acme.com",
  "password": "Password@123"
}
```

Expected:

```text
200 OK
```

Copy:

```text
data.accessToken -> managerAccessToken
data.refreshToken -> managerRefreshToken
```

### 12.2 Manager can read tenant dashboard

```http
GET http://localhost:8081/api/tenant/dashboard/summary
Authorization: Bearer <managerAccessToken>
```

Expected:

```text
200 OK
```

### 12.3 Manager cannot create users

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Authorization: Bearer <managerAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "fullName": "Blocked User",
  "email": "blocked@acme.com",
  "password": "Password@123",
  "role": "TENANT_USER"
}
```

Expected:

```text
403 Forbidden
```

---

## 13. Refresh Token Tests

### 13.1 Refresh tenant access token

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<tenantRefreshToken>"
}
```

Expected:

```text
200 OK
```

Copy the new tokens from the response. Refresh-token rotation means the old refresh token should not be reused.

### 13.2 Old refresh token should fail after rotation

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<oldTenantRefreshToken>"
}
```

Expected:

```text
401 Unauthorized
```

### 13.3 Logout one refresh token

```http
POST http://localhost:8081/api/auth/logout
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<tenantRefreshToken>"
}
```

Expected:

```text
200 OK
```

### 13.4 Logout all tenant user sessions

```http
POST http://localhost:8081/api/auth/logout-all
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 14. Password Change

### 14.1 Change current user's password

```http
POST http://localhost:8081/api/auth/change-password
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "currentPassword": "Password@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

Expected:

```text
200 OK
```

After this, login using the new password.

### 14.2 Same password should fail

```http
POST http://localhost:8081/api/auth/change-password
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "currentPassword": "NewPassword@123",
  "newPassword": "NewPassword@123",
  "confirmPassword": "NewPassword@123"
}
```

Expected:

```text
400 Bad Request
```

---

## 15. Forgot and Reset Password

### 15.1 Request password reset

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/forgot-password
Content-Type: application/json
```

Body:

```json
{
  "email": "admin@acme.com"
}
```

Expected:

```text
200 OK
```

In local development, copy the returned `devResetToken`.

### 15.2 Reset password

```http
POST http://localhost:8081/api/auth/reset-password
Content-Type: application/json
```

Body:

```json
{
  "resetToken": "<devResetToken>",
  "newPassword": "ResetPassword@123",
  "confirmPassword": "ResetPassword@123"
}
```

Expected:

```text
200 OK
```

---

## 16. Audit Log APIs

### 16.1 Get tenant audit logs

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 16.2 Filter audit logs by action

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs?page=0&size=10&sortBy=createdAt&sortDir=desc&action=USER_CREATED&success=true
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

### 16.3 Get audit logs for one user

```http
GET http://localhost:8081/api/tenants/{tenantId}/audit-logs/users/{managerUserId}?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <tenantAccessToken>
```

Expected:

```text
200 OK
```

---

## 17. Session Revocation Security Tests

### 17.1 Role downgrade revokes refresh token

1. Login as manager and save `managerRefreshToken`.
2. As tenant admin, downgrade manager role.

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/role
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "role": "TENANT_USER"
}
```

Expected:

```text
200 OK
```

3. Try old manager refresh token.

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<managerRefreshToken>"
}
```

Expected:

```text
401 Unauthorized
```

### 17.2 User suspension revokes refresh token

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{managerUserId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "SUSPENDED"
}
```

Expected:

```text
200 OK
```

Try the old refresh token:

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<managerRefreshToken>"
}
```

Expected:

```text
401 Unauthorized
```

### 17.3 Tenant suspension revokes all tenant refresh tokens

Run this near the end because it affects the whole tenant.

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Authorization: Bearer <tenantAccessToken>
Content-Type: application/json
```

Body:

```json
{
  "status": "SUSPENDED"
}
```

Expected:

```text
200 OK
```

Try any old tenant refresh token:

```http
POST http://localhost:8081/api/auth/refresh
Content-Type: application/json
```

Body:

```json
{
  "refreshToken": "<oldTenantRefreshToken>"
}
```

Expected:

```text
401 Unauthorized
```

---

## 18. Useful Final Checks

### 18.1 Swagger UI

```http
GET http://localhost:8081/swagger-ui.html
```

Expected:

```text
200 OK
```

### 18.2 OpenAPI JSON

```http
GET http://localhost:8081/v3/api-docs
```

Expected:

```text
200 OK
```

### 18.3 H2 Console

```http
GET http://localhost:8081/h2-console
```

Expected:

```text
200 OK
```

---

## 19. Suggested Test Order

Use this order for clean testing:

```text
1. Health check
2. System admin login
3. Tenant onboarding
4. Tenant admin login
5. Current user / me
6. Tenant dashboard
7. System dashboard
8. Tenant read/update
9. User create/list/update
10. Manager login
11. Authorization forbidden tests
12. Refresh/logout tests
13. Password change/reset tests
14. Audit log tests
15. Token revocation tests
16. Tenant suspension/deactivation tests last
```
