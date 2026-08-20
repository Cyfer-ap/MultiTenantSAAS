# MultiTenantSAAS Wiki

MultiTenantSAAS is a full-stack multi-tenant SaaS platform focused on tenant isolation, secure authentication, scoped authorization, project collaboration, subscription enforcement, PostgreSQL correctness and production-oriented engineering.

The version-controlled source for this Wiki lives under `wiki/` in the main repository. Publishing is described in [[Wiki-Maintenance]].

Current snapshot: **post-PR #65 (`0e1edd6`)**.

## Current platform state

Major implemented capabilities include:

- separate tenant and system-admin control planes
- JWT authentication, refresh-token rotation/revocation, logout/logout-all, verified-email login, OTP, password recovery and account lockout
- shared-schema tenant isolation
- scoped permission-oriented authorization and organization hierarchy
- invitations
- projects, memberships, tasks, Kanban/table workspace
- task comments, mentions, activity, one-level replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments with presigned flows and lifecycle cleanup
- tenant and platform audit logs
- internal subscription plans, lifecycle evaluation, read-only enforcement and quotas
- tenant-scoped notification persistence and unread/read state
- durable notification delivery records/outbox processing
- email notification delivery integration
- in-app notification center
- task assignment/reassignment notifications
- task status/cancellation notifications
- comment, reply and mention notifications
- precise task/comment/reply deep links
- per-event optional email notification preferences
- project membership add/role-change/remove notifications
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

## Current database checkpoint

Portable common migrations now extend through **V27**:

```text
V21 task collaboration
V22 task attachments
V23 attachment cleanup hardening
V24 comment replies and pins
V25 notifications
V26 notification deliveries
V27 notification preferences
```

## Testing checkpoint

Focused tests remain the primary regression layer. The post-PR #65 hardening checkpoint adds a cross-module critical tenant journey that exercises onboarding/login, invitation acceptance, project membership, task assignment, collaboration notifications/deep links, task status and session revocation in one flow. See [[Testing-and-CI]].

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

The platform has a strong production-readiness foundation, but it is not yet fully production-complete. Remaining platform gaps include external billing/reconciliation, durable usage metering, tenant webhooks, API keys/service accounts, backup/restore drills, external alerting/runbooks, enterprise SSO, production R2 operations validation and broader failure/load verification.

External billing is the next major platform milestone after the current documentation/critical-journey checkpoint.
