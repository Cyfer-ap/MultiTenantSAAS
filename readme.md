# Multi-Tenant SaaS Backend

A learning-focused **Multi-Tenant SaaS backend** built with **Spring Boot**, Java 21, Spring Security, JWT, Spring Data JPA, and H2.

The project demonstrates core SaaS backend concepts such as tenant onboarding, tenant isolation, role-based access control, authentication, refresh tokens, password reset, audit logging, and system-admin level platform access.

---

## Current Status

Implemented so far:

```text
Tenant onboarding
Tenant management
Tenant-scoped user management
Tenant roles and statuses
System admin bootstrap and login
JWT access-token authentication
Refresh-token rotation and revocation
Logout and logout-all
Change password
Forgot/reset password
Reusable strong password validation
DB-backed authorization checks
Tenant isolation
Tenant and system dashboards
Audit logging with actor/target user model
Swagger/OpenAPI UI
H2 file-based development database
```

---

## Tech Stack

```text
Backend: Spring Boot 4
Language: Java 21
Build: Maven
Database: H2 for local development
ORM: Spring Data JPA / Hibernate
Security: Spring Security
Auth: JWT access tokens + refresh tokens
API Docs: Springdoc OpenAPI / Swagger UI
```

---

## Local URLs

```text
Backend:      http://localhost:8081
Health:       http://localhost:8081/api/health
Swagger UI:   http://localhost:8081/swagger-ui.html
OpenAPI JSON: http://localhost:8081/v3/api-docs
H2 Console:   http://localhost:8081/h2-console
```

H2 login:

```text
JDBC URL: jdbc:h2:file:./data/multitenant_saas_db
Username: sa
Password: empty
```

---

## Quick Start

From the inner Spring Boot project folder:

```text
D:\Projects\multitenant-saas\multitenant-saas
```

Run:

```bash
mvnw.cmd spring-boot:run
```

or on Linux/macOS:

```bash
./mvnw spring-boot:run
```

---

## Default Local System Admin

The local bootstrap system admin is configured in `application.properties`:

```text
Email: system@saas.local
Password: SystemAdmin@123
```

System admin login:

```text
POST http://localhost:8081/api/system/auth/login
```

---

## Documentation

The detailed learning/reference material is inside the `guides` folder:

```text
guides/Details.txt
guides/progress.md
guides/postman_tests.md
guides/security_model.md
```

Recommended reading order:

```text
1. Details.txt
2. progress.md
3. security_model.md
4. postman_tests.md
```

---

## Main Roles

```text
SYSTEM_ADMIN    Platform-level admin, not tied to a tenant
TENANT_ADMIN    Full admin inside one tenant
TENANT_MANAGER  Read/management-level access inside one tenant
TENANT_USER     Basic tenant user
```

---

## Next Possible Work

```text
System-admin refresh tokens
System-admin audit logs
System-admin CRUD management
Plan/subscription module
Flyway database migrations
PostgreSQL migration
Automated tests
Email service for password reset
Invitation-based user onboarding
```
