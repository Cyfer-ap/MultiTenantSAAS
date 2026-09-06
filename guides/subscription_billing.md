# Subscription and billing

Reviewed through PR #98 on 2026-09-06.

## Milestone status

**Billing & Payments is complete at application level.**

Live-provider readiness remains a separate deployment/operations concern.

## Domain boundary

The platform keeps application plans, tenant subscription state, evaluated access, entitlements, quotas and payment-provider objects separate.

Application plans are stored in the platform database. Stripe Products/Prices and Razorpay Plans are separate provider objects. The frontend receives plan display data and enabled provider names only; it never receives provider plan IDs, API secrets or webhook secrets.

Creating a new application plan through system administration does **not** automatically create Stripe Products/Prices or Razorpay Plans. Current checkout requires server-side mappings from application plan code to provider billing ID. Automatic provider provisioning can be designed later as a separate feature.

## Implemented billing flow

- `GET /api/tenants/{tenantId}/billing/checkout/configuration`
- `POST /api/tenants/{tenantId}/billing/checkout`
- professional plan selection → provider selection → hosted checkout UX
- hosted Stripe and Razorpay subscription checkout adapters
- `POST /api/billing/webhooks/stripe`
- `POST /api/billing/webhooks/razorpay`
- exact raw-body signature verification, durable events and duplicate/replay protection
- webhook-driven subscription lifecycle mapping
- cross-provider linkage protection and verified-history recovery
- `POST /api/tenants/{tenantId}/billing/cancel`
- provider-aware cancellation and stale-terminal-state repair
- system-admin billing subscription/event views
- `POST /api/system/billing/subscriptions/{tenantId}/reconcile` read-only comparison
- append-only usage events and plan-level `API_REQUESTS` limits
- tenant API keys authenticated only under `/api/external/**`

Local subscription state is webhook-authoritative during normal operation. Verified provider lookup/reconciliation may repair stale terminal state when a provider cancellation already succeeded but its webhook was missed.

## Read-only recovery

Ordinary tenant mutations are blocked when subscription access is read-only. Billing checkout is explicitly allowed as a recovery action, while tenant authorization remains required.

## Stripe configuration

Stripe can be enabled without disabling Razorpay. Stripe uses server-created hosted Checkout Sessions in `subscription` mode.

Example server-side configuration:

```dotenv
STRIPE_BILLING_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PRICE_PRO=price_...
STRIPE_PRICE_ENTERPRISE=price_...
STRIPE_SUCCESS_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=success
STRIPE_CANCEL_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=cancelled
STRIPE_WEBHOOK_ENABLED=true
STRIPE_WEBHOOK_SECRET=whsec_...
```

Register the Test Mode webhook endpoint:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Subscribe it to all lifecycle events currently required by the backend:

```text
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

The webhook signing secret is separate from the Stripe API secret key.

### Stripe validation result

The deployed Stripe Test Mode subscription flow is working. Hosted Checkout completes with Test Mode cards, signed subscription webhooks synchronize local state, and provider-side cancellation was confirmed.

During final cancellation testing, Stripe correctly cancelled subscriptions but the application remained `ACTIVE`. The Stripe webhook endpoint had not been subscribed to `customer.subscription.deleted`, so the terminal event was never delivered. The endpoint configuration was corrected, and PR #98 added an idempotent provider-state repair path for already-cancelled subscriptions.

This is a Test Mode application-validation statement, not live-mode readiness.

## Razorpay configuration

Example server-side configuration:

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

### Razorpay validation result

The application integration is implemented, but real Test Mode recurring authorization remains provider-sandbox blocked. Hosted checkout opens, while attempted sandbox cards fail inside Razorpay before recurring authorization completes. International-card acceptance is unavailable in the current path, and domestic recurring-compatible test attempts have also failed.

Keep Razorpay available in code. Do not hold the application billing milestone open because of this external provider limitation.

## Provider plan mappings

Current provider mappings are configuration-driven. Conceptually:

```text
Application plan code
    ├─ Stripe   -> recurring Price ID
    └─ Razorpay -> Plan ID
```

The platform does not yet provision or synchronize provider Products/Prices/Plans when a system administrator creates or edits an application plan. If implemented later, provider provisioning must handle partial failure, idempotency, immutable Stripe Price semantics, provider-specific lifecycle, rollback/compensation and live/test-mode separation.

## Closure decision

Billing should not receive additional application features merely because Razorpay Test Mode refuses cards. Future payment work is limited to genuine regressions, live-readiness work when required, or explicitly scoped enhancements such as automatic provider-plan provisioning.
