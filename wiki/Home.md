# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform focused on tenant isolation, secure authentication, scoped authorization, project collaboration, subscription enforcement, PostgreSQL correctness and production-oriented engineering.

The version-controlled source for this Wiki lives under `wiki/` in the main repository. Publishing is described in [[Wiki-Maintenance]].

## Current platform state

Major implemented capabilities include:

- separate tenant and system-admin control planes
- JWT authentication, refresh-token rotation/revocation, logout/logout-all, password recovery and account lockout
- shared-schema tenant isolation
- scoped permission-oriented authorization and organization hierarchy
- invitations
- projects, memberships, tasks, Kanban/table workspace
- task comments, mentions, activity, one-level replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments with presigned flows
- tenant and platform audit logs
- internal subscription plans, lifecycle evaluation, read-only enforcement and quotas
- tenant-scoped notification persistence and unread/read state
- durable notification delivery records/outbox processing
- email notification delivery integration
- in-app task-assignment notification center
- PostgreSQL 17 + Flyway + Testcontainers validation
- Docker/Compose execution paths
- request correlation, Actuator/Micrometer observability
- CI, security scanning, container validation and Qodana

## Stack

**Backend:** Java 21, Spring Boot 4.0.7, Spring Security, OAuth2 Resource Server/JWT, Spring Data JPA/Hibernate, Flyway, PostgreSQL, Testcontainers, Actuator, AWS SDK v2.

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
  +--> domain invariants
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

Authorization does not bypass subscription restrictions, and a valid subscription does not grant missing permissions.

## Start here

- [[Local-Development]]
- [[Architecture]]
- [[Security-and-Authentication]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
- [[Frontend]]
- [[Testing-and-CI]]

## Product domains

- [[Projects-and-Tasks]]
- [[Collaboration-and-Attachments]]
- [[Notifications]]
- [[Subscriptions-and-Quotas]]

## Production and support

- [[Production-Deployment]]
- [[Operations-and-Observability]]
- [[API-and-Errors]]
- [[PostgreSQL-and-Flyway]]
- [[Transaction-and-Concurrency]]

## Project status

- [[Roadmap]]
- [[Developer-Handoff]]

## Production-readiness boundary

The platform has a strong production-readiness foundation, but it is not yet fully production-complete. Remaining platform gaps include external billing/reconciliation, durable usage metering, tenant webhooks, API keys/service accounts, backup/restore drills, external alerting/runbooks, enterprise SSO and broader failure/load verification.

The notification outbox and production email-delivery foundation are already implemented and should no longer be described as deferred generic platform work.
