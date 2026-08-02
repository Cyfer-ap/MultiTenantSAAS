# Multi-Tenant SaaS Platform

A full-stack learning project for building a secure, auditable, multi-tenant SaaS platform.

The current product vertical is project and workforce management. It includes tenant onboarding, tenant-scoped users, invitations, projects, project memberships, tasks, dashboards, audit logs, platform administration, password recovery, and multi-device session revocation.

Documentation snapshot:

```text
Repository: Cyfer-ap/MultiTenantSAAS
Baseline commit reviewed: ae1fa4cbb5133ae0b3bcd2596379e1ab64f36be1
Status: Feature-complete fixed-role MVP
Next major phase: Hierarchical and scoped authorization
```

## Repository structure

```text
MultiTenantSAAS/
├── guides/
├── multitenant-saas/             Spring Boot backend
├── multitenant-saas-frontend/    React frontend
└── readme.md
```

## Current capabilities

### Tenant application

```text
Public tenant onboarding
Tenant login and session restoration
Refresh-token rotation
Single-session logout
Sign out all tenant devices
Change password
Forgot/reset password
Account lockout and administrative unlock

Tenant dashboard
Tenant user management
Invitation lifecycle
Projects
Project memberships
Project tasks
Tenant audit logs
Account settings
Role-aware navigation and protected routes
```

### Platform administration

```text
System-admin login
System-admin password change
Platform dashboard
Tenant listing and management
System-admin tenant onboarding
Tenant-user management
System-admin management
Platform audit logs
```

### Security

```text
Shared-database, shared-schema tenancy
Tenant-scoped repository access
DB-backed authorization
Structured 401/403 responses
Hashed refresh, invitation, and reset tokens
Refresh-token rotation and revocation
Per-user tenant session version
Immediate tenant access-token rejection after logout-all,
password change, or password reset
Last-active-admin protections
Cross-tenant integration tests
```

## Technology stack

### Backend

```text
Java 21
Spring Boot 4.0.6
Spring Web MVC
Spring Data JPA
Spring Security
OAuth2 Resource Server / JWT
Flyway
H2 file database for local development
Maven
JUnit 5 and MockMvc
Springdoc OpenAPI
Actuator
```

### Frontend

```text
React 19
TypeScript 6
Vite 8
Material UI 9
TanStack Query 5
Axios
React Router 7
React Hook Form
Zod
Vitest
Testing Library
```

## Local development

### Backend configuration

From:

```text
multitenant-saas/src/main/resources/
```

copy:

```text
application-local.example.properties
```

to:

```text
application-local.properties
```

Set a local JWT secret of at least 32 bytes and choose strong local bootstrap credentials. The local properties file must remain uncommitted.

### Start the backend

From `multitenant-saas/`:

```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Backend URLs:

```text
API:          http://localhost:8081
Health:       http://localhost:8081/api/health
Swagger:      http://localhost:8081/swagger-ui.html
OpenAPI JSON: http://localhost:8081/v3/api-docs
H2 console:   http://localhost:8081/h2-console
```

### Frontend configuration

Create `multitenant-saas-frontend/.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

### Start the frontend

From `multitenant-saas-frontend/`:

```powershell
npm install
npm run dev
```

Default Vite URL:

```text
http://localhost:5173
```

## Verification commands

### Backend

```powershell
cd multitenant-saas
.\mvnw.cmd clean test
```

Current verified backend suite:

```text
47 tests
```

### Frontend

```powershell
cd multitenant-saas-frontend
npm run lint
npm run test
npm run build
```

Vitest is configured with bounded worker concurrency and extended test/hook timeouts to reduce jsdom and Material UI suite contention.

## Documentation

```text
guides/progress.md
    Current implemented state and technical inventory.

guides/Plan.txt
    Active roadmap and execution order.

guides/security_model.md
    Existing authentication, tenant isolation, authorization,
    and session-revocation model.

guides/authorization_v2_plan.md
    Design specification for hierarchical, scoped RBAC.

guides/frontend_architecture.md
    Frontend structure, session handling, routing, API access,
    state management, and conventions.

guides/frontend_testing.md
    Frontend testing strategy and regression checklist.

guides/postman_tests.md
    Manual backend API testing guide.
```

## Current authorization model

The MVP currently uses fixed tenant roles:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

and project roles:

```text
PROJECT_LEAD
MEMBER
```

The next phase will extend this into an enterprise authorization model with:

```text
Arbitrary organizational depth
Organizational units
Reporting relationships
Permission-based roles
Tenant-defined roles
Scoped role assignments
Direct-report and subtree scopes
Project/resource scopes
Temporary delegation
Effective-access explanation
```

The fixed roles must remain operational during migration so the existing MVP does not break.

## Development principles

```text
Every schema change uses Flyway.
Every business query is tenant scoped.
Sensitive mutations are audited.
Authorization is centralized and DB backed.
Collection APIs support pagination.
Secrets are environment controlled.
Frontend API errors use the shared backend contract.
New features include backend and frontend tests.
Complex architecture is introduced only when justified.
```

## Near-term direction

```text
1. Freeze and document the current MVP.
2. Design the authorization-v2 domain.
3. Implement organizational hierarchy.
4. Add permission catalogue and custom roles.
5. Add scoped role assignments.
6. Migrate existing modules incrementally.
7. Add delegation and effective-access inspection.
8. Revisit subscriptions, PostgreSQL, containers, and deployment
   after the authorization platform is stable.
```
