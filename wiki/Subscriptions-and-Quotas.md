# Subscriptions and Quotas

Reviewed through PR #98 on 2026-09-06.

## Milestone status

**Billing & Payments is complete at application level.**

Provider live-readiness remains a separate deployment/operations concern.

## Separation of concerns

Keep application plans, stored subscription lifecycle, evaluated access, entitlements, usage and provider objects separate.

Application plan codes are mapped to provider plan/price IDs on the server. Provider IDs are not returned to the frontend.

Creating a new application plan through system administration does **not** automatically create Stripe Products/Prices or Razorpay Plans. A provider mapping must exist before that provider can offer checkout for the plan.

## Access and recovery

Ordinary tenant writes are rejected with `WORKSPACE_READ_ONLY` when lifecycle access is unavailable. Billing checkout is an explicitly permitted recovery action, but tenant authorization is still required.

Resource limits and per-period API limits are independent of lifecycle access.

## Billing implementation

- safe checkout configuration discovery
- professional plan/provider selection UX
- hosted Stripe and Razorpay subscription checkout
- signed durable webhooks with duplicate/replay protection
- lifecycle mapping for supported provider events
- provider-aware cancellation
- provider-linkage/history recovery
- cross-provider overwrite protection
- stale-terminal-state repair from verified provider state
- system-admin billing operations visibility
- read-only provider reconciliation
- append-only usage events
- tenant API-key creation/list/revocation
- API-key authentication only under `/api/external/**`
- plan-level `API_REQUESTS` quota enforcement with atomic consumption and `429 Retry-After`

Normal local provider-linked subscription state is webhook-authoritative.

## Current provider status

### Stripe

Stripe is working in deployed Test Mode. Hosted Checkout completes, signed subscription webhooks synchronize local state, and provider-side cancellation has been confirmed.

The final cancellation synchronization defect was configuration-related: the Stripe endpoint was missing `customer.subscription.deleted`. Stripe cancelled subscriptions correctly, but local state remained `ACTIVE`. The endpoint now subscribes to created/updated/deleted lifecycle events and PR #98 handles already-terminal provider state idempotently.

### Razorpay

Razorpay application integration is implemented, but Test Mode recurring authorization remains provider-sandbox blocked. Subscription creation and hosted redirect work; attempted test cards fail inside Razorpay before recurring authorization.

This external limitation does not invalidate application/CI coverage and does not keep the billing milestone open.

## Live-mode boundary

Neither Test Mode validation nor mocked provider contracts constitute a production/live-readiness claim. Live keys, live provider products/plans and production operational checks are a separate provider-specific track.
