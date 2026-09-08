# Production Deployment

Reviewed through PR #119 on 2026-09-08.

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

## Enterprise OIDC SSO deployment

Application-level OIDC SSO is complete through PR #119. Production deployment requires explicit callback/completion configuration in addition to the federation encryption key.

Example:

```dotenv
IDENTITY_FEDERATION_ENCRYPTION_KEY=<Base64 of exactly 32 random bytes>
OIDC_REDIRECT_URI=https://multitenantsaas-akxn.onrender.com/api/auth/oidc/callback
OIDC_FRONTEND_COMPLETION_URI=https://multitenantsaas-frontend.onrender.com/auth/oidc/complete
OIDC_AUTHORIZATION_TRANSACTION_MINUTES=5
OIDC_SESSION_HANDOFF_MINUTES=2
```

`OIDC_REDIRECT_URI` is the IdP callback and must be registered **exactly** in the external OIDC client. `OIDC_FRONTEND_COMPLETION_URI` is the application completion route; the backend redirects only an opaque one-time handoff code there, never platform access/refresh tokens.

Keep `IDENTITY_FEDERATION_ENCRYPTION_KEY` stable while encrypted IdP client secrets/PKCE data exist. Changing it without a migration makes encrypted federation data unreadable.

### IdP deployment checklist

1. Create a confidential OIDC web client.
2. Register the exact backend callback URI.
3. Enable authorization code flow; `openid` is mandatory.
4. Ensure verified email is available for first-link testing.
5. Configure the tenant provider in **Authentication** and verify it.
6. Test SSO with an already-existing active tenant user whose email matches the verified IdP email.
7. Confirm success/audit behavior before enabling `REQUIRED`.
8. Verify the tenant-admin password break-glass path independently.

See [[Enterprise-SSO]].

## Billing deployment boundary

Billing/catalog lifecycle is complete at application level, but Test Mode validation is not a live-production readiness claim. Provider live credentials/catalogs, compliance and operational checks remain separate work.

### Stripe Test Mode

**Stripe is working/validated in deployed Test Mode.** Hosted checkout, signed lifecycle synchronization, provider-side cancellation and reconciliation are validated.

Webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/stripe
```

Subscription lifecycle events include:

```text
customer.subscription.created
customer.subscription.updated
customer.subscription.deleted
```

### Razorpay Test Mode

Razorpay application integration and managed Plan provisioning are implemented, but recurring Test Mode authorization remains provider-sandbox blocked. Keep Razorpay configured/available as required; do not treat the sandbox card failure as unfinished core billing architecture.

Webhook target:

```text
POST https://multitenantsaas-akxn.onrender.com/api/billing/webhooks/razorpay
```

## Database checkpoint

Portable common migrations extend through **V43**. V40–V43 are the SSO persistence layers.

## Standard environment safety

Use `.env.production.example` as the variable inventory. Never commit provider keys, database credentials, JWT secrets, email secrets, storage credentials, webhook signing encryption keys or federation secrets.

Keep production defaults such as:

```text
SYSTEM_ADMIN_BOOTSTRAP_ENABLED=false
PASSWORD_RESET_EXPOSE_TOKEN=false
CORS_ALLOWED_ORIGINS=<hosted frontend only>
AUTH_COOKIE_SECURE=true
```

Hosted staging is not a production SLA. Backup/restore drills, alerts, runbooks, load/failure testing and production R2 validation remain following work.
