# Developer Handoff

Use this page to resume development without depending on prior chat/session history.

## Repository

```text
Cyfer-ap/MultiTenantSAAS
default branch: main
base reviewed state: post-PR #65 (0e1edd6)
```

Always sync `main` before starting a new branch.

## Read first

1. [[Home]]
2. [[Architecture]]
3. [[Security-and-Authentication]]
4. [[Authorization]]
5. [[Collaboration-and-Attachments]]
6. [[Notifications]]
7. [[PostgreSQL-and-Flyway]]
8. [[Testing-and-CI]]
9. [[Operations-and-Observability]]
10. [[Roadmap]]

Repository-side focused notes remain under `guides/`.

## Current phase

**Product expansion + production/platform completion**

The previous Step 40 transaction/concurrency phase is historical. Major database-backed hardening and PostgreSQL concurrency regression coverage are already present.

The current short checkpoint is documentation synchronization plus one cross-module critical tenant journey. External billing is the next major platform milestone after the checkpoint.

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

## Current feature baseline

Implemented product capabilities include:

- tenant/system-admin control planes
- hardened browser authentication/session lifecycle
- verified-email login, OTP and password recovery
- scoped authorization and organization hierarchy
- subscription lifecycle/read-only/quota enforcement
- projects, memberships and Kanban/table task workspace
- comments, mentions, activity, replies and pinned comments
- R2/S3-compatible task attachments with cleanup hardening
- notification persistence/read state
- durable notification delivery records/outbox processing
- email notification delivery
- in-app notification center
- task assignment and task-status notifications
- comment/reply/mention notifications
- precise collaboration deep links
- per-event email preferences
- project membership notifications

## Migration invariant

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 current-schema baseline
db/common      -> portable V18+
```

Current common migrations extend through **V27**. Never rewrite an already-applied migration.

## Critical journey

`CriticalTenantJourneyIntegrationTest` is the cross-module checkpoint test. It verifies the composition of onboarding/login, invitation acceptance, project membership, task assignment, collaboration notifications, task status and session revocation.

Run it with:

```powershell
cd multitenant-saas
.\mvnw.cmd "-Dtest=CriticalTenantJourneyIntegrationTest" test
```

It supplements rather than replaces focused feature/security tests.

## Verification checklist

Backend:

```powershell
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify
```

Frontend when touched:

```powershell
npm run format:check
npm run lint
npm test
npm run build
```

For PowerShell targeted Maven tests with multiple classes:

```powershell
.\mvnw.cmd "-Dtest=FirstTest,SecondTest" test
```

Keep required GitHub checks green before merge.

## Next priority

External billing is the next major platform feature. It should integrate with the existing subscription/entitlement domain through a clean provider boundary rather than replacing that domain model.

## Deferred platform priorities

- usage metering/accounting
- tenant webhooks
- API keys/service accounts
- backup/restore, alerts and runbooks
- enterprise SSO
- authorization delegation/explain-access
- broader load/failure-recovery and production R2 verification
