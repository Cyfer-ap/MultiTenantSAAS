# Multi-Tenant SaaS Backend — Progress Notes

## 1. Project Overview

This project is a learning-focused **Multi-Tenant SaaS backend** built with **Spring Boot**.

The goal is to implement core SaaS backend concepts step by step, including:

```text
Tenant management
User management
Authentication
JWT-based access tokens
Role-based authorization
Tenant isolation
Dashboard summary APIs
Audit logging
Subscription and plan management
Production database migration
```

The project currently runs locally on:

```text
http://localhost:8081
```

---

# 2. Project Setup

The project was created using **Spring Initializr** with Maven and Java 21.

Basic structure:

```text
src/main/java
src/main/resources
pom.xml
```

The application starts from:

```java
@SpringBootApplication
public class MultitenantSaasApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultitenantSaasApplication.class, args);
    }
}
```

## What this does

```text
Starts the Spring Boot application
Auto-configures required Spring components
Scans packages for controllers, services, repositories, configs, etc.
```

Because the main class is inside:

```text
com.chacha.multitenantsaas
```

Spring Boot automatically scans subpackages such as:

```text
controller
service
repository
entity
dto
config
exception
security
tenant
common
```

---

# 3. Current Package Structure

```text
com.chacha.multitenantsaas
 ├── common
 │   └── ApiResponse.java
 ├── config
 │   ├── SecurityConfig.java
 │   └── JwtConfig.java
 ├── controller
 │   ├── AuthController.java
 │   ├── AppUserController.java
 │   ├── DashboardController.java
 │   ├── HealthCheckController.java
 │   └── TenantController.java
 ├── dto
 │   ├── AppUserCreateRequest.java
 │   ├── AppUserResponse.java
 │   ├── AppUserRoleUpdateRequest.java
 │   ├── AppUserStatusUpdateRequest.java
 │   ├── AppUserUpdateRequest.java
 │   ├── DashboardSummaryResponse.java
 │   ├── LoginRequest.java
 │   ├── LoginResponse.java
 │   ├── TenantCreateRequest.java
 │   ├── TenantResponse.java
 │   ├── TenantStatusUpdateRequest.java
 │   └── TenantUpdateRequest.java
 ├── entity
 │   ├── AppUser.java
 │   ├── Tenant.java
 │   ├── TenantStatus.java
 │   ├── UserRole.java
 │   └── UserStatus.java
 ├── exception
 │   ├── AuthenticationFailedException.java
 │   ├── DuplicateResourceException.java
 │   ├── GlobalExceptionHandler.java
 │   └── ResourceNotFoundException.java
 ├── repository
 │   ├── AppUserRepository.java
 │   └── TenantRepository.java
 ├── security
 ├── service
 │   ├── AppUserService.java
 │   ├── AuthService.java
 │   ├── DashboardService.java
 │   ├── JwtService.java
 │   └── TenantService.java
 ├── tenant
 └── MultitenantSaasApplication.java
```

---

# 4. Dependencies Used

## Initial Dependencies

```text
Spring Web MVC
Spring Boot DevTools
Validation
Lombok
Spring Boot Actuator
```

## Database Dependencies

```text
Spring Data JPA
Hibernate
H2 Database
H2 Console
```

## Security and Authentication Dependencies

```text
Spring Security
Spring OAuth2 Resource Server
JWT support through Spring Security OAuth2 JWT classes
```

## Why these dependencies are used

```text
Spring Web MVC              → Build REST APIs
Validation                  → Validate request bodies
Spring Data JPA             → Work with database entities and repositories
Hibernate                   → JPA implementation
H2 Database                 → Local development database
H2 Console                  → Browser-based database inspection
Spring Security             → Authentication and authorization foundation
OAuth2 Resource Server      → JWT encoding/validation support
Actuator                    → Health and monitoring endpoints
DevTools                    → Development restart support
Lombok                      → Available, but mostly not used yet
```

---

# 5. Local Development Configuration

The backend runs on port:

```properties
server.port=8081
```

The project originally used an in-memory H2 database:

```properties
spring.datasource.url=jdbc:h2:mem:multitenant_saas_db
```

