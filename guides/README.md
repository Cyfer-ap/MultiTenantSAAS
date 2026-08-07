# Guide index

This directory contains both current project documentation and older implementation notes.

## Source-of-truth order

When documentation and implementation disagree, use this order:

1. current application code and tests
2. current Flyway migrations
3. the focused current guides listed below
4. older workflow, planning, and historical guides

Do not edit an already-applied migration to make a guide match it.

## Current focused guides

- `current_architecture.md` - backend/frontend boundaries and major platform layers
- `data_model.md` - current domain model and tenant/system separation
- `authorization_model.md` - authentication, tenant roles, permissions, policies, and enforcement layers
- `subscription_billing.md` - plans, subscriptions, access evaluation, quotas, and read-only behavior
- `postgresql_and_migrations.md` - H2/PostgreSQL profiles, Flyway layout, Testcontainers, and migration rules
- `step39_closeout.md` - Step 39 verification, fixes, and hand-off to Step 40

## Older guides

The remaining files are still useful for background, testing workflows, API examples, and implementation history, but some were written before the current authorization, subscription, and PostgreSQL architecture existed.

Treat these as reference material rather than authoritative architecture documents unless they agree with the current code and migrations.

## Migration rule from Step 39 onward

The repository currently has two migration histories:

- H2 historical migrations: `multitenant-saas/src/main/resources/db/migration`
- PostgreSQL current-schema baseline: `multitenant-saas/src/main/resources/db/postgresql`

Future portable schema changes begin with **V18** in:

`multitenant-saas/src/main/resources/db/common`

Both normal H2 execution and PostgreSQL execution must load `db/common`.

Never copy old V1-V17 migrations into `db/common`, and never rewrite an already-applied migration.

## Development checkpoint

Step 39 establishes the PostgreSQL production-readiness foundation and migration parity rules. It does not mean the application is production-complete.

The next development step is:

**Step 40 - Transaction & Concurrency Hardening**

That step should focus on transactional boundaries, race conditions, locking/idempotency where required, and consistency of quota/subscription-sensitive writes.
