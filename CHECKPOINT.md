# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`  
Branch: `main`  
Date: 2026-08-20  
Base reviewed state: post-PR #59 feature state; Dependabot PRs #53/#54 intentionally skipped/reverted via PR #60.

## Current phase

**Product expansion + production/platform completion**

The former Step 40 transaction/concurrency phase is no longer the active phase. Its main database-backed hardening work and PostgreSQL concurrency coverage are present and the remaining work should be treated as targeted follow-up, not as the project-wide current milestone.

## Confirmed platform state

- Java 21 / Spring Boot 4.0.7 backend
- React 19.2 / TypeScript 6 / Vite 8 frontend
- separate tenant and system-admin control planes
- shared-schema tenant isolation
- scoped permission-oriented authorization and organization hierarchy
- internal subscription lifecycle, read-only enforcement and quotas
- projects, memberships, tasks, Kanban/table workspace
- task collaboration: comments, mentions, activity, replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments with presigned flows
- tenant-scoped notification persistence and unread/read state
- durable notification delivery outbox with retries/lease/idempotency controls
- email notification delivery through the existing email abstraction
- in-app task-assignment notifications
- PostgreSQL 17 + Flyway + Testcontainers validation
- Docker/Compose production-oriented paths
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
```

Never rewrite an applied migration.

## Immediate product opportunities

1. expand notifications to comments, replies, mentions and task-status events
2. add precise collaboration deep links and notification preferences/channel controls
3. continue collaboration UX only where it adds clear product value

## Remaining platform gaps

- external billing provider + signed webhook reconciliation
- durable usage metering/accounting
- tenant outbound webhooks
- API keys/service accounts
- backup/restore drills, monitoring/alerts and operational runbooks
- enterprise SSO
- broader load/failure-recovery verification
- authorization delegation and explain-access

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
- `wiki/Home.md`
- `wiki/Roadmap.md`

When documentation disagrees with implementation, code/tests and Flyway migrations are authoritative.