This caused data to disappear after server restart because `mem` means the database exists only in memory.

The project now uses file-based H2 for local persistence:

```properties
spring.datasource.url=jdbc:h2:file:./data/multitenant_saas_db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

JPA configuration:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

H2 console configuration:

```properties
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

Actuator configuration:

```properties
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

JWT configuration:

```properties
app.jwt.secret=change-this-secret-key-to-a-secure-32-byte-minimum-value-for-dev
app.jwt.expiration-minutes=60
app.jwt.issuer=multitenant-saas
```

## H2 Console Login

```text
URL: http://localhost:8081/h2-console
JDBC URL: jdbc:h2:file:./data/multitenant_saas_db
Username: sa
Password: empty
```

Useful H2 queries:

```sql
SELECT * FROM TENANTS;
SELECT * FROM APP_USERS;
SELECT ID, EMAIL, PASSWORD_HASH FROM APP_USERS;
```

The `data/` folder and H2 database files should not be committed to Git.

Recommended `.gitignore` entries:

```gitignore
data/
*.mv.db
*.trace.db
```

---

# 6. API Response Format

A common response wrapper was added:

```java
ApiResponse<T>
```

All custom APIs follow this response structure:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "2026-06-10T00:00:00Z"
}
```

## Why this matters

This keeps API responses consistent across:

```text
Tenant APIs
User APIs
Authentication APIs
Dashboard APIs
Error responses
```

---

# 7. Backend Request Flow

The project follows this standard Spring Boot layered flow:

```text
Client
  ↓
Controller
  ↓
Service
  ↓
Repository
  ↓
Database
```

Each layer has a clear role:

```text
Controller  → Handles HTTP requests and responses
Service     → Contains business logic
Repository  → Handles database operations
Entity      → Represents database tables
DTO         → Represents request/response payloads
Config      → Contains application configuration
Exception   → Handles custom/global errors
```

---

# 8. Exception Handling

The project has global exception handling through:

```java
@RestControllerAdvice
```

Files added:

```text
DuplicateResourceException.java
ResourceNotFoundException.java
AuthenticationFailedException.java
GlobalExceptionHandler.java
```

## Currently handled cases

```text
Duplicate resource
Resource not found
Authentication failure
Validation failure
Invalid request body / enum value
Illegal argument
Generic server error
```

## Why this matters

Instead of returning raw Spring Boot error pages or stack traces, the API returns clean JSON responses using `ApiResponse`.

Example duplicate response:

```json
{
  "success": false,
  "message": "Tenant slug already exists: acme",
  "data": null,
  "timestamp": "..."
}
```

---

# 9. Tenant Management

## Tenant Entity

The `Tenant` entity represents one organization, company, or customer using the SaaS platform.

Example:

```text
Tenant 1 → Acme Corporation
Tenant 2 → Nova Labs
Tenant 3 → Demo School
```

Current tenant fields:

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

## Important annotations used

```text
@Entity          → Maps Java class to database table
@Table           → Defines table-level settings and constraints
@Id              → Primary key
@GeneratedValue  → Auto-generates UUID
@Column          → Defines column constraints
@Enumerated      → Stores enum values
@PrePersist      → Runs before first save
@PreUpdate       → Runs before update
```

---

# 10. Tenant Repository, Service, Controller, and DTOs

Tenant module files added:

```text
Tenant.java
TenantStatus.java
TenantRepository.java
TenantService.java
TenantController.java
TenantCreateRequest.java
TenantUpdateRequest.java
TenantStatusUpdateRequest.java
TenantResponse.java
```

Tenant module flow:

```text
Client
  ↓
TenantController
  ↓
TenantService
  ↓
TenantRepository
  ↓
TENANTS table
```

## JpaRepository

```java
JpaRepository<Tenant, UUID>
```

This provides built-in methods:

```text
save()
findAll()
findById()
existsById()
deleteById()
count()
```

Custom tenant repository methods:

```java
Optional<Tenant> findBySlug(String slug);
boolean existsBySlug(String slug);
long countByStatus(TenantStatus status);
```

---

# 11. Tenant APIs

