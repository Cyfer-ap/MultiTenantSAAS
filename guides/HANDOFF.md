# Development Handoff

Use this file to resume development without relying on prior chat/session history.

## Repository

`Cyfer-ap/MultiTenantSAAS`

Default branch: `main`

## Start here

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/README.md`
4. `guides/current_architecture.md`
5. `guides/authorization_model.md`
6. `guides/collaboration_and_notifications.md`
7. `guides/DEFERRED_PLATFORM_WORK.md`
8. `wiki/Roadmap.md`

## Current phase

**Product expansion + production/platform completion**

Base reviewed state: post-PR #65 (`0e1edd6`). The old Step 40 transaction/concurrency milestone is historical. Its principal database-backed hardening and PostgreSQL concurrency coverage are already present.

The current short milestone is a post-PR #65 hardening checkpoint: synchronize repository/Wiki documentation and add one cross-module critical tenant journey. External billing is the next major feature after this checkpoint.

## Current feature baseline

Implemented areas include:

- separate tenant and system-admin control planes
- secure browser authentication/session lifecycle
- verified-email login, OTP and password recovery
- tenant isolation
- scoped authorization and organization hierarchy
- subscription lifecycle, read-only enforcement and quotas
- projects, memberships and task Kanban/table workspace
- task collaboration: comments, mentions, activity, replies and pins
- R2/S3-compatible task attachments with lifecycle cleanup
- tenant notification persistence and read state
- durable notification delivery records/outbox processing
- email notification delivery
- in-app notification center
- task assignment/reassignment notifications
- task status/cancellation notifications
- comment, reply and mention notifications
- precise task/comment/reply notification deep links
- per-event optional email preferences
- project membership add/role-change/remove notifications
- PostgreSQL/Flyway/Testcontainers path
- CI, Security, Container CI and Qodana

## Architecture boundaries to preserve

Do not collapse these concepts:

```text
authentication
tenant isolation
authorization
subscription lifecycle
quota enforcement
domain invariants
```

System-admin identity remains separate from tenant-user identity.

## Migration invariant

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 baseline
db/common      -> portable V18+
```

Current common migrations extend through **V27**. Never rewrite an already-applied migration.

## Critical-journey regression

The focused test suite remains authoritative for individual features. The hardening checkpoint adds `CriticalTenantJourneyIntegrationTest` to verify that these subsystem boundaries still compose correctly:

```text
onboarding/login
    ↓
invitation acceptance
    ↓
project membership
    ↓
task assignment
    ↓
comment mention/reply
    ↓
notification deep links/read state
    ↓
task status transition
    ↓
logout-all/session revocation
```

Run it with:

```powershell
cd multitenant-saas
.\mvnw.cmd "-Dtest=CriticalTenantJourneyIntegrationTest" test
```

Do not treat this as a substitute for the existing focused security, authorization, subscription, collaboration or PostgreSQL tests.

## Next product/platform priority

1. external billing provider boundary and provider mapping
2. signed webhook verification, replay protection and durable reconciliation
3. connect provider state to the existing subscription/entitlement model without weakening current lifecycle/authorization boundaries

## Deferred platform priorities

- usage metering/accounting
- tenant outbound webhooks
- API keys/service accounts
- recovery/alerting/runbooks
- enterprise SSO
- authorization delegation/explain-access
- broader load/failure-recovery and production R2 validation

## Verification baseline

Backend:

```powershell
cd multitenant-saas
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify
```

Frontend when touched:

```powershell
cd multitenant-saas-frontend
npm run format:check
npm run lint
npm test
npm run build
```

For targeted Maven tests containing multiple classes in PowerShell, quote the property:

```powershell
.\mvnw.cmd "-Dtest=FirstTest,SecondTest" test
```

Before merge, required GitHub checks should remain green.
