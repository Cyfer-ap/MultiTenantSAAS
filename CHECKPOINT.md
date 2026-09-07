# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-07
Base reviewed state: post-PR #106 (`486f592`)

## Current phase

**Billing, cancellation hardening and managed provider catalogs — COMPLETE at application level**

The billing milestone is closed through PR #106. Provider-neutral checkout, Stripe/Razorpay adapters, signed durable webhooks, lifecycle synchronization, cancellation/reconciliation, metering, API keys/quotas, managed provider catalogs, safe plan retirement, immutable purchased terms/history and tenant/system-admin billing history UX are implemented.

Live-provider readiness remains a separate deployment/operations concern.

## Delivered extension sequence

The earlier billing foundation/hardening remains intact through PR #99. The managed-catalog lifecycle extension is:

- PR #100: `ACTIVE`/`INACTIVE`/`RETIRED`, provider mappings and immutable purchased-plan snapshots
- PR #101: automatic Stripe Product/Price provisioning and immutable Price replacement
- PR #102: safe provider-aware plan retirement and period-end cancellation orchestration
- PR #103: production startup fix/regression test for Stripe retirement provisioner
- PR #104: immutable tenant subscription history backend and paginated tenant/system-admin APIs
- PR #105: billing history UI and terminal retired-plan lifecycle UX
- PR #106: automatic Razorpay Plan provisioning/versioning, durable mappings and legacy compatibility

## Provider status

### Stripe

**Validated deployed Test Mode path.** Hosted checkout, Test cards, signed lifecycle webhooks, provider-side cancellation and reconciliation have been validated. The webhook endpoint includes `customer.subscription.created`, `customer.subscription.updated` and `customer.subscription.deleted`.

### Razorpay

**Application integration/catalog provisioning implemented; recurring Test Mode authorization remains provider-sandbox blocked.** Plan management and checkout integration are implemented, but sandbox cards fail before recurring authorization completes. Keep Razorpay available and keep live-readiness separate.

## Managed catalog lifecycle

Application plans remain separate from provider objects but are now linked through durable TEST/LIVE provider mappings.

- Stripe: create Product + recurring Price; economic edits create replacement Prices; retirement deactivates active catalog objects for new sales while preserving historical references.
- Razorpay: create provider Plan; provider-visible edits create replacement Plans and archive prior local mappings because Razorpay does not expose the same plan-update/deactivation lifecycle.
- legacy configured provider IDs remain supported as compatibility/import paths.

`RETIRED` immediately prevents new checkout while valid existing subscriptions retain purchased entitlements through their current period. Provider subscriptions are scheduled to end at the provider period/cycle boundary. Normal local lifecycle remains webhook-authoritative.

## Immutable history

V34 purchased-plan snapshots preserve the terms a tenant bought independently of later catalog edits. V36 adds immutable tenant-subscription history, exposed to tenant users and system admins. Tenant-facing history hides internal provider references; system-admin history retains operational provider identifiers.

## Database checkpoint

Portable migrations extend through **V36**:

```text
V28 billing foundation
V29 provider subscription linkage
V30 durable billing usage events
V31 tenant API keys
V32 API-key last-used metadata
V33 subscription-plan usage limits
V34 provider catalog mappings + purchased-plan snapshots
V35 durable plan-retirement operations
V36 immutable tenant subscription history
```

Never rewrite an applied migration.

## Verification checkpoint

The #100–#106 sequence was developed under the repository's Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security/Trivy, Container CI and Qodana gates. The production-startup regression introduced by #102 was isolated and fixed in #103; the recovered deployment was subsequently confirmed healthy.

Mock/provider-contract tests prove application behavior but do not prove Razorpay sandbox availability or live-provider readiness.

## Documentation/Wiki

This checkpoint supersedes the post-#98/#99 provider-plan boundary language. `wiki/*.md` remains canonical Wiki source and is automatically published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Next platform milestone

Start **tenant-configurable outbound webhooks**. Recommended sequence: endpoint/signing foundation → durable delivery engine → domain event integration/replay → tenant-admin delivery UX. Enterprise SSO follows after outbound integrations.
