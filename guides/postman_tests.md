# Multi-Tenant SaaS Backend — API Test Reference

Base URL:

```text
http://localhost:8081
```

Common header for JSON requests:

```http
Content-Type: application/json
```

Current API response format:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "..."
}
```

---

# 1. Health Check APIs

## 1.1 Application Health Check

```http
GET http://localhost:8081/api/health
```

Expected response:

```json
{
  "success": true,
  "message": "Health check successful",
  "data": "Multi-Tenant SaaS Backend is running on port 8081",
  "timestamp": "..."
}
```

## 1.2 Actuator Health Check

```http
GET http://localhost:8081/actuator/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

# 2. Tenant APIs

## 2.1 Create Tenant

```http
POST http://localhost:8081/api/tenants
Content-Type: application/json
```

Request body:

```json
{
  "name": "Acme Corporation",
  "slug": "acme"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant created successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation",
    "slug": "acme",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

Copy the returned tenant `id` for later tests.

---

## 2.2 Get All Tenants

```http
GET http://localhost:8081/api/tenants
```

Expected response:

```json
{
  "success": true,
  "message": "Tenants fetched successfully",
  "data": [
    {
      "id": "...",
      "name": "Acme Corporation",
      "slug": "acme",
      "status": "ACTIVE",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "timestamp": "..."
}
```

---

## 2.3 Get Tenant by ID

```http
GET http://localhost:8081/api/tenants/{tenantId}
```

Example:

```http
GET http://localhost:8081/api/tenants/2a3c7f3e-1b8a-4d6c-b3f5-2e1e8f7b9a11
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant fetched successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation",
    "slug": "acme",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 2.4 Get Tenant by Slug

```http
GET http://localhost:8081/api/tenants/slug/{slug}
```

Example:

```http
GET http://localhost:8081/api/tenants/slug/acme
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant fetched successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation",
    "slug": "acme",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 2.5 Tenant Not Found Test

```http
GET http://localhost:8081/api/tenants/slug/unknown-company
```

Expected response:

```json
{
  "success": false,
  "message": "Tenant not found with slug: unknown-company",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
404 Not Found
```

---

## 2.6 Update Tenant

```http
PUT http://localhost:8081/api/tenants/{tenantId}
Content-Type: application/json
```

Request body:

```json
{
  "name": "Acme Corporation Updated",
  "slug": "acme-updated"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant updated successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation Updated",
    "slug": "acme-updated",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 2.7 Update Tenant Status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "SUSPENDED"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant status updated successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation Updated",
    "slug": "acme-updated",
    "status": "SUSPENDED",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

Allowed tenant status values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Another request body example:

```json
{
  "status": "ACTIVE"
}
```

Another request body example:

```json
{
  "status": "INACTIVE"
}
```

---

## 2.8 Invalid Tenant Status Test

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "BLOCKED"
}
```

Expected response:

```json
{
  "success": false,
  "message": "Invalid request body. Please check the submitted values.",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
400 Bad Request
```

---

## 2.9 Deactivate Tenant

This endpoint performs a soft delete. It does not remove the tenant from the database. It changes the tenant status to `INACTIVE`.

```http
DELETE http://localhost:8081/api/tenants/{tenantId}
```

Expected response:

```json
{
  "success": true,
  "message": "Tenant deactivated successfully",
  "data": {
    "id": "...",
    "name": "Acme Corporation Updated",
    "slug": "acme-updated",
    "status": "INACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 2.10 Duplicate Tenant Slug Test

First create a tenant:

```http
POST http://localhost:8081/api/tenants
Content-Type: application/json
```

Request body:

```json
{
  "name": "Acme Corporation",
  "slug": "acme"
}
```

Send the same request again.

Expected response:

```json
{
  "success": false,
  "message": "Tenant slug already exists: acme",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
409 Conflict
```

---

## 2.11 Tenant Validation Error Test

```http
POST http://localhost:8081/api/tenants
Content-Type: application/json
```

Request body:

```json
{
  "name": "",
  "slug": ""
}
```

Expected response:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "name": "Tenant name is required",
    "slug": "Tenant slug is required"
  },
  "timestamp": "..."
}
```

Expected HTTP status:

```text
400 Bad Request
```

---

# 3. User APIs

Users are tenant-scoped. All user APIs require a valid `tenantId`.

User API base path:

```text
/api/tenants/{tenantId}/users
```

---

## 3.1 Create User for Tenant

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Acme Admin",
  "email": "admin@acme.com",
  "password": "Password@123",
  "role": "TENANT_ADMIN"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin",
    "email": "admin@acme.com",
    "role": "TENANT_ADMIN",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

Allowed user role values:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Important: the API accepts `password`, but the response does not return the password.

---

## 3.2 Create Tenant Manager User

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Acme Manager",
  "email": "manager@acme.com",
  "password": "Password@123",
  "role": "TENANT_MANAGER"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Manager",
    "email": "manager@acme.com",
    "role": "TENANT_MANAGER",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 3.3 Create Normal Tenant User

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Acme User",
  "email": "user@acme.com",
  "password": "Password@123",
  "role": "TENANT_USER"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User created successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme User",
    "email": "user@acme.com",
    "role": "TENANT_USER",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 3.4 Get Users by Tenant

```http
GET http://localhost:8081/api/tenants/{tenantId}/users
```

Expected response:

```json
{
  "success": true,
  "message": "Users fetched successfully",
  "data": [
    {
      "id": "...",
      "tenantId": "...",
      "fullName": "Acme Admin",
      "email": "admin@acme.com",
      "role": "TENANT_ADMIN",
      "status": "ACTIVE",
      "createdAt": "...",
      "updatedAt": "..."
    }
  ],
  "timestamp": "..."
}
```

Copy one user `id` for later tests.

---

## 3.5 Get Single User by ID Inside Tenant

```http
GET http://localhost:8081/api/tenants/{tenantId}/users/{userId}
```

Expected response:

```json
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin",
    "email": "admin@acme.com",
    "role": "TENANT_ADMIN",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 3.6 Update User Details

```http
PUT http://localhost:8081/api/tenants/{tenantId}/users/{userId}
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Acme Admin Updated",
  "email": "admin.updated@acme.com"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User updated successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin Updated",
    "email": "admin.updated@acme.com",
    "role": "TENANT_ADMIN",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 3.7 Update User Role

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{userId}/role
Content-Type: application/json
```

Request body:

```json
{
  "role": "TENANT_MANAGER"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User role updated successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin Updated",
    "email": "admin.updated@acme.com",
    "role": "TENANT_MANAGER",
    "status": "ACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

Allowed user role values:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

---

## 3.8 Update User Status

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{userId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "SUSPENDED"
}
```

Expected response:

```json
{
  "success": true,
  "message": "User status updated successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin Updated",
    "email": "admin.updated@acme.com",
    "role": "TENANT_MANAGER",
    "status": "SUSPENDED",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

Allowed user status values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Another request body example:

```json
{
  "status": "ACTIVE"
}
```

Another request body example:

```json
{
  "status": "INACTIVE"
}
```

---

## 3.9 Deactivate User

This endpoint performs a soft delete. It does not remove the user from the database. It changes the user status to `INACTIVE`.

```http
DELETE http://localhost:8081/api/tenants/{tenantId}/users/{userId}
```

Expected response:

```json
{
  "success": true,
  "message": "User deactivated successfully",
  "data": {
    "id": "...",
    "tenantId": "...",
    "fullName": "Acme Admin Updated",
    "email": "admin.updated@acme.com",
    "role": "TENANT_MANAGER",
    "status": "INACTIVE",
    "createdAt": "...",
    "updatedAt": "..."
  },
  "timestamp": "..."
}
```

---

## 3.10 Duplicate User Email Test

First create one user:

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Duplicate Test One",
  "email": "duplicate@acme.com",
  "password": "Password@123",
  "role": "TENANT_USER"
}
```

Now create another user in the same tenant with the same email:

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "Duplicate Test Two",
  "email": "duplicate@acme.com",
  "password": "Password@123",
  "role": "TENANT_USER"
}
```

Expected response:

```json
{
  "success": false,
  "message": "User email already exists for this tenant: duplicate@acme.com",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
409 Conflict
```

---

## 3.11 User Validation Error Test

```http
POST http://localhost:8081/api/tenants/{tenantId}/users
Content-Type: application/json
```

Request body:

```json
{
  "fullName": "",
  "email": "wrong-email",
  "password": "short",
  "role": null
}
```

Expected response:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "fullName": "Full name is required",
    "email": "Email must be valid",
    "password": "Password must be between 8 and 100 characters",
    "role": "User role is required"
  },
  "timestamp": "..."
}
```

Expected HTTP status:

```text
400 Bad Request
```

---

## 3.12 Invalid User Role Test

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{userId}/role
Content-Type: application/json
```

Request body:

```json
{
  "role": "SUPER_ADMIN"
}
```

Expected response:

```json
{
  "success": false,
  "message": "Invalid request body. Please check the submitted values.",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
400 Bad Request
```

---

# 4. Authentication APIs

Current authentication status:

```text
Login validates tenant, user, password, user status, and tenant status.
Login returns JWT access token.
JWT token validation/protected APIs are not yet implemented.
```

Auth API base path:

```text
/api/tenants/{tenantId}/auth
```

---

## 4.1 Login

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "admin@acme.com",
  "password": "Password@123"
}
```

Expected response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "tenantId": "...",
    "userId": "...",
    "fullName": "Acme Admin",
    "email": "admin@acme.com",
    "role": "TENANT_ADMIN",
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresInSeconds": 3600,
    "message": "Login successful"
  },
  "timestamp": "..."
}
```

Copy the `accessToken` for future protected API tests.

---

## 4.2 Login with Wrong Password

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "admin@acme.com",
  "password": "WrongPassword123"
}
```

Expected response:

```json
{
  "success": false,
  "message": "Invalid email or password",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
401 Unauthorized
```

---

## 4.3 Login with Unknown Email

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "unknown@acme.com",
  "password": "Password@123"
}
```

Expected response:

```json
{
  "success": false,
  "message": "Invalid email or password",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
401 Unauthorized
```

---

## 4.4 Login with Inactive User

First update user status:

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{userId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "INACTIVE"
}
```

Now attempt login:

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "admin@acme.com",
  "password": "Password@123"
}
```

Expected response:

```json
{
  "success": false,
  "message": "User account is not active",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
401 Unauthorized
```

Set the user back to active before continuing:

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/users/{userId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "ACTIVE"
}
```

---

## 4.5 Login with Inactive Tenant

First update tenant status:

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "INACTIVE"
}
```

Now attempt login:

```http
POST http://localhost:8081/api/tenants/{tenantId}/auth/login
Content-Type: application/json
```

Request body:

```json
{
  "email": "admin@acme.com",
  "password": "Password@123"
}
```

Expected response:

```json
{
  "success": false,
  "message": "Tenant is not active",
  "data": null,
  "timestamp": "..."
}
```

Expected HTTP status:

```text
401 Unauthorized
```

Set the tenant back to active before continuing:

```http
PATCH http://localhost:8081/api/tenants/{tenantId}/status
Content-Type: application/json
```

Request body:

```json
{
  "status": "ACTIVE"
}
```

---

# 5. Dashboard APIs

## 5.1 Get Dashboard Summary

```http
GET http://localhost:8081/api/dashboard/summary
```

Expected response:

```json
{
  "success": true,
  "message": "Dashboard summary fetched successfully",
  "data": {
    "totalTenants": 1,
    "activeTenants": 1,
    "inactiveTenants": 0,
    "suspendedTenants": 0,
    "totalUsers": 3,
    "activeUsers": 3,
    "inactiveUsers": 0,
    "suspendedUsers": 0
  },
  "timestamp": "..."
}
```

---

# 6. H2 Console Reference

H2 Console URL:

```text
http://localhost:8081/h2-console
```

For file-based H2:

```text
JDBC URL: jdbc:h2:file:./data/multitenant_saas_db
Username: sa
Password: empty
```

For in-memory H2, if used:

```text
JDBC URL: jdbc:h2:mem:multitenant_saas_db
Username: sa
Password: empty
```

Useful tenant query:

```sql
SELECT ID, NAME, SLUG, STATUS, CREATED_AT, UPDATED_AT FROM TENANTS;
```

Useful user query:

```sql
SELECT ID, TENANT_ID, FULL_NAME, EMAIL, ROLE, STATUS, PASSWORD_HASH, CREATED_AT, UPDATED_AT FROM APP_USERS;
```

Useful dashboard verification queries:

```sql
SELECT STATUS, COUNT(*) FROM TENANTS GROUP BY STATUS;
```

```sql
SELECT STATUS, COUNT(*) FROM APP_USERS GROUP BY STATUS;
```

---

# 7. Recommended Clean Test Sequence

Use this order when testing from scratch:

```text
1. Create tenant
2. Get all tenants
3. Get tenant by ID
4. Get tenant by slug
5. Update tenant
6. Update tenant status to ACTIVE
7. Create TENANT_ADMIN user
8. Create TENANT_MANAGER user
9. Create TENANT_USER user
10. Get users by tenant
11. Get single user by ID
12. Update user details
13. Update user role
14. Update user status to ACTIVE
15. Login
16. Copy JWT access token
17. Check dashboard summary
18. Test duplicate tenant slug
19. Test duplicate user email
20. Test not-found cases
21. Test invalid enum cases
```

---

# 8. Postman Environment Variables

Recommended variables:

```text
baseUrl = http://localhost:8081
tenantId = paste-created-tenant-id
userId = paste-created-user-id
accessToken = paste-login-access-token
```

Example variable usage:

```text
{{baseUrl}}/api/tenants
```

```text
{{baseUrl}}/api/tenants/{{tenantId}}/users
```

```text
{{baseUrl}}/api/tenants/{{tenantId}}/auth/login
```

Future protected API header:

```http
Authorization: Bearer {{accessToken}}
```

Currently, APIs are still open until JWT validation and endpoint protection are implemented.
