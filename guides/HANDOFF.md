# Development Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`  
Default branch: `main`  
Reviewed state: post-PR #84 (`b97a3ef`)  
Date: 2026-08-26

## Current phase

**Razorpay Test Mode validation**

The billing implementation now includes provider-neutral orchestration, Stripe/Razorpay adapters, signed durable webhooks, webhook-driven lifecycle changes, provider cancellation, operational views, reconciliation, durable metering, tenant API keys, API quotas and tenant hosted checkout.

## Important status

Razorpay is not yet working end to end. Checkout creation and redirect succeed, but every attempted card fails on Razorpay's hosted page before recurring authorization. This blocks a real activation webhook and local subscription activation test. Keep live mode deferred.

## Server configuration

Internal plans are not imported from Razorpay. Map app codes to provider plans only on the server:

```text
PRO        -> RAZORPAY_PLAN_PRO
ENTERPRISE -> RAZORPAY_PLAN_ENTERPRISE
```

Correct webhook URL:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Never place real credentials in docs, commits, frontend configuration or screenshots. Rotate exposed credentials.

## Boundaries

- webhook state is authoritative
- reconciliation reports mismatches but does not overwrite local state
- checkout is a lifecycle recovery action but still requires tenant authorization
- provider identifiers and credentials stay server-side
- API keys remain tenant-bound and restricted to `/api/external/**`

## Resume steps

1. create a test subscription directly from Razorpay Dashboard
2. inspect the failed payment's `code`, `description`, `source`, `step` and `reason`
3. contact Razorpay Support if the dashboard-native flow fails
4. optionally add a feature-flagged, system-admin-only billing simulator
5. repeat deployed activation, webhook, reconciliation and cancellation tests
6. enable live plans only after the full Test Mode lifecycle succeeds

## Verification

GitHub Actions is authoritative while local Docker is unavailable. Provider contract mocks do not count as real Razorpay E2E validation.
