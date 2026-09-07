# Development Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #106 (`486f592`)
Date: 2026-09-07

## Current phase

**Billing/catalog lifecycle complete at application level; outbound webhooks next**

Billing now includes provider-neutral orchestration, Stripe/Razorpay adapters, signed durable webhooks, webhook-driven lifecycle changes, professional checkout UX, verified provider cancellation/reconciliation, managed provider catalogs, safe terminal plan retirement, immutable purchased terms/history, durable metering, tenant API keys and API quotas.

Stripe remains the validated deployed Test Mode payment path. Razorpay application integration and managed Plan provisioning are complete, while recurring Test Mode authorization remains externally provider-sandbox blocked.

## Managed catalog boundary

Application plans and provider billing objects remain separate, but enabled managed providers are synchronized automatically through durable TEST/LIVE mappings.

- Stripe: Product + recurring Price creation; economic edits create replacement Prices.
- Razorpay: Plan creation; provider-visible edits create replacement Plans and archive prior local mappings.
- legacy configured provider IDs remain compatibility/import paths.

Plan states are `ACTIVE`, `INACTIVE` and terminal `RETIRED`. Retiring a plan blocks new checkout immediately while existing valid subscriptions retain their purchased entitlement through the current period and provider renewals are scheduled to stop at period/cycle end.

Never place real credentials or provider IDs in docs, commits, frontend configuration or screenshots.

## History boundary

Purchased terms and V36 subscription-history rows are immutable historical records. Tenant-facing history hides internal provider references; system-admin history retains provider identifiers for operations.

## Boundaries

- webhook lifecycle state is authoritative in normal operation
- verified provider lookup/reconciliation may repair stale terminal state
- checkout is a lifecycle recovery action but still requires tenant authorization
- provider identifiers and credentials stay server-side
- API keys remain tenant-bound and restricted to `/api/external/**`
- never hard-delete used plans/provider references merely to remove them from future sale

## Documentation/Wiki

Current root docs, focused guides and version-controlled Wiki source are refreshed through PR #106. `wiki/*.md` is canonical and publishes automatically from merged `main`.

## Resume steps

1. treat billing/catalog lifecycle as closed at application level
2. do not continue Razorpay sandbox debugging as core feature work
3. begin tenant-configurable outbound webhooks
4. keep provider live-mode readiness as an independent deployment/operations track
5. preserve full CI requirements on every PR

## Verification

Require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana before merge. Relevant Wiki changes must also pass `Wiki Sync / Validate Wiki Source`.
