# Production Deployment

Reviewed through PR #106 on 2026-09-07.

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

Billing/catalog lifecycle is complete at application level, but Test Mode validation is not a live-production readiness claim. Provider live credentials/catalogs, compliance and operational checks remain separate work.

Managed provider catalogs use an explicit environment (`TEST`/`LIVE`) and durable provider mappings. Do not mix provider environments.

## Stripe Test Mode

Stripe can run beside Razorpay.

Example server-side Test Mode variables:

```dotenv
STRIPE_BILLING_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_BILLING_ENVIRONMENT=TEST
STRIPE_PRICE_PRO=price_...
STRIPE_PRICE_ENTERPRISE=price_...
STRIPE_SUCCESS_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=success
STRIPE_CANCEL_URL=https://multitenantsaas-frontend.onrender.com/subscription?checkout=cancelled
STRIPE_WEBHOOK_ENABLED=true
STRIPE_WEBHOOK_SECRET=whsec_...
```

Legacy `STRIPE_PRICE_*` values remain compatibility/import paths. Managed system-admin plans persist Product/Price mappings in the database.

Register:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Required subscription lifecycle events currently include:

```text
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

The deployed Stripe Test Mode path is validated for hosted checkout, signed lifecycle synchronization and provider-side cancellation.

## Razorpay Test Mode

Example variable names:

```dotenv
RAZORPAY_BILLING_ENABLED=true
RAZORPAY_KEY_ID=...
RAZORPAY_KEY_SECRET=...
RAZORPAY_BILLING_ENVIRONMENT=TEST
RAZORPAY_PLAN_PRO=plan_...
RAZORPAY_PLAN_ENTERPRISE=plan_...
RAZORPAY_SUBSCRIPTION_TOTAL_COUNT=120
RAZORPAY_WEBHOOK_ENABLED=true
RAZORPAY_WEBHOOK_SECRET=...
```

Legacy `RAZORPAY_PLAN_*` values remain compatibility/import paths. Managed system-admin plans persist Razorpay Plan mappings in the database.

Webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

Razorpay application integration and managed Plan provisioning are implemented, but recurring Test Mode authorization remains provider-sandbox blocked: attempted cards fail before authorization completes. Keep live readiness deferred.

## Provider catalog lifecycle

Application plans remain separate from provider billing objects but are synchronized for enabled managed providers.

- Stripe economic edits create replacement Prices; retired Product/Price objects are unavailable for new purchase but retained for history.
- Razorpay provider-visible edits create replacement Plans and archive prior local mappings.
- terminal plan retirement schedules existing provider subscriptions to stop at period/cycle end and does not optimistically mark local subscriptions cancelled.

## Database checkpoint

Common portable migrations extend through **V36**. V34 adds provider mappings/purchased snapshots, V35 durable retirement operations and V36 immutable tenant subscription history.

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
