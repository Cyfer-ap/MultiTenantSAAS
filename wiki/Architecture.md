# Architecture

## Repository layout

```text
MultiTenantSAAS/
├── .github/                         CI, security and quality workflows
├── guides/                          focused engineering documentation
├── wiki/                            version-controlled GitHub Wiki source
├── scripts/                         verification and Wiki publishing helpers
├── multitenant-saas/                Spring Boot backend
├── multitenant-saas-frontend/       React/Vite frontend
├── compose.yaml                     full-stack Compose environment
├── docker-compose.postgres.yml      PostgreSQL-only development environment
├── .env.example
├── .env.production.example
└── qodana.yaml
```

## Technology boundaries

### Backend

- Java 21
- Spring Boot 4.0.7
- Spring Web MVC
- Spring Security
- OAuth2 Resource Server / JWT
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL 17
- H2 historical/test path
- Testcontainers
- Actuator / Micrometer
- Maven Wrapper

### Frontend

- React 19.2
- TypeScript 6
- Vite 8
- Material UI 9
- React Router 7
- TanStack React Query
- Axios
- React Hook Form + Zod
- Vitest + Testing Library

## Two control planes

The platform deliberately separates:

1. **Tenant plane** — tenant users, organization, authorization, projects, tasks, invitations, subscription state and tenant audit activity.
2. **System plane** — system administrators, platform dashboards, tenant administration, plan/subscription administration and platform audit activity.

A system administrator is not a tenant user with a stronger tenant role.

## Backend enforcement pipeline

```text
authentication
    ↓
tenant boundary
    ↓
authorization and target scope
    ↓
subscription lifecycle access
    ↓
resource quota
    ↓
domain invariant
    ↓
database constraint / transaction
```

A business operation may need to satisfy every layer.

## Persistence

Production-readiness work targets PostgreSQL. Schema evolution belongs to Flyway; Hibernate validates the production schema rather than creating it.

Tenant-scoped tables and repository queries must retain the tenant boundary. Prefer a tenant-qualified database lookup over an unscoped entity lookup followed by an in-memory tenant check.

## Cross-cutting design rules

- tenant isolation is never implied by role
- frontend visibility is not authorization
- subscription state is not authorization
- resource quotas are not subscription lifecycle state
- database invariants should be backed by constraints/locks when concurrency matters
- sensitive operations should be auditable
- request correlation data is operational metadata, not a business identifier
