# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-13
Base reviewed state: post-PR #125 (`0694403`)

## Current phase

**Authorization delegation and Explain Access — COMPLETE at application level**

Billing/catalog, tenant-configurable outbound webhooks and enterprise OIDC SSO remain closed application milestones. PRs #121, #122 and #125 complete the authorization follow-up: shared explain-access evaluation, bounded delegation with runtime non-escalation, direct/delegated provenance, and tenant Authorization UX.

## Delivered authorization sequence

- PR #121: structured authorization decisions and tenant-admin Explain Access API using the same evaluator as enforcement
- PR #122: V44 bounded authorization delegation, create/list/revoke lifecycle, provenance persistence, audit events and runtime parent-authority revalidation
- PR #125: delegation-safe reference data, direct-vs-delegated Explain Access provenance, Delegations/Explain Access UI and delegate-only workspace navigation

PRs #123 and #124 were dependency maintenance and are not part of the authorization capability sequence.

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

## Verification checkpoint

PR #125 passed Repository Hygiene, PostgreSQL/Flyway, Backend, Frontend formatting/tests/lint/build, Security, Container CI and Qodana on its final head before merge. Frontend coverage executed 71 test files / 240 tests successfully.

## Documentation/Wiki

`wiki/*.md` remains canonical Wiki source and is automatically published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Next platform milestone

Start **Production Operations & Disaster Recovery**:

1. PostgreSQL backup strategy and isolated restore drills
2. health/readiness and operational metrics
3. alerting for application/database/integration/provider failures
4. incident and recovery runbooks
5. broader load/failure-recovery and production R2 verification after the recovery baseline

Optional SAML/SCIM and notification expansion remain demand-driven work.
