# Multi-Tenant SaaS Platform

A production-oriented full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, collaboration, subscription enforcement, external billing, durable integrations, enterprise OIDC SSO, PostgreSQL correctness and operational hardening.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Application state reviewed through: PR #119 (`c36de3f`)
> Snapshot date: 2026-09-08
> Current phase: **Enterprise OIDC SSO complete; authorization delegation/explain-access next**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users and organization hierarchy
- scoped permission-oriented authorization
- projects, tasks, comments, mentions, replies, pins and activity history
- S3/R2-compatible attachments
- durable in-app/email notifications and preferences
- subscription lifecycle restrictions, quotas and professional hosted checkout
- provider-backed cancellation, reconciliation and immutable subscription history
- tenant API keys restricted to `/api/external/**` with metering/plan limits
- tenant-configurable HMAC-signed outbound webhooks with retries/history/replay
- enterprise OIDC SSO with safe account linking, optional/required policy, break-glass recovery and tenant-admin configuration UX

### System plane

- separate system-admin identity/control plane
- tenant, user, subscription-plan and subscription administration
- managed Stripe/Razorpay provider catalog provisioning
- billing event/history visibility and read-only provider reconciliation
- durable usage summaries and plan-level API limits
- tenant/platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.0.8, Spring Security/JWT/OIDC, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2 and Actuator/Micrometer.

**Frontend:** React 19.2, TypeScript 6, Vite 8, Material UI 9, React Router 7, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Enforcement pipeline

```text
authentication / federation
    ↓
tenant isolation
    ↓
authorization / scoped permission evaluation
    ↓
subscription lifecycle access
    ↓
resource and API-usage quotas
    ↓
domain invariants
```

The backend is authoritative at every enforcement boundary.

# Billing & Payments

**Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.**

Implemented capabilities include provider-neutral Stripe/Razorpay checkout, signed provider webhooks, durable/idempotent lifecycle synchronization, provider-aware cancellation/reconciliation, safe terminal plan retirement, durable TEST/LIVE provider catalog mappings, managed Stripe Product/Price provisioning, managed Razorpay Plan provisioning, immutable purchased-plan snapshots, subscription history, usage metering, API keys and quotas.

### Stripe

**Stripe is working and validated in deployed Test Mode.** Hosted Checkout, signed subscription lifecycle webhooks, provider-side cancellation and reconciliation have been validated. This is not a live-production readiness claim.

### Razorpay

**Razorpay application integration and managed catalog provisioning are implemented, but recurring Test Mode authorization remains provider-sandbox blocked.** Keep Razorpay available; treat live/provider readiness separately from core application completeness.

# Tenant Outbound Webhooks

**Tenant-configurable outbound webhooks are complete at application level through PR #112.**

The platform supports tenant endpoint lifecycle, event subscriptions, generated/rotatable encrypted signing secrets, HTTPS/public-routable SSRF validation, durable HMAC-SHA256 delivery, retries/backoff/leases/timeouts, transactional domain events, immutable delivery attempts, tenant-scoped history and guarded replay through the Integrations workspace.

Focused guides:

- `guides/outbound-webhook-events.md`
- `guides/outbound-webhook-delivery-history.md`
- `guides/outbound-webhook-admin-ux.md`

# Enterprise OIDC SSO

**Enterprise OIDC SSO / identity federation is complete at application level through PR #119.**

The implementation sequence is:

- #114 — tenant IdP configuration, encrypted write-only secret and lifecycle
- #115 — controlled OIDC discovery/JWKS verification
- #116 — tenant-bound state/nonce/PKCE callback runtime and safe identity linking
- #117 — workspace auth-mode discovery, `OPTIONAL`/`REQUIRED` policy and break-glass
- #118 — browser SSO UX and one-time opaque session handoff
- #119 — Authentication admin UX, recoverable enable/disable lifecycle and federation audit visibility

### Security model

- provider secrets are server-only and encrypted with AES-256-GCM
- provider destination URLs are HTTPS/public-routable and revalidated before remote requests
- redirects are disabled for provider HTTP calls
- state/nonce are high entropy and hashed at rest; PKCE verifier is encrypted
- authorization transactions are tenant/provider/version bound and single use
- ID-token signature/algorithm, issuer, audience/authorized party, time claims and nonce are validated
- federation does not auto-provision application users
- first link requires `email_verified=true` and an existing active user in the same tenant
- `REQUIRED` policy needs a verified IdP plus an active password-capable tenant-admin break-glass path
- invalidating provider verification safely returns the tenant to `OPTIONAL`
- backend callback redirects only a short-lived opaque handoff code to the frontend completion route
- audit records omit provider tokens, authorization codes, state, nonce, PKCE, client secrets and sensitive provider payloads

### Tenant authentication modes

Workspace discovery can expose:

```text
PASSWORD_ONLY
PASSWORD_OR_SSO
SSO_ONLY
SSO_REQUIRED
```

The tenant Authentication page is available under the existing `tenant.update` permission and supports provider create/edit, verify/re-verify, client-secret rotation, disable/re-enable and policy control.

### Deployment variables

```dotenv
IDENTITY_FEDERATION_ENCRYPTION_KEY=<Base64 of exactly 32 random bytes>
OIDC_REDIRECT_URI=https://YOUR_BACKEND_DOMAIN/api/auth/oidc/callback
OIDC_FRONTEND_COMPLETION_URI=https://YOUR_FRONTEND_DOMAIN/auth/oidc/complete
OIDC_AUTHORIZATION_TRANSACTION_MINUTES=5
OIDC_SESSION_HANDOFF_MINUTES=2
```

The backend callback URI must be registered exactly with the IdP. Keep the encryption key stable while encrypted federation data exists.

Full setup/test procedure: `guides/enterprise-sso-foundation.md`.

SAML is intentionally deferred unless a concrete enterprise requirement justifies another protocol adapter.

## Database checkpoint

```text
multitenant-saas/src/main/resources/db/migration    historical H2 V1-V17
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL V17 baseline
multitenant-saas/src/main/resources/db/common      portable V18+
```

Shared migrations currently extend through **V43**. The most recent milestone migrations are:

- V37 outbound webhook endpoints/event subscriptions
- V38 outbound webhook events/deliveries
- V39 outbound webhook delivery attempts
- V40 tenant identity-provider configuration
- V41 OIDC authorization transactions and tenant federated identities
- V42 tenant SSO policy
- V43 OIDC browser session handoffs

Never rewrite an applied Flyway migration.

## Verification

Required GitHub Actions gates include Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security/Trivy, Container CI and Qodana. PR #119 passed all of them on its final head before merge.

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

Use `.env.production.example` as the deployment variable inventory; never commit real keys or secrets.

## Documentation and Wiki

Start with:

- `CHECKPOINT.md`
- `HANDOFF.md`
- `guides/enterprise-sso-foundation.md`
- `guides/subscription_billing.md`
- `wiki/Home.md`
- `wiki/Security-and-Authentication.md`
- `wiki/Production-Deployment.md`
- `wiki/Roadmap.md`

The repository `wiki/` directory is canonical Wiki source. `.github/workflows/wiki-sync.yml` validates relevant changes and publishes merged `main` Wiki updates using `scripts/publish-wiki.ps1`.

## Next platform milestone

The recommended product sequence is now:

1. **authorization delegation and explain-access**
2. backup/restore drills, monitoring, alerts and operational runbooks
3. broader load/failure-recovery and production R2 verification
4. optional SAML where a concrete enterprise requirement exists
5. optional notification expansion such as digests/live browser delivery
