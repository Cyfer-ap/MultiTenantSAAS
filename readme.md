# Multi-Tenant SaaS Platform

A full-stack multi-tenant SaaS platform focused on secure tenant isolation, permission-oriented authorization, project collaboration, subscription enforcement, PostgreSQL correctness, and production-oriented engineering.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Base reviewed state: post-PR #65 (`0e1edd6`)
> Current phase: **product expansion + production/platform completion**
> Immediate platform milestone: **external billing integration after the current hardening checkpoint**

## Platform capabilities

### Tenant plane

- tenant onboarding and lifecycle
- JWT authentication and session restoration
- hashed refresh tokens, rotation, revocation, logout and logout-all
- verified email login, email OTP, password recovery and account lockout/unlock
- tenant users and invitations
- organization hierarchy and scoped authorization
- projects, memberships and tasks
- Kanban/table task workspace
- task comments, mentions, activity history, one-level replies and pinned comments
- R2/S3-compatible task attachments with presigned upload/download flows
- tenant-scoped notifications, unread state and in-app notification center
- task assignment, task status, comment, reply, mention and project-membership notifications
- precise task/comment/reply notification deep links
- per-event optional email notification preferences
- tenant audit logs
- subscription visibility, lifecycle restrictions and quotas

### System plane

- separate system-admin authentication/control plane
- platform dashboard
- tenant administration and onboarding
- tenant-user administration
- system-admin management
- platform audit logs
- subscription plan and lifecycle administration

System administrators remain a separate identity domain; they are not tenant users with an elevated tenant role.

## Technology stack

### Backend

- Java 21
- Spring Boot 4.0.7
- Spring Security / OAuth2 Resource Server / JWT
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL 17
- H2 historical/test migration path
- Testcontainers PostgreSQL integration testing
- AWS SDK v2 for S3-compatible object storage / Cloudflare R2
- Spring Boot Actuator and Micrometer
- Maven Wrapper (`.\mvnw.cmd`)

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

## Security and request enforcement

Business requests pass through distinct enforcement layers:

```text
authentication
    ↓
tenant isolation
    ↓
authorization / scoped permission evaluation
    ↓
subscription lifecycle access
    ↓
resource quota enforcement
    ↓
domain invariants
```

These controls intentionally remain separate. Authorization never bypasses subscription restrictions, and a valid subscription never grants a missing permission.

## Collaboration and storage

Task collaboration is implemented as a tenant/project/task-scoped subsystem:

- comments and validated project-member mentions
- task activity timeline
- one-level comment replies
- pinned comments
- attachment metadata and lifecycle cleanup
- presigned object-storage uploads/downloads
- Cloudflare R2 through the S3-compatible AWS SDK

Relevant schema migrations:

```text
V21  task collaboration
V22  task attachments
V23  attachment cleanup hardening
V24  comment replies and pins
```

See `guides/collaboration_and_notifications.md` and the Wiki pages `Collaboration-and-Attachments` and `Notifications`.

## Notifications

The notification subsystem now includes:

- tenant- and recipient-scoped notification persistence
- unread count and read/read-all mutations
- durable PostgreSQL-backed delivery records
- retry/backoff/lease/idempotency-oriented delivery processing
- email delivery through the existing email-provider abstraction
- in-app notification bell and safe internal navigation
- task assignment/reassignment events
- task status/cancellation events
- top-level task comment events
- comment reply events
- mention events
- project membership add/role-change/remove events
- exact comment/reply deep links, including targets outside the normal first comment page
- recipient-scoped optional email preferences while in-app notification history remains mandatory

Relevant migrations:

```text
V25  notifications
V26  notification deliveries
V27  notification preferences
```

`WORKSPACE_INVITATION` and `SECURITY_ALERT` remain part of the notification type catalogue; security-alert email is intentionally mandatory/non-configurable. Product follow-up should now focus on genuinely new value such as invitation-event wiring, optional digests/live delivery and operational delivery visibility rather than rebuilding the notification foundation.

## Authorization

Authorization has evolved beyond the legacy coarse tenant roles. Current concepts include:

- permission catalogue
- authorization roles
- role-permission mappings
- user role assignments
- organization hierarchy and assignments
- project/organization scoped authorization evaluation
- backend-authoritative permission checks
- authorization management UI

