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

The old Step 40 transaction/concurrency milestone is no longer the active development phase. Its principal database-backed hardening and PostgreSQL concurrency coverage are already present.

## Current feature baseline

Implemented areas include:

- separate tenant and system-admin control planes
- secure browser authentication/session lifecycle
- tenant isolation
- scoped authorization and organization hierarchy
- subscription lifecycle, read-only enforcement and quotas
- projects, memberships and task Kanban/table workspace
- task collaboration: comments, mentions, activity, replies and pins
- R2/S3-compatible task attachments
- tenant notification persistence
- durable notification delivery records/outbox processing
- email notification delivery
- in-app task-assignment notifications
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

Current common migrations extend through V26. Never rewrite an already-applied migration.

## Next product priorities

1. broaden notifications to comments/replies/mentions/task status changes
2. improve collaboration deep links and notification preferences/channel controls
3. choose the next platform capability only when product value justifies it

## Deferred platform priorities

- external billing provider + webhook reconciliation
- usage metering/accounting
- tenant outbound webhooks
- API keys/service accounts
- recovery/alerting/runbooks
- enterprise SSO
- authorization delegation/explain-access

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
