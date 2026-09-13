# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-13
Base reviewed state: post-PR #126 (`5013260`)

## Current phase

**Authorization delegation and Explain Access — COMPLETE at application level**

Billing/catalog, tenant-configurable outbound webhooks and enterprise OIDC SSO remain closed application milestones. PRs #121, #122 and #125 complete the authorization follow-up: shared explain-access evaluation, bounded delegation with runtime non-escalation, direct/delegated provenance, and tenant Authorization UX. PR #126 closed the milestone documentation.

## Delivered authorization sequence

- PR #121: structured authorization decisions and tenant-admin Explain Access API using the same evaluator as enforcement
- PR #122: V44 bounded authorization delegation, create/list/revoke lifecycle, provenance persistence, audit events and runtime parent-authority revalidation
- PR #125: delegation-safe reference data, direct-vs-delegated Explain Access provenance, Delegations/Explain Access UI and delegate-only workspace navigation
- PR #126: documentation/checkpoint closure and roadmap handoff

## Authorization invariants

- tenant isolation remains mandatory before authorization evaluation
- backend authorization remains authoritative; frontend guards are UX only
- Explain Access uses the same evaluator as enforcement rather than a parallel permission model
- every delegated grant has one explicit direct parent authority assignment
- delegated authority must remain a permission, scope and validity subset of its current direct source
- delegated assignments cannot be re-delegated
- `authorization.manage` and `authorization.delegate` cannot be delegated
- unsupported delegation sources/scopes remain rejected rather than approximated
- parent authority is revalidated at access time; revoked, expired, inactive or narrowed source authority invalidates the child grant
- revoking a delegation deactivates the generated assignment
- Explain Access exposes only the matched grant/provenance required to explain the decision

## Authorization workspace

Managers with `authorization.manage` can use:

```text
/authorization/manage
/authorization/delegations
/authorization/explain
```

Users with `authorization.delegate` but not `authorization.manage` can enter the Authorization workspace and use Delegations only.

Delegation UI supports bounded create/list/status/expiry/revoke behavior. Explain Access distinguishes `DIRECT` from `DELEGATED` grant source and, for delegated grants, exposes delegation/parent/delegator provenance.

## Database checkpoint

Portable common migrations extend through **V44**.

Recent milestone migrations:

```text
V40 tenant identity-provider configuration
V41 OIDC authorization transactions + tenant federated identities
V42 tenant SSO policy
V43 OIDC browser session handoffs
V44 authorization delegation provenance + authorization.delegate permission
```

Never rewrite an applied migration.

## Billing/provider status

### Stripe

**Working and validated in deployed Test Mode.** Hosted checkout, signed lifecycle webhooks, provider-side cancellation and reconciliation are implemented and validated. Managed Product/Price provisioning remains implemented.

### Razorpay

**Application integration/catalog provisioning implemented; recurring Test Mode authorization remains provider-sandbox blocked.** Keep Razorpay available; live/provider readiness remains separate from core application completeness.

## Product-core gap checkpoint

The platform foundation is now broad. The largest remaining core gaps are user-facing rather than tenancy/billing/authorization plumbing:

- global search + command palette
- favorites/recent items and saved views
- My Work / unified attention queue
- role/capability-aware dashboard
- richer task views: Kanban/calendar, subtasks, dependencies, labels and recurring work
- project/task templates
- custom fields/forms
- workflow/approval automation
- knowledge/documents beyond task attachments
- user-facing analytics/reporting
- import/export and bulk productivity
- smoother multi-workspace switching/personalization

See `guides/Wild_Thoughts.md` for the audited feature vault and differentiated experiments.

## Verification checkpoint

PR #125 passed Repository Hygiene, PostgreSQL/Flyway, Backend, Frontend formatting/tests/lint/build, Security, Container CI and Qodana on its final head before merge. Frontend coverage executed 71 test files / 240 tests successfully. PR #126 passed CI, Security, Qodana and Wiki validation before merge.

## Documentation/Wiki

`wiki/*.md` remains canonical Wiki source and is automatically published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Next product milestone

Start **Product Experience & Work Management Enrichment**.

Recommended progression:

1. global search + command palette + favorites/recent items
2. My Work + saved views + capability-aware dashboard
3. Kanban/calendar and richer task relationships such as subtasks/dependencies/labels
4. recurring work/project templates and practical bulk/import/export UX
5. custom fields/forms, workflow/approval and knowledge/document capabilities
6. analytics plus selected differentiated experiments from `guides/Wild_Thoughts.md`

**Production Operations & Disaster Recovery is intentionally deferred from the immediate sequence** while user-facing product depth is expanded. It remains an important later milestone together with load/failure-recovery and production R2 verification. Optional SAML/SCIM and notification expansion remain demand-driven work.