Legacy roles such as `TENANT_ADMIN`, `TENANT_MANAGER` and `TENANT_USER` remain where required for compatibility.

Advanced authorization still worth adding later includes temporary delegation and an explain-access capability.

## Subscription and quota model

The platform separates:

- plans and entitlements
- tenant subscription lifecycle
- evaluated access state
- workspace read-only enforcement
- resource quotas

The current subscription model is internal. A production external billing provider, signed webhook reconciliation and payment-state synchronization are still future work and are the next major platform milestone.

## PostgreSQL and Flyway

Migration layout:

```text
multitenant-saas/src/main/resources/db/migration    historical H2 V1-V17
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL V17 baseline
multitenant-saas/src/main/resources/db/common      portable V18+
```

Current shared migrations have advanced through **V27**.

Never rewrite an already-applied migration.

## Concurrency hardening

The former Step 40 transaction/concurrency phase is no longer the active project phase. Database-backed protections and regression coverage now include, among other paths:

- subscription state serialization
- one-subscription-per-tenant creation races
- invitation single-use/replacement behavior
- failed-login/account-lock races
- refresh-token rotation/session invalidation races
- PostgreSQL pessimistic-lock integration coverage
- attachment completion/deletion serialization and cleanup recovery
- notification delivery leasing/idempotency behavior

`guides/step40_transaction_concurrency.md` is retained as a historical closeout/reference page.

## Testing strategy

Focused unit/integration tests remain the primary regression layer. In addition, the hardening checkpoint adds a deliberately cross-module critical tenant journey covering the wiring between:

```text
tenant onboarding + login
        ↓
invitation acceptance
        ↓
project membership
        ↓
task assignment
        ↓
comment mention / reply
        ↓
notification deep links + read state
        ↓
task status change
        ↓
session revocation
```

This journey is not a replacement for focused tests; it protects the seams between otherwise independently tested subsystems. A dedicated browser-E2E runner such as Playwright remains a later testing-infrastructure option rather than being mixed into this small checkpoint.

## Production and operations

Implemented foundation:

- `postgres,production` execution path
- production environment template
- Docker/Compose paths
- request correlation IDs and completion logging
- secured Actuator access
- SaaS-specific metrics
- CI, security scanning, container validation and Qodana

Still not production-complete:

- external billing provider and reconciliation
- durable usage metering/accounting
- tenant webhook platform
- API keys/service accounts
- backup/restore drills, alerts and operational runbooks
- enterprise SSO
- broader load/failure-recovery verification
- confirmed production-environment R2 round-trip/operations validation

See `guides/DEFERRED_PLATFORM_WORK.md` for the current platform backlog.

## Local development

### PostgreSQL

```powershell
docker compose -f .\docker-compose.postgres.yml up -d
```

### Backend

```powershell
cd multitenant-saas
.\mvnw.cmd test
.\mvnw.cmd verify
```

### Frontend

```powershell
cd multitenant-saas-frontend
npm install
npm run lint
npm test
npm run build
```

Typical local configuration:

```dotenv
BACKEND_PORT=8081
FRONTEND_PORT=8080
VITE_API_BASE_URL=http://localhost:8081
CORS_ALLOWED_ORIGINS=http://localhost:8080
```

## Documentation

Documentation source-of-truth order:

1. current application code and tests
2. current Flyway migrations
3. focused guides under `guides/`
4. historical planning/progress notes

Primary entry points:

- `CHECKPOINT.md`
- `guides/README.md`
- `guides/HANDOFF.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `wiki/Home.md`
- `wiki/Roadmap.md`

The version-controlled Wiki source lives under `wiki/`. Publish it with:

```powershell
.\scripts\publish-wiki.ps1
```

Use `-NoPush` to preview the Wiki diff without publishing.

## Current roadmap

Near-term order:

1. complete the post-PR #65 documentation and critical-journey hardening checkpoint
2. connect the internal subscription model to an external billing provider
3. add durable usage metering/accounting
4. add tenant webhooks and API keys/service accounts
5. add recovery/alerting/runbooks and broader operational verification
6. add enterprise SSO when product requirements justify it
7. add advanced authorization delegation/explain-access when needed

The platform has a strong production-readiness foundation, but it should not be described as fully production-complete until the remaining provider, recovery, observability and operational gaps are closed.
