# Subscription and billing

Reviewed through PR #84 on 2026-08-26.

## Domain boundary

The platform keeps application plans, tenant subscription state, evaluated access, entitlements, quotas and payment-provider objects separate.

Application plans are stored in the platform database. Razorpay plans are created in Razorpay and mapped server-side by app plan code:

```text
PRO        -> RAZORPAY_PLAN_PRO
ENTERPRISE -> RAZORPAY_PLAN_ENTERPRISE
```

The frontend receives plan display data and enabled provider names only. It never receives provider plan IDs, API secrets or webhook secrets.

## Implemented billing flow

- `GET /api/tenants/{tenantId}/billing/checkout/configuration`
- `POST /api/tenants/{tenantId}/billing/checkout`
- hosted Stripe and Razorpay subscription checkout adapters
- `POST /api/billing/webhooks/stripe`
- `POST /api/billing/webhooks/razorpay`
- exact raw-body signature verification, durable events and duplicate/replay protection
- webhook-driven subscription lifecycle mapping
- `POST /api/tenants/{tenantId}/billing/cancel`
- system-admin billing subscription/event views
- `POST /api/system/billing/subscriptions/{tenantId}/reconcile` read-only comparison
- append-only usage events and plan-level `API_REQUESTS` limits
- tenant API keys authenticated only under `/api/external/**`

Razorpay lifecycle events handled include activated, charged, updated, pending, halted, cancelled and completed.

Local subscription state is webhook-authoritative. Cancellation returns accepted and waits for the signed provider event.

## Read-only recovery

Ordinary tenant mutations are blocked when subscription access is read-only. Billing checkout is explicitly allowed as a recovery action, while tenant authorization remains required.

## Razorpay configuration

```dotenv
RAZORPAY_BILLING_ENABLED=true
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
RAZORPAY_PLAN_PRO=plan_...
RAZORPAY_PLAN_ENTERPRISE=plan_...
RAZORPAY_SUBSCRIPTION_TOTAL_COUNT=120
RAZORPAY_WEBHOOK_ENABLED=true
RAZORPAY_WEBHOOK_SECRET=...
```

Use Test Mode keys with Test Mode plan IDs. Do not mix modes. Do not commit values.

Deployed webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

A browser GET to that route is not a webhook test.

## Current Razorpay blocker

**The real Test Mode subscription flow has not succeeded.**

The application reaches Razorpay's hosted checkout, but all attempted cards fail before recurring authorization. International cards are rejected because international acceptance is unavailable; domestic recurring-compatible test cards have also produced provider failures. As a result, activation webhook delivery and local state synchronization have not been proven against the live Razorpay sandbox.

Application CI uses mocked provider HTTP contracts and remains green, but that is not equivalent to provider E2E.

## Diagnostic sequence

1. create and open a subscription directly from Razorpay Dashboard using the same Test Mode plan
2. record the failed payment fields: `code`, `description`, `source`, `step`, `reason`
3. if the direct flow fails, give those values and the Test Mode payment/subscription IDs to Razorpay Support
4. if the direct flow succeeds, compare its plan/account/settings with the API-created subscription
5. rerun activation, charged, pending/halted, cancellation, webhook replay and reconciliation tests

Keep live keys and live plans deferred until this succeeds. A feature-flagged system-admin simulator may be added for application lifecycle testing, but it must never be represented as provider validation.
