# Production Deployment

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

## Razorpay Test Mode

Current deployment intentionally uses Test Mode. Required variable names:

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

Map Test Mode keys only to Test Mode plan IDs. Never commit values or expose them to the frontend.

Correct webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

The webhook secret is a value chosen/configured for that Razorpay webhook, not the Razorpay API key secret. Opening the URL in a browser is a GET and is not a valid webhook test.

Rotate any credential exposed in a screenshot, log or commit.

## Current deployment status

The backend startup and application checkout route work. Razorpay's hosted page opens, but every attempted Test Mode card fails before recurring authorization. Therefore activation webhooks and local subscription activation have not been validated with the real provider.

Do not enable live keys or live plans until Test Mode completes successfully.

## Standard environment safety

Also configure database, JWT, CORS, bootstrap and frontend URL variables from the production environment template. Keep:

```text
SYSTEM_ADMIN_BOOTSTRAP_ENABLED=false
PASSWORD_RESET_EXPOSE_TOKEN=false
CORS_ALLOWED_ORIGINS=<hosted frontend only>
FORWARD_HEADERS_STRATEGY=framework
```

Hosted staging is not a production SLA. Backup/restore drills, alerts, runbooks, load/failure testing and production R2 validation remain.
