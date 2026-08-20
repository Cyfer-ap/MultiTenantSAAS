# Guide index

This directory contains focused current project documentation plus historical references.

## Source-of-truth order

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical workflow/planning notes

Never modify an already-applied migration to make documentation match.

## Current focused guides

- `current_architecture.md`
- `data_model.md`
- `security_model.md`
- `authorization_model.md`
- `subscription_billing.md`
- `postgresql_and_migrations.md`
- `frontend_architecture.md`
- `frontend_testing.md`
- `collaboration_and_notifications.md`
- `DEFERRED_PLATFORM_WORK.md`
- `HANDOFF.md`

Historical milestone references remain available, including:

- `step39_closeout.md`
- `step40_transaction_concurrency.md`
- `authorization_v2_plan.md`
- `progress.md`
- `Plan.txt`

## Current project phase

The project is in **product expansion + production/platform completion**.

The previous Step 40 transaction/concurrency phase is historical; its major PostgreSQL concurrency protections are already implemented. The current short checkpoint synchronizes documentation through PR #65 and adds cross-module critical-journey regression coverage before external billing begins.

Recent product milestones include:

- task collaboration, comments, mentions and activity
- threaded replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments
- notification persistence and in-app notification center
- durable notification delivery records/outbox processing
- email notification delivery integration
- task assignment and task-status notifications
- comment/reply/mention notifications
- precise task/comment/reply notification deep links
- per-event email notification preferences
- project membership notifications

Current portable Flyway migrations extend through **V27**.

## Verification baseline

Backend:

```powershell
cd multitenant-saas
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify
```

Focused critical journey:

```powershell
.\mvnw.cmd "-Dtest=CriticalTenantJourneyIntegrationTest" test
```

Frontend when touched:

```powershell
cd multitenant-saas-frontend
npm run format:check
npm run lint
npm test
npm run build
```

## Production profile

Production deployments should combine:

```text
postgres,production
```

and preserve tenant isolation, authorization, subscription enforcement, Flyway ownership and database-backed concurrency invariants.

## Immediate roadmap

External billing is the next major platform feature after the current checkpoint. See `DEFERRED_PLATFORM_WORK.md` for the broader intentionally deferred platform backlog.

## Wiki

Version-controlled Wiki source lives under `wiki/` and should be kept in sync with these focused guides.

Publish with:

```powershell
.\scripts\publish-wiki.ps1
```

Use `-NoPush` for a preview.
