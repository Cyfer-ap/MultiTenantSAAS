# Production Deployment

Reviewed through PR #98 on 2026-09-06.

## Current hosted topology

```text
Render Static Site (frontend)
        |
        v
Render Web Service (backend)
        |
        v
Neon PostgreSQL 17
```

Frontend: `https://multitenantsaas-frontend.onrender.com`
Backend: `https://multitenantsaas-akxn.onrender.com`

Use `SPRING_PROFILES_ACTIVE=postgres,production`.

## Billing deployment boundary

Billing & Payments is complete at application level, but Test Mode validation is not a live-production readiness claim. Provider live keys/plans/products and production operational checks remain separate work.

## Stripe Test Mode

Stripe can run beside Razorpay.

Example server-side Test Mode variables:

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

Register:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Required enabled subscription lifecycle events currently include:

```text
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

The deployed Stripe Test Mode path is validated for hosted checkout, signed lifecycle synchronization and provider-side cancellation.

Important incident note: during final cancellation validation, Stripe had cancelled subscriptions correctly but local state remained `ACTIVE` because `customer.subscription.deleted` was not enabled on the webhook endpoint. The endpoint configuration is now corrected; PR #98 additionally repairs already-terminal provider state idempotently.

## Razorpay Test Mode

Example variable names:

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

Correct webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Razorpay checkout creation/redirect works, but recurring Test Mode authorization remains provider-sandbox blocked: attempted cards fail before authorization completes. Keep live readiness deferred.

## Provider plan mapping

Application plans are separate from Stripe Products/Prices and Razorpay Plans. Creating an application plan through system administration does not automatically provision provider billing objects. New paid plans need explicit server-side provider mappings before checkout can be enabled for those providers.

## Standard environment safety

Never commit or expose provider keys, API secrets, webhook secrets or provider plan/price IDs. Rotate any credential exposed in a screenshot, log or commit.

Also configure database, JWT, CORS, bootstrap and frontend URL variables from the production environment template. Keep:

```text
SYSTEM_ADMIN_BOOTSTRAP_ENABLED=false
PASSWORD_RESET_EXPOSE_TOKEN=false
CORS_ALLOWED_ORIGINS=<hosted frontend only>
FORWARD_HEADERS_STRATEGY=framework
```

Hosted staging is not a production SLA. Backup/restore drills, alerts, runbooks, load/failure testing and production R2 validation remain.
