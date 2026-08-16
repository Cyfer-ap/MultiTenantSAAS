# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform focused on tenant isolation, authentication, permission-oriented authorization, organization-aware access, projects/tasks, subscription enforcement, PostgreSQL correctness, and production hardening.

This Wiki is the long-form technical reference for the repository. The version-controlled source for these pages lives in the main repository under `wiki/`; publishing is described in [[Wiki-Maintenance]].

## Current platform state

Major foundations already implemented and hardened include:

- tenant and system-administrator control planes
- JWT authentication, refresh-token rotation, logout, logout-all, password change/reset, and account lockout
- shared-schema tenant isolation
- permission-oriented authorization with scoped assignments
- organization hierarchy and reporting relationships
- invitations
- projects, memberships, and tasks
- tenant and platform audit logs
- subscription plans, lifecycle evaluation, read-only enforcement, and resource quotas
- PostgreSQL 17 + Flyway migration strategy
- Docker/Compose development and deployment paths
- PostgreSQL concurrency hardening and integration tests
- request correlation IDs and completion logging
- secured Actuator health/metrics access
- SaaS-specific Micrometer counters
- frontend route-level code splitting
- CI, container/security checks, and Qodana analysis

## Stack

**Backend:** Java 21, Spring Boot 4.0.7, Spring Security, OAuth2 Resource Server/JWT, Spring Data JPA/Hibernate, Flyway, PostgreSQL, H2 test/history path, Testcontainers, Actuator.

**Frontend:** React 19.2, TypeScript 6, Vite 8, Material UI 9, React Router 7, TanStack React Query, Axios, React Hook Form, Zod, Vitest and Testing Library.

## Request path

```text
Browser
  |
  v
React / Vite
  |
  v
Spring Security + JWT
  |
  v
Controller
  |
  +--> tenant isolation
  +--> authorization / scope evaluation
  +--> subscription access
  +--> quota enforcement
  |
  v
Transactional services
  |
  v
Tenant-scoped repositories
  |
  v
PostgreSQL
```

These controls are deliberately separate. Authorization does not bypass subscription restrictions, and a valid subscription does not grant missing permissions.

## Start here

- [[Local-Development]]
- [[Architecture]]
- [[Security-and-Authentication]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
- [[Frontend]]
- [[Testing-and-CI]]

For production and support work:

- [[Production-Deployment]]
- [[Operations-and-Observability]]
- [[API-and-Errors]]
- [[PostgreSQL-and-Flyway]]
- [[Transaction-and-Concurrency]]

For project status:

- [[Roadmap]]
- [[Developer-Handoff]]

## Production-readiness boundary

The application has a strong production-readiness foundation, but remaining operational work includes database backup/restore, external monitoring/alerting, provider billing/webhooks, background jobs/notifications, and additional load/failure-recovery verification.
