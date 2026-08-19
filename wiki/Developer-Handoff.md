# Developer Handoff

Use this page to resume development without depending on prior chat/session history.

## Repository

```text
Cyfer-ap/MultiTenantSAAS
default branch: main
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
8. [[Operations-and-Observability]]
9. [[Roadmap]]

Repository-side focused notes remain under `guides/`.

## Current phase

**Product expansion + production/platform completion**

The previous Step 40 transaction/concurrency phase is historical. Major database-backed hardening and PostgreSQL concurrency regression coverage are already present.

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
- scoped authorization and organization hierarchy
- subscription lifecycle/read-only/quota enforcement
- projects, memberships and Kanban/table task workspace
- comments, mentions, activity, replies and pinned comments
- R2/S3-compatible task attachments
- notification persistence/read state
- durable notification delivery records/outbox processing
- email notification delivery
- in-app task-assignment notifications

## Migration invariant

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 current-schema baseline
db/common      -> portable V18+
```

Current common migrations extend through V26. Never rewrite an already-applied migration.

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

## Next priorities

Product:

1. comment/reply/mention/task-event notifications
2. more precise collaboration deep links
3. notification preferences/channel controls

Platform:

- external billing + webhook reconciliation
- usage metering/accounting
- tenant webhooks
- API keys/service accounts
- backup/restore, alerts and runbooks
- enterprise SSO
- authorization delegation/explain-access
