# Multi-Tenant SaaS Backend

A learning-focused Multi-Tenant SaaS backend built with **Spring Boot**.

The goal of this project is to understand and implement core SaaS backend concepts step by step, including tenant management, user management, authentication, role-based access control, tenant isolation, subscription handling, and scalable REST API design.

---

## Project Blueprint

This backend will gradually include the following modules:

```text
1. Project setup and base architecture
2. Tenant management
3. User management
4. Authentication and JWT authorization
5. Role-based access control
6. Tenant isolation
7. Organization-level operations
8. Subscription and plan management
9. Admin and tenant dashboard APIs
10. Audit logging
11. PostgreSQL migration
12. API documentation and testing
```

---

## Tech Stack

```text
Backend: Spring Boot
Language: Java 21
Build Tool: Maven
Database: H2 for development, PostgreSQL later
ORM: Spring Data JPA / Hibernate
Security: Spring Security
Authentication: JWT
API Style: REST
Server Port: 8081
```

---

## Current Package Structure

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
 ├── tenant
 └── MultitenantSaasApplication.java
```

---

## What Has Been Done So Far

The project has been initialized with Spring Boot and Maven.

A clean package structure has been created for building the SaaS backend step by step. The backend currently runs on port `8081`.

Currently completed:

```text
Spring Boot project setup
Base package structure
Health check API
Common API response wrapper
Global exception handling
H2 database integration
File-based H2 persistence
Spring Data JPA setup
Hibernate configuration
H2 browser console setup
Actuator health endpoint

Tenant entity
Tenant status management
Tenant CRUD-style APIs
Tenant soft delete / deactivation

User entity under tenant
User role and status management
Tenant-scoped user APIs
User soft delete / deactivation

Dashboard summary API

Spring Security setup
Password hashing with BCrypt
Basic login API
JWT access token generation
```

---

## Main Modules Implemented

### 1. Health Check

Used to verify that the backend is running correctly.

```text
GET /api/health
GET /actuator/health
```

---

### 2. Tenant Management

A tenant represents an organization, company, or customer using the SaaS platform.

Current tenant features:

```text
Create tenant
List tenants
Get tenant by ID
Get tenant by slug
Update tenant details
Update tenant status
Deactivate tenant
```

Tenant status values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

Tenant endpoints:

```text
POST   /api/tenants
GET    /api/tenants
GET    /api/tenants/{id}
GET    /api/tenants/slug/{slug}
PUT    /api/tenants/{id}
PATCH  /api/tenants/{id}/status
DELETE /api/tenants/{id}
```

---

### 3. User Management

Users are created inside a tenant. This keeps user operations tenant-scoped and prepares the project for tenant isolation.

Current user features:

```text
Create user under tenant
List users under tenant
Get single user under tenant
Update user details
Update user role
Update user status
Deactivate user
```

User roles:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

User status values:

```text
ACTIVE
INACTIVE
SUSPENDED
```

User endpoints:

```text
POST   /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users
GET    /api/tenants/{tenantId}/users/{userId}
PUT    /api/tenants/{tenantId}/users/{userId}
PATCH  /api/tenants/{tenantId}/users/{userId}/role
PATCH  /api/tenants/{tenantId}/users/{userId}/status
DELETE /api/tenants/{tenantId}/users/{userId}
```

---

### 4. Authentication

Basic authentication flow has been added.

Current authentication features:

```text
Password field added during user creation
Password stored as BCrypt hash
Login validates tenant, user, password, tenant status, and user status
JWT access token generated after successful login
```

Authentication endpoint:

```text
POST /api/tenants/{tenantId}/auth/login
```

Current login response includes:

```text
tenantId
userId
fullName
email
role
accessToken
tokenType
expiresInSeconds
```

JWT validation and protected endpoints are not yet implemented.

---

### 5. Dashboard Summary

A basic dashboard summary endpoint has been added to quickly check system-level counts.

Current dashboard data includes:

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

Dashboard endpoint:

```text
GET /api/dashboard/summary
```

---

## API Response Format

All custom APIs follow a common response format:

```json
{
  "success": true,
  "message": "Operation message",
  "data": {},
  "timestamp": "..."
}
```

This response format is handled through:

```text
common/ApiResponse.java
```

---

## Local Development Configuration

The project currently uses H2 for local development.

Recommended file-based H2 configuration:

```properties
spring.application.name=multitenant-saas

server.port=8081

spring.datasource.url=jdbc:h2:file:./data/multitenant_saas_db
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

app.jwt.secret=change-this-secret-key-to-a-secure-32-byte-minimum-value-for-dev
app.jwt.expiration-minutes=60
app.jwt.issuer=multitenant-saas
```

---

## H2 Console

H2 Console URL:

```text
http://localhost:8081/h2-console
```

Login details for file-based H2:

```text
JDBC URL: jdbc:h2:file:./data/multitenant_saas_db
Username: sa
Password: empty
```

Useful queries:

```sql
SELECT * FROM TENANTS;
```

```sql
SELECT * FROM APP_USERS;
```

---

## Current Maven Dependencies

The project currently uses these major dependencies:

```text
Spring Boot Web MVC
Spring Boot Validation
Spring Boot Actuator
Spring Data JPA
Hibernate
H2 Database
H2 Console
Spring Security
OAuth2 Resource Server / JWT support
Lombok
Spring Boot DevTools
```

For Spring Boot 4, H2 Console support requires:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-h2console</artifactId>
</dependency>
```

---

## Current Development Flow

The project currently follows this backend request flow:

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

For tenant-scoped user operations:

```text
Client
  ↓
AppUserController
  ↓
AppUserService
  ↓
AppUserRepository
  ↓
APP_USERS table
```

---

## Current Limitations

The project is still in active development.

Currently missing:

```text
JWT token validation
Protected API endpoints
Role-based authorization
Tenant context resolution
Automatic tenant isolation
Refresh tokens
Logout handling
Audit logging
Subscription and plan management
PostgreSQL integration
Database migrations
API documentation
Automated tests
```

---

## Next Planned Step

The next development step is to validate JWT tokens and protect selected APIs.

This will allow the backend to start enforcing authentication using:

```text
Authorization: Bearer <accessToken>
```

After that, the project can move toward role-based authorization and tenant-aware request handling.
