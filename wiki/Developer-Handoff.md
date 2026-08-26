# Developer Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #84 (`b97a3ef`)
Date: 2026-08-26

## Current phase

**Razorpay Test Mode validation and billing operational hardening**

Delivered through PR #84:

- provider-neutral billing and Stripe/Razorpay adapters
- tenant hosted checkout
- signed durable provider webhooks
- webhook-driven subscription synchronization
- provider cancellation
- billing event/subscription views and read-only reconciliation
- durable usage metering
- tenant API keys and isolated external API authentication
- per-plan API-request quotas
- subscription-page checkout and read-only recovery
- deployed startup regression fix

## Blocking fact

Razorpay is not working end to end. The hosted Test Mode page opens, but all attempted cards fail inside Razorpay before recurring authorization. No real activation webhook/local activation cycle has succeeded. Live mode remains deferred.

## Preserve these boundaries

- authentication, tenant isolation, authorization, subscription access and quotas are independent
- checkout bypasses only lifecycle read-only enforcement and still requires `tenant.update`
- provider webhooks remain authoritative for local subscription state
- reconciliation reports mismatches without overwriting state
- provider plan IDs, API keys and secrets remain server-side
- tenant API keys authenticate only `/api/external/**` and never impersonate users

## Resume sequence

1. test a subscription created directly in Razorpay Dashboard
2. record `code`, `description`, `source`, `step` and `reason`
3. contact Razorpay Support if the direct provider flow fails
4. optionally build a feature-flagged, system-admin-only lifecycle simulator
5. rerun activation, charged/pending/halted, cancellation, replay and reconciliation smoke tests
6. prepare live plans/keys only after Test Mode succeeds

## Verification

GitHub Actions is authoritative while local Docker is unavailable. Mock provider tests validate application contracts, not Razorpay sandbox availability.
