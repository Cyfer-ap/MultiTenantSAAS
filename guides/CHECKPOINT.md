# Guides Checkpoint

Snapshot date: 2026-09-13
Reviewed state: post-PR #125 (`0694403`)

## Completed application milestones

- billing/catalog lifecycle through PR #106
- tenant-configurable outbound webhooks through PR #112
- enterprise OIDC SSO / identity federation through PR #119
- authorization delegation and Explain Access through PR #125

## Authorization checkpoint

PRs #121, #122 and #125 deliver structured access decisions, Explain Access, V44 durable delegation provenance, bounded create/list/revoke delegation lifecycle, audit events, runtime source revalidation, direct/delegated provenance and a permission-gated Authorization workspace.

Core invariant:

```text
delegated authority ⊆ delegator's current direct authority
```

The backend evaluator remains authoritative. Delegated grants cannot be re-delegated, protected authorization permissions cannot be delegated, and parent authority is revalidated on every relevant access decision.

Portable migrations extend through **V44**. Never rewrite an applied migration.

## Provider status

- Stripe: working and validated in deployed Test Mode
- Razorpay: application/catalog integration implemented; recurring Test Mode authorization remains provider-sandbox blocked

## Next checkpoint

The next product checkpoint belongs to **Production Operations & Disaster Recovery**: backup/restore drills, monitoring, alerts and operational runbooks.

Code/tests and current migrations remain authoritative over historical planning documents.