```text
POST   /api/tenants
GET    /api/tenants
GET    /api/tenants/{id}
GET    /api/tenants/slug/{slug}
PUT    /api/tenants/{id}
PATCH  /api/tenants/{id}/status
DELETE /api/tenants/{id}
```

## Create Tenant

```http
POST /api/tenants
Content-Type: application/json
```

Example body:

```json
{
  "name": "Nova Labs",
  "slug": "nova-labs"
}
```

## Get Tenant by ID

```http
GET /api/tenants/{id}
```

## Get Tenant by Slug

```http
GET /api/tenants/slug/{slug}
```

This is important because future tenant resolution may happen through:

```text
Subdomain: acme.yoursaas.com
Header: X-Tenant-Slug: acme
```

## Update Tenant

```http
PUT /api/tenants/{id}
```

Updates:

```text
name
slug
```

Duplicate slug protection prevents two tenants from using the same slug.

## Update Tenant Status

```http
PATCH /api/tenants/{id}/status
```

Allowed values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

## Deactivate Tenant / Soft Delete

```http
DELETE /api/tenants/{id}
```

This does not physically delete the tenant.

Internally it does:

```java
tenant.setStatus(TenantStatus.INACTIVE);
```

This preserves users, billing history, audit logs, and historical records.

---

# 12. User Management Foundation

The user module was added under tenants.

Files added:

```text
AppUser.java
UserRole.java
UserStatus.java
AppUserRepository.java
AppUserService.java
AppUserController.java
AppUserCreateRequest.java
AppUserUpdateRequest.java
AppUserRoleUpdateRequest.java
AppUserStatusUpdateRequest.java
AppUserResponse.java
```

We use `AppUser` instead of `User` because `User` can conflict with Spring Security classes.

---

# 13. Tenant and User Relationship

A user belongs to a tenant.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "tenant_id", nullable = false)
private Tenant tenant;
```

This means:

```text
Many users can belong to one tenant.
```

Relationship:

```text
Tenant
  └── Users
```

Example:

```text
Nova Labs
  ├── admin@novalabs.com
  ├── manager@novalabs.com
  └── user@novalabs.com
