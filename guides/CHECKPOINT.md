# MultiTenantSAAS Documentation Recovery Checkpoint

Date: 2026-08-16

Repository: Cyfer-ap/MultiTenantSAAS

## Important discovery

The repository currently contains the original generated documentation archive:

`multitenant-saas-guide-update.zip`

It also contains:

`GUIDE_UPDATE_MANIFEST.md`

The original manifest says the generated update bundle contains/replaces:

- `readme.md`
- `guides/progress.md`
- `guides/Plan.txt`
- `guides/security_model.md`
- `multitenant-saas-frontend/README.md`

and adds:

- `guides/frontend_architecture.md`
- `guides/frontend_testing.md`
- `guides/authorization_v2_plan.md`

## Current documentation state

The repository has since advanced beyond that original snapshot.

Current authoritative guide index:

`guides/README.md`

The guide index states that the current development phase is:

**Step 40 - Transaction & Concurrency Hardening**

and that current focused documentation includes:

- `guides/current_architecture.md`
- `guides/data_model.md`
- `guides/authorization_model.md`
- `guides/subscription_billing.md`
- `guides/postgresql_and_migrations.md`
- `guides/step39_closeout.md`
- `guides/step40_transaction_concurrency.md`

## Source-of-truth rule

Use this order when documentation and implementation disagree:

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical workflow/planning guides

## Migration checkpoint

The repository has separate H2 historical and PostgreSQL baseline histories.
Future portable schema changes begin with V18 in `db/common`.

Never rewrite an already-applied migration.

## Recovery recommendation

Use the repository's existing `multitenant-saas-guide-update.zip` when you need
the exact earlier generated files.

For ongoing development, use `guides/README.md` and the current focused guides
instead of treating the old `progress.md` snapshot as authoritative.
