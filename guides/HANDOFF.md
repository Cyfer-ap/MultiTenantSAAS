# Documentation Handoff

This file is intended to let a new ChatGPT session or developer resume work
without relying on chat history.

## Repository

Cyfer-ap/MultiTenantSAAS

## Documentation entry point

Start with:

`guides/README.md`

## Current phase

Step 40 - Transaction & Concurrency Hardening.

Slice 40.1 serializes subscription creation and mutable subscription state
transitions with database-backed pessimistic locking.

Remaining Step 40 areas include:

1. invitation acceptance/replacement races
2. failed-login counter races
3. session-version/password/logout-all lost-update protection
4. stable handling of database duplicate/integrity races
5. PostgreSQL concurrency integration tests and lock-order/deadlock review

## Historical generated documentation bundle

The root repository contains:

`multitenant-saas-guide-update.zip`

The corresponding manifest is:

`GUIDE_UPDATE_MANIFEST.md`

Use those when recovering the earlier guide-generation deliverable.