```

---

# 14. User Roles and Statuses

## User roles

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

## User statuses

```text
ACTIVE
INACTIVE
SUSPENDED
```

These are currently simple enums.

Later, roles can be expanded into a full role-permission system.

---

# 15. Email Uniqueness Inside Tenant

The `AppUser` entity has this unique constraint:

```java
columnNames = {"tenant_id", "email"}
```

This means the same email cannot be repeated inside the same tenant.

Blocked:

```text
Tenant A → admin@example.com
Tenant A → admin@example.com
```

Allowed by current design:

```text
Tenant A → admin@example.com
Tenant B → admin@example.com
```

This supports tenant-scoped identity.

---

# 16. Email Normalization

Before saving or checking emails, the app normalizes them:

```java
request.email().trim().toLowerCase()
```

This prevents duplicate-looking emails such as:

```text
Admin@Acme.com
admin@acme.com
```

---

# 17. User APIs

```text
POST   /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users/{userId}
PUT    /api/tenants/{tenantId}/users/{userId}
PATCH  /api/tenants/{tenantId}/users/{userId}/role
PATCH  /api/tenants/{tenantId}/users/{userId}/status
DELETE /api/tenants/{tenantId}/users/{userId}
```

## Create User Inside Tenant

```http
POST /api/tenants/{tenantId}/users
Content-Type: application/json
```

Current request body includes password:

```json
{
  "fullName": "Nova Admin",
  "email": "admin@novalabs.com",
  "password": "Password@123",
  "role": "TENANT_ADMIN"
}
```

The API accepts `password`, but the database stores only `password_hash`.

## Get Users by Tenant

```http
GET /api/tenants/{tenantId}/users
```

## Get Single User by ID Inside Tenant

```http
GET /api/tenants/{tenantId}/users/{userId}
```

This is safer than:

```text
/api/users/{userId}
```

because the lookup is tenant-scoped.

## Update User Details

```http
PUT /api/tenants/{tenantId}/users/{userId}
```

Updates:

```text
fullName
email
```

## Update User Role

```http
PATCH /api/tenants/{tenantId}/users/{userId}/role
```

Example:

```json
{
  "role": "TENANT_MANAGER"
}
```

## Update User Status

```http
PATCH /api/tenants/{tenantId}/users/{userId}/status
```

Example:

```json
{
  "status": "SUSPENDED"
}
```

## Deactivate User / Soft Delete

```http
DELETE /api/tenants/{tenantId}/users/{userId}
```

This does not physically delete the user.

Internally it does:

```java
user.setStatus(UserStatus.INACTIVE);
```

---

# 18. Tenant-Scoped User Lookup

Important repository method:

```java
findByTenantIdAndId(UUID tenantId, UUID userId)
```

This ensures that a user is fetched only if the user belongs to the requested tenant.

This is an early foundation of tenant isolation.

---

# 19. Dashboard Summary API

Files added:

```text
DashboardSummaryResponse.java
DashboardService.java
DashboardController.java
```

Endpoint:

```http
GET /api/dashboard/summary
```

The dashboard summary returns:

```text
Total tenants
Active tenants
Inactive tenants
Suspended tenants
Total users
Active users
Inactive users
Suspended users
```

Repository count methods added:

```java
long countByStatus(TenantStatus status);
long countByStatus(UserStatus status);
```

Spring Data JPA automatically converts these into SQL count queries filtered by status.

---

# 20. Security Foundation

Spring Security was added to the project.

File added:

```text
SecurityConfig.java
```

Current security configuration:

```text
CSRF disabled for API development
/api/** temporarily permitted
/h2-console/** permitted
/actuator/** permitted
Frame options set to same-origin for H2 console
PasswordEncoder bean configured
```

Current APIs are still open intentionally while authentication is being built.

Later, `/api/**` will be protected and only login/public endpoints will remain open.

---

# 21. Password Hashing

A password field was added to the create-user request:

```text
password
```

A password hash field was added to the `AppUser` entity:

```java
@Column(name = "password_hash", length = 255)
private String passwordHash;
```

During user creation, raw passwords are encoded:

```java
String passwordHash = passwordEncoder.encode(request.password());
```

The raw password is never stored.

The database stores:

```text
password_hash
```

not:

```text
password
```

## Important note

Older users created before this field was added may have `NULL` or blank `password_hash` values.

Those users cannot log in unless recreated or given a password hash.

---

# 22. Authentication: Basic Login

Files added:

```text
LoginRequest.java
LoginResponse.java
AuthenticationFailedException.java
AuthService.java
AuthController.java
```

Endpoint:

```http
POST /api/tenants/{tenantId}/auth/login
```

Request body:

```json
{
  "email": "admin@novalabs.com",
  "password": "Password@123"
}
```

The login process checks:

```text
Tenant exists
Tenant is ACTIVE
User exists inside that tenant
User is ACTIVE
User has password_hash
Password matches password_hash
```

Password verification uses:

```java
passwordEncoder.matches(request.password(), user.getPasswordHash())
```

This checks the raw password against the stored hash.

---

# 23. JWT Token Generation

JWT support was added after basic login.

Files added:

```text
JwtConfig.java
JwtService.java
```

Dependency added:

```text
spring-boot-starter-oauth2-resource-server
```

JWT configuration properties:

```properties
app.jwt.secret=change-this-secret-key-to-a-secure-32-byte-minimum-value-for-dev
app.jwt.expiration-minutes=60
app.jwt.issuer=multitenant-saas
```

JWT tokens are generated using:

```text
JwtEncoder
NimbusJwtEncoder
HS256 signing
```

## JWT claims currently included

```text
issuer
issuedAt
expiresAt
subject = userId
tenantId
email
fullName
role
```

## Current login response includes

```text
tenantId
userId
fullName
email
role
accessToken
tokenType
expiresInSeconds
message
```

Example response structure:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "tenantId": "...",
    "userId": "...",
    "fullName": "Nova Admin",
    "email": "admin@novalabs.com",
    "role": "TENANT_ADMIN",
    "accessToken": "eyJ...",
    "tokenType": "Bearer",
    "expiresInSeconds": 3600,
    "message": "Login successful"
  },
  "timestamp": "..."
}
```

---

# 24. Current API Summary

## Health and Utility APIs

```text
GET /api/health
GET /actuator/health
GET /h2-console
```

## Tenant APIs

```text
POST   /api/tenants
GET    /api/tenants
GET    /api/tenants/{id}
GET    /api/tenants/slug/{slug}
PUT    /api/tenants/{id}
PATCH  /api/tenants/{id}/status
DELETE /api/tenants/{id}
```

## User APIs

```text
POST   /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users/{userId}
PUT    /api/tenants/{tenantId}/users/{userId}
PATCH  /api/tenants/{tenantId}/users/{userId}/role
PATCH  /api/tenants/{tenantId}/users/{userId}/status
DELETE /api/tenants/{tenantId}/users/{userId}
```

## Authentication APIs

```text
POST /api/tenants/{tenantId}/auth/login
```

## Dashboard APIs

```text
GET /api/dashboard/summary
```

---

# 25. Current Project Status

The backend currently supports:

```text
Spring Boot setup
Maven setup
Java 21 configuration
Port 8081 configuration
Base package architecture
Common API response format
Global exception handling
H2 database integration
File-based H2 local persistence
H2 browser console
Spring Data JPA
Hibernate table generation
Actuator health endpoint
Tenant creation
Tenant listing
Tenant lookup by ID
Tenant lookup by slug
Tenant update
Tenant status update
Tenant soft delete
User entity under tenant
User creation inside tenant
User listing inside tenant
User lookup inside tenant
User detail update
User role update
User status update
User soft delete
Dashboard summary endpoint
Spring Security foundation
Temporary open security configuration
PasswordEncoder setup
Password hashing during user creation
Basic login credential validation
JWT access token generation
```

---

# 26. Important Concepts to Remember

## Multi-Tenant SaaS

A multi-tenant system serves multiple organizations using the same backend.

Each tenant should only access its own data.

## Tenant Isolation

Tenant isolation means data from one tenant should not be accessible from another tenant.

Example:

```text
Tenant A users should not be visible to Tenant B.
```

This is why we use tenant-scoped APIs like:

```text
/api/tenants/{tenantId}/users/{userId}
```

## DTOs

DTOs keep API request/response data separate from database entities.

This improves:

```text
Security
Clean API design
Maintainability
Flexibility
```

## Soft Delete

Soft delete means records are not physically removed from the database.

Instead, status is changed.

Example:

```text
DELETE tenant → tenant status becomes INACTIVE
DELETE user   → user status becomes INACTIVE
```

## Password Hashing

Passwords must not be stored as plain text.

The API accepts raw password only during request processing.

The database stores only the hashed value.

## JWT

A JWT has three parts:

```text
header.payload.signature
```

The payload contains claims such as:

```text
tenantId
userId
email
role
expiry time
```

The signature proves the token was generated by the backend and was not modified.

---

# 27. Current Limitations

The project is still in development.

Current limitations:

```text
JWT tokens are generated but not yet validated on protected routes
Most APIs are still temporarily open through SecurityConfig
No role-based authorization yet
No tenant context resolver yet
No automatic tenant extraction from token/header/subdomain yet
No refresh token support
No logout/token blacklist
No audit logging
No subscription/plan module
No PostgreSQL integration yet
No Flyway/Liquibase migrations yet
No Swagger/OpenAPI documentation yet
No automated tests yet
Secrets are still stored in application.properties for local development
```

---

# 28. Next Logical Steps

The next development steps should be:

```text
1. Add JWT decoder configuration
2. Validate JWT tokens on incoming requests
3. Protect selected APIs using Spring Security
4. Keep login and health endpoints public
5. Extract userId, tenantId, email, and role from JWT claims
6. Add role-based access control
7. Add tenant context resolution
8. Enforce tenant-level data isolation using authenticated token claims
9. Add audit logging
10. Move from H2 to PostgreSQL
11. Add Flyway or Liquibase database migrations
12. Add Swagger/OpenAPI documentation
13. Add automated tests
```
