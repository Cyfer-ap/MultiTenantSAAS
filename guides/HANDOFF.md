# Development Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #98 (`87319f8`)
Date: 2026-09-06

## Current phase

**Billing & Payments complete at application level; next product milestone selection**

The billing implementation includes provider-neutral orchestration, Stripe/Razorpay adapters, signed durable webhooks, webhook-driven lifecycle changes, professional checkout UX, provider cancellation, stale-linkage recovery, operational views, reconciliation, durable metering, tenant API keys and API quotas.

Stripe works in deployed Test Mode for hosted Checkout, signed lifecycle synchronization and provider-side cancellation. The final stale cancellation state was traced to the Stripe webhook endpoint missing `customer.subscription.deleted`; that event is now enabled and PR #98 adds idempotent repair when Stripe is already terminal but local state is stale.

## Razorpay status

Razorpay remains externally blocked at Test Mode recurring authorization. Checkout creation and redirect work, but attempted sandbox cards fail within Razorpay before recurring authorization. Keep the adapter in place and treat live readiness as separate provider work.

## Server configuration boundary

Application plans are not imported from payment providers, and creating a system-admin plan does not automatically create provider billing objects.

Current checkout uses server-side mappings from application plan codes to Stripe Price IDs and Razorpay Plan IDs. New paid plans require explicit provider mappings before provider checkout can be offered.

Never place real credentials or provider IDs in docs, commits, frontend configuration or screenshots. Rotate exposed credentials.

## Boundaries

- webhook lifecycle state is authoritative in normal operation
- verified provider lookup/reconciliation may repair stale terminal state
- checkout is a lifecycle recovery action but still requires tenant authorization
- provider identifiers and credentials stay server-side
- API keys remain tenant-bound and restricted to `/api/external/**`

## Documentation/Wiki

Current root docs, focused guides and version-controlled Wiki source have been refreshed to the billing-closure checkpoint.

`wiki/*.md` is canonical. `.github/workflows/wiki-sync.yml` validates relevant pull requests and automatically publishes merged `main` Wiki changes using `scripts/publish-wiki.ps1`. Manual publishing is fallback-only.

## Resume steps

1. treat Billing & Payments as closed at application level
2. do not continue provider-sandbox debugging as core feature work
3. begin the next product milestone, recommended: tenant-configurable outbound webhooks
4. keep provider live-mode readiness as an independent deployment/operations track
5. preserve full CI requirements on every PR

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Require backend, PostgreSQL/Flyway, frontend, repository hygiene, security, containers and Qodana before merge. Relevant Wiki changes must also pass `Wiki Sync / Validate Wiki Source`.
