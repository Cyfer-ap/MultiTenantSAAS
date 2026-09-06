# Developer Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #98 (`87319f8`)
Date: 2026-09-06

## Current phase

**Billing & Payments complete at application level; next product milestone selection**

Delivered through PR #98:

- provider-neutral Stripe/Razorpay billing
- professional tenant plan/provider checkout UX
- signed durable provider webhooks
- webhook-driven subscription synchronization
- provider-aware cancellation and stale-linkage recovery
- cross-provider overwrite protection
- idempotent repair for already-terminal provider subscriptions
- billing event/subscription views and read-only reconciliation
- durable usage metering
- tenant API keys and isolated external API authentication
- per-plan API-request quotas
- subscription recovery checkout for read-only workspaces

## Stripe

Stripe is working in deployed Test Mode. Hosted Checkout completes, signed lifecycle webhooks synchronize local state, and provider-side cancellation was confirmed.

The final cancellation discrepancy was caused by the Stripe webhook endpoint not subscribing to `customer.subscription.deleted`. Stripe cancelled correctly but local state remained `ACTIVE`. The endpoint is now configured for created/updated/deleted subscription events, and PR #98 handles already-terminal provider state idempotently.

## Razorpay

Razorpay application integration is implemented, but hosted Test Mode recurring authorization remains externally blocked. Attempted sandbox cards fail within Razorpay before recurring authorization. Keep live mode deferred; do not treat this provider limitation as unfinished application billing work.

## Plan/provider boundary

Application plan creation does not automatically provision Stripe Products/Prices or Razorpay Plans. Provider checkout requires explicit server-side mappings. Automatic provider provisioning is future optional work.

## Preserve these boundaries

- authentication, tenant isolation, authorization, subscription access and quotas are independent
- checkout bypasses only lifecycle read-only enforcement and still requires `tenant.update`
- provider webhooks remain authoritative for normal local lifecycle synchronization
- verified provider lookup may repair stale terminal local state
- provider plan/price IDs, API keys and secrets remain server-side
- tenant API keys authenticate only `/api/external/**` and never impersonate users

## Resume sequence

1. consider Billing & Payments closed at application level
2. start tenant-configurable outbound webhooks as the recommended next product milestone
3. keep Stripe/Razorpay live readiness separate from feature development
4. subsequently consider enterprise SSO, explain-access/delegation and deeper operational hardening

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Require backend, PostgreSQL/Flyway, frontend, repository hygiene, security, container and Qodana checks before merge.
