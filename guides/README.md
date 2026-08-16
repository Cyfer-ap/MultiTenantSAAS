# Guide index

This directory contains the current focused project documentation plus historical references.

## Source-of-truth order

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical workflow/planning notes

Never modify an already-applied migration to make documentation match.

## Current focused guides

- `current_architecture.md`
- `data_model.md`
- `authorization_model.md`
- `subscription_billing.md`
- `postgresql_and_migrations.md`
- `step39_closeout.md`
- `step40_transaction_concurrency.md`

The current development phase is **Step 40 — Transaction & Concurrency Hardening**.

The repository also contains a production execution profile. Production deployments should combine the PostgreSQL and production profiles.
