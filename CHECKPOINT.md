# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-08-20
Base reviewed state: post-PR #65 (`0e1edd6`).

## Current phase

**Product expansion + production/platform completion**

The former Step 40 transaction/concurrency phase is historical. The current short milestone is a post-PR #65 hardening checkpoint: synchronize documentation and add one cross-module critical user journey. External billing is the next major platform feature after this checkpoint.

## Confirmed platform state

- Java 21 / Spring Boot 4.0.7 backend
- React 19.2 / TypeScript 6 / Vite 8 frontend
- separate tenant and system-admin control planes
- shared-schema tenant isolation
- scoped permission-oriented authorization and organization hierarchy
- internal subscription lifecycle, read-only enforcement and quotas
- projects, memberships, tasks, Kanban/table workspace
- task collaboration: comments, mentions, activity, replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments with presigned flows and lifecycle cleanup
- tenant-scoped notification persistence and unread/read state
- durable notification delivery outbox with retries/lease/idempotency controls
- email notification delivery through the existing email abstraction
- task assignment/reassignment notifications
- task status/cancellation notifications
- comment, reply and mention notifications
- precise task/comment/reply notification deep links
- project membership add/role-change/remove notifications
- per-event optional email notification preferences
- PostgreSQL 17 + Flyway + Testcontainers validation
- Docker/Compose production-oriented paths
- request correlation, secured Actuator and SaaS-specific metrics
- CI, Security, Container CI and Qodana checks

## Migration checkpoint

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 baseline
db/common      -> portable V18+
```

Current common migrations extend through:

```text
V21 task collaboration
V22 task attachments
V23 attachment cleanup hardening
V24 comment replies and pins
V25 notifications
V26 notification deliveries
V27 notification preferences
```

Never rewrite an applied migration.

## Critical journey checkpoint

The focused test suite already covers individual authentication, authorization, project/task, collaboration, notification and subscription behaviors. The current hardening slice adds one intentionally cross-module journey covering:

```text
tenant onboarding/login
        ↓
invitation acceptance
        ↓
project membership
        ↓
task assignment
        ↓
comment mention and reply
        ↓
notification type/deep-link/read state
        ↓
task status change
        ↓
logout-all/session revocation
```

This protects integration seams without introducing a new browser-test dependency into the same change.

## Immediate priority

1. finish this documentation + critical-journey checkpoint
2. begin external billing integration against the existing subscription/entitlement model

## Remaining platform gaps

- external billing provider + signed webhook reconciliation
- durable usage metering/accounting
- tenant outbound webhooks
- API keys/service accounts
- backup/restore drills, monitoring/alerts and operational runbooks
- enterprise SSO
- broader load/failure-recovery verification
- authorization delegation and explain-access
- production-environment R2 operational validation

## Production profile

Production should continue to use:

```text
SPRING_PROFILES_ACTIVE=postgres,production
```

with secrets supplied through environment configuration rather than committed files.

## Documentation entry points

- `readme.md`
- `guides/README.md`
- `guides/HANDOFF.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `guides/collaboration_and_notifications.md`
- `wiki/Home.md`
- `wiki/Notifications.md`
- `wiki/Roadmap.md`

When documentation disagrees with implementation, code/tests and Flyway migrations are authoritative.
