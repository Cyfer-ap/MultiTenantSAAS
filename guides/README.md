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

The project is now in **product expansion + production/platform completion**.

The previous Step 40 transaction/concurrency phase is retained as historical engineering context; its major PostgreSQL concurrency protections are already implemented.

Recent product milestones include:

- task collaboration, comments, mentions and activity
- threaded replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments
- notification persistence
- durable notification delivery records/outbox processing
- email notification delivery integration
- in-app task-assignment notifications

## Production profile

Production deployments should combine:

```text
postgres,production
```

and preserve tenant isolation, authorization, subscription enforcement, Flyway ownership and database-backed concurrency invariants.

## Wiki

Version-controlled Wiki source lives under `wiki/` and should be kept in sync with these focused guides.

Publish with:

```powershell
.\scripts\publish-wiki.ps1
```

Use `-NoPush` for a preview.
