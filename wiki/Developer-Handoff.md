# Developer Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #106 (`486f592`)
Date: 2026-09-07

## Current phase

**Billing/catalog lifecycle complete at application level; outbound webhooks next**

Delivered through PR #106:

- provider-neutral Stripe/Razorpay billing
- professional tenant plan/provider checkout UX
- signed durable provider webhooks
- webhook-driven subscription synchronization
- verified provider-aware period-end cancellation and stale-linkage recovery
- cross-provider overwrite protection and terminal-state repair
- durable TEST/LIVE provider catalog mappings
- automatic Stripe Product/Price provisioning and Price version replacement
- automatic Razorpay Plan provisioning and replacement mappings
- terminal safe plan retirement with existing paid-period entitlement preserved
- durable provider-retirement operations/retry
- immutable purchased-plan snapshots and tenant subscription history
- tenant/system-admin billing-history UX
- billing operations visibility/reconciliation
- durable usage metering, tenant API keys and plan-level API quotas

## Provider status

Stripe is the validated deployed Test Mode path. Razorpay application integration and managed Plan provisioning are implemented, but recurring Test Mode authorization remains externally provider-sandbox blocked.

Do not extend application billing just to work around the Razorpay sandbox.

## Plan/provider lifecycle

Application plans remain separate from provider objects but are linked durably per TEST/LIVE environment.

- `ACTIVE`: purchasable
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal/no new checkout; valid existing subscriptions remain entitled through their current period and are scheduled not to renew

Stripe economic edits create replacement Prices. Razorpay provider-visible edits create replacement Plans. Historical provider mappings remain retained for existing subscriptions and billing history.

## Preserve these boundaries

- authentication, tenant isolation, authorization, subscription access and quotas are independent
- checkout bypasses only lifecycle read-only enforcement and still requires tenant authorization
- provider webhooks remain authoritative for normal local lifecycle synchronization
- verified provider lookup/reconciliation may repair stale terminal local state
- provider IDs, API keys and secrets remain server-side
- purchased-plan and subscription-history snapshots are immutable historical records
- tenant API keys authenticate only `/api/external/**` and never impersonate users

## Database checkpoint

Common Flyway migrations extend through **V36**. Never rewrite an applied migration.

## Resume sequence

1. treat billing/catalog lifecycle as closed at application level
2. start tenant-configurable outbound webhooks
3. keep Stripe/Razorpay live readiness separate from feature development
4. then consider enterprise SSO, explain-access/delegation and deeper operational hardening

Recommended webhook sequence:

1. endpoint registration/event subscriptions/signing secrets + SSRF-safe URL validation
2. durable outbox/delivery engine, HMAC, retry/backoff, leasing and idempotency
3. project/task/collaboration/membership/subscription event integration + replay
4. tenant-admin endpoint and delivery-history UX

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana before merge.
