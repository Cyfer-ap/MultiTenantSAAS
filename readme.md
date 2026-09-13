# Multi-Tenant SaaS Platform

A production-oriented full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, collaboration, subscription enforcement, external billing, durable integrations, enterprise OIDC SSO, PostgreSQL correctness and operational hardening.

> **Current documentation snapshot**
>
> Repository: `Cyfer-ap/MultiTenantSAAS`
> Branch: `main`
> Application state reviewed through: PR #125 (`0694403`)
> Snapshot date: 2026-09-13
> Current phase: **Authorization delegation and Explain Access complete; Production Operations & Disaster Recovery next**

## Platform capabilities

### Tenant plane

- secure tenant onboarding, JWT/browser sessions, invitations, users and organization hierarchy
- scoped permission-oriented authorization with bounded delegation and Explain Access
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

**Frontend:** React 19.2, TypeScript 6, Vite 8, Material UI 9, React Router, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

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

Stripe is working and validated in deployed Test Mode. Razorpay application integration and managed catalog provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked. Keep both providers; live/provider readiness is a separate operational track.

# Tenant Outbound Webhooks

**Tenant-configurable outbound webhooks are complete at application level through PR #112.**

The platform supports tenant endpoint lifecycle, event subscriptions, generated/rotatable encrypted signing secrets, HTTPS/public-routable SSRF validation, durable HMAC-SHA256 delivery, retries/backoff/leases/timeouts, transactional domain events, immutable delivery attempts, tenant-scoped history and guarded replay through the Integrations workspace.

# Enterprise OIDC SSO

**Enterprise OIDC SSO / identity federation is complete at application level through PR #119.**

The implementation includes tenant IdP configuration, encrypted write-only secrets, controlled OIDC discovery/JWKS verification, state/nonce/PKCE runtime, strict ID-token validation, safe linking to existing active users only, workspace auth-mode discovery, `OPTIONAL`/`REQUIRED` policy, tenant-admin break-glass, browser SSO handoff, Authentication admin UX and federation audit visibility.

Full setup/test procedure: `guides/enterprise-sso-foundation.md`.

# Authorization Delegation & Explain Access

**Authorization delegation and Explain Access are complete at application level through PR #125.**

Feature sequence:

- #121 — structured authorization decision model and Explain Access API using the enforcement evaluator
- #122 — V44 bounded delegation foundation, provenance, create/list/revoke lifecycle, audit and runtime source revalidation
- #125 — delegation-safe reference data, direct/delegated provenance, Authorization workspace, Delegations UX and Explain Access UX

Key invariants:

- every delegated grant is derived from one direct parent authority assignment
- child permissions, scope and validity must remain within the current parent authority
- delegated grants cannot be re-delegated
- `authorization.manage` and `authorization.delegate` cannot be delegated
- source authority is revalidated at access time, so revoked/expired/narrowed source grants invalidate children
- Explain Access uses the same evaluator as enforcement and exposes only the matched grant/provenance required for diagnosis
- managers can manage roles/assignments, delegations and Explain Access; delegate-only actors can access Delegations without gaining authorization administration

See `guides/authorization_model.md` and `wiki/Authorization.md`.

## Database checkpoint

```text
multitenant-saas/src/main/resources/db/migration    historical H2 V1-V17
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL V17 baseline
multitenant-saas/src/main/resources/db/common      portable V18+
```

Shared migrations currently extend through **V44**. Recent milestone migrations are:

- V37 outbound webhook endpoints/event subscriptions
- V38 outbound webhook events/deliveries
- V39 outbound webhook delivery attempts
- V40 tenant identity-provider configuration
- V41 OIDC authorization transactions and tenant federated identities
- V42 tenant SSO policy
- V43 OIDC browser session handoffs
- V44 authorization delegation provenance and `authorization.delegate`

Never rewrite an applied Flyway migration.

## Verification

Required GitHub Actions gates include Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana. PR #125 passed all required gates on its final head; frontend coverage reported 71 test files / 240 tests passing.

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

Use `.env.production.example` as the deployment variable inventory; never commit real keys or secrets.

## Documentation and Wiki

Start with:

- `CHECKPOINT.md`
- `HANDOFF.md`
- `guides/authorization_model.md`
- `guides/enterprise-sso-foundation.md`
- `guides/subscription_billing.md`
- `wiki/Home.md`
- `wiki/Authorization.md`
- `wiki/Production-Deployment.md`
- `wiki/Roadmap.md`

The repository `wiki/` directory is canonical Wiki source. `.github/workflows/wiki-sync.yml` validates relevant changes and publishes merged `main` Wiki updates using `scripts/publish-wiki.ps1`.

## Next platform milestone

The recommended product sequence is now:

1. **Production Operations & Disaster Recovery** — PostgreSQL backup/restore drills, monitoring, alerts and operational runbooks
2. broader load/failure-recovery and production R2 verification
3. optional SAML/SCIM where concrete enterprise requirements exist
4. optional notification expansion such as digests/live browser delivery
