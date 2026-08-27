# Developer Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #90 (`e9257b3`)
Date: 2026-08-27

## Current phase

**Billing provider validation and operational hardening**

Delivered through PR #90:

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
- parallel Stripe and Razorpay choices in tenant checkout

## Working provider

Stripe is working end to end in deployed Test Mode. Hosted Checkout completes, the signed subscription webhook synchronizes local state, and the Stripe-enabled backend starts successfully after PR #90.

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

1. preserve and monitor the working Stripe Test Mode path
2. test a subscription created directly in Razorpay Dashboard
3. record `code`, `description`, `source`, `step` and `reason`
4. contact Razorpay Support if the direct provider flow fails
5. optionally build a feature-flagged, system-admin-only lifecycle simulator
6. rerun Razorpay activation, charged/pending/halted, cancellation, replay and reconciliation smoke tests
7. prepare live plans/keys only after provider-specific Test Mode validation succeeds

## Verification

GitHub Actions is authoritative while local Docker is unavailable. Stripe has also passed a deployed Test Mode smoke test. Mock provider tests validate application contracts, not Razorpay sandbox availability.
