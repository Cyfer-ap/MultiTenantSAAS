# Subscriptions and Quotas

Reviewed through PR #90 on 2026-08-27.

## Separation of concerns

Keep application plans, stored subscription lifecycle, evaluated access, entitlements, usage and provider objects separate.

Application plan codes are mapped to provider plan IDs on the server. Razorpay plans are not imported into the application plan catalogue and provider IDs are not returned to the frontend.

## Access and recovery

Ordinary tenant writes are rejected with `WORKSPACE_READ_ONLY` when lifecycle access is unavailable. Billing checkout is an explicitly permitted recovery action, but tenant authorization is still required.

Resource limits and per-period API limits are independent of lifecycle access.

## Billing implementation

- safe checkout configuration discovery
- hosted Stripe and Razorpay subscription checkout
- signed durable webhooks with duplicate/replay protection
- lifecycle mapping for supported provider events
- provider-backed cancellation
- system-admin billing operations visibility
- read-only provider reconciliation
- append-only usage events
- tenant API-key creation/list/revocation
- API-key authentication only under `/api/external/**`
- plan-level `API_REQUESTS` quota enforcement with atomic consumption and `429 Retry-After`

Local provider-linked subscription state is webhook-authoritative.

## Razorpay mapping

```text
PRO        -> RAZORPAY_PLAN_PRO
ENTERPRISE -> RAZORPAY_PLAN_ENTERPRISE
```

Use Test Mode keys with Test Mode plans. Keep secrets and provider IDs server-side.

## Current provider status

Stripe is **working end to end in deployed Test Mode**. Hosted Checkout completes and the signed subscription webhook synchronizes local subscription state.

Razorpay is **not validated end to end**. Subscription creation and hosted redirect work, but all attempted test cards fail within Razorpay before recurring authorization. Activation webhooks and local activation remain unproven against the real sandbox.

This external blocker does not invalidate CI's application and mock-provider coverage, but it prevents a production-readiness claim.
