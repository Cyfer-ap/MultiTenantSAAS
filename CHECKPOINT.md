# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-06
Base reviewed state: post-PR #98 (`87319f8`)

## Current phase

**Billing & Payments — COMPLETE at application level**

The application-level billing milestone is closed. Provider-neutral checkout, Stripe/Razorpay adapters, signed durable webhooks, webhook-driven lifecycle synchronization, cancellation, reconciliation, usage metering, tenant API keys, API request quotas and the professional tenant subscription UX are implemented.

Live-provider readiness remains a separate deployment/operations concern.

## Delivered billing sequence

- PRs #67-#73: billing foundation, Stripe/Razorpay adapters, checkout API, signed durable webhooks and lifecycle synchronization
- PR #74: provider-backed tenant cancellation
- PRs #75-#76: billing operations visibility and read-only reconciliation
- PR #77: durable usage metering
- PRs #78-#79: tenant API-key lifecycle, authentication and metering
- PR #80: plan-level external API quotas
- PRs #81-#82: checkout discovery and tenant hosted-checkout UI
- PR #83: Razorpay provider startup injection fix
- PR #84: checkout recovery for read-only workspaces
- PRs #88-#90: provider status/docs, parallel Stripe integration and Stripe startup fix
- PR #91: billing checkpoint consolidation
- PR #92: professional subscription purchase UX
- PR #93: Tomcat/security hardening
- PR #94: billing lifecycle/security regression coverage
- PR #95: provider-aware cancellation recovery
- PR #96: recovery from verified provider history and cross-provider linkage protection
- PR #97: Stripe period-end cancellation API correction
- PR #98: idempotent reconciliation for already-cancelled provider subscriptions

## Stripe status

**Working in deployed Test Mode.**

Confirmed:

- hosted subscription Checkout works
- successful Stripe Test Mode card payments work
- signed subscription webhooks synchronize local state
- webhook endpoint is subscribed to `customer.subscription.created`, `customer.subscription.updated` and `customer.subscription.deleted`
- cancellation reaches Stripe
- cancellation state is repaired idempotently if local state is stale
- reconciliation remains available

The cancellation incident was caused by the Stripe webhook endpoint initially omitting `customer.subscription.deleted`: Stripe had cancelled the subscriptions correctly, but the application remained locally `ACTIVE`. The endpoint configuration was corrected and PR #98 hardened repeat cancellation/reconciliation.

This is a Test Mode application-validation statement, not a production/live-mode readiness claim.

## Razorpay status

**Application integration implemented; Test Mode recurring authorization remains provider-sandbox blocked.**

Confirmed:

- provider configuration loads
- hosted checkout creation works
- browser reaches Razorpay Test Mode checkout
- provider adapter, signatures, lifecycle mapping and cancellation paths are covered by application tests

External blocker:

- attempted Razorpay Test Mode cards fail before recurring authorization completes
- international-card acceptance is unavailable in the current sandbox/account path
- domestic recurring-compatible test attempts have also failed

Do not treat this external provider sandbox behavior as unfinished application architecture. Keep Razorpay available in code and defer live-provider readiness until a separate provider review.

## Plan/provider boundary

Application plans are stored in the platform database. Provider billing objects are separate.

Current Stripe and Razorpay checkout mappings are server-side configuration mappings. Creating a new application plan through system administration does **not** automatically provision a Stripe Product/Price or Razorpay Plan. A new paid plan must have a matching provider mapping before checkout can be offered through that provider.

Provider provisioning/synchronization is a possible future feature and is outside the closed billing milestone.

## Database checkpoint

Portable migrations extend through **V33**:

```text
V28 billing foundation
V29 provider subscription linkage
V30 durable billing usage events
V31 tenant API keys
V32 API-key last-used metadata
V33 subscription-plan usage limits
```

Never rewrite an applied migration.

## Verification checkpoint

Billing hardening through PR #98 passed the repository's required CI families across the sequence: backend, PostgreSQL/Flyway, frontend, repository hygiene, security/Trivy, containers and Qodana. Deployed Stripe Test Mode checkout, webhook synchronization and provider-side cancellation were additionally validated.

Mocked provider-contract tests prove application behavior; they do not prove Razorpay sandbox availability.

## Documentation closure

Current README, root checkpoint/handoff files, focused guides, Wiki source, roadmap, testing/deployment pages and documentation manifests are refreshed to this checkpoint.

`wiki/*.md` is the canonical Wiki source. `.github/workflows/wiki-sync.yml` validates Wiki changes on pull requests and automatically publishes merged `main` changes to the live GitHub Wiki with `scripts/publish-wiki.ps1`. Manual Wiki publishing is now a fallback rather than a normal release step.

Historical planning files remain intentionally historical and are not rewritten as current specifications.

## Next platform milestone

Billing is no longer the active development phase. The next major product milestone should be selected from the current roadmap. Recommended next feature: **tenant-configurable outbound webhooks**, followed by enterprise SSO, authorization delegation/explain-access and deeper operational recovery/load testing.
