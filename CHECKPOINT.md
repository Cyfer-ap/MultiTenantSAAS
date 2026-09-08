# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-08
Base reviewed state: post-PR #119 (`c36de3f`)

## Current phase

**Enterprise OIDC SSO / identity federation — COMPLETE at application level**

Billing/catalog lifecycle and tenant-configurable outbound webhooks remain closed at application level. PRs #114–#119 complete the OIDC federation milestone: tenant-scoped IdP configuration, verification, secure OIDC runtime, workspace discovery, optional/required policy, browser login UX, tenant-admin administration and federation audit visibility.

SAML is intentionally deferred until a concrete enterprise requirement justifies another protocol adapter. Live-provider billing readiness and production operations remain independent tracks.

## Delivered SSO sequence

- PR #114: tenant identity-provider model, OIDC configuration lifecycle, encrypted write-only client secret and `tenant.update` authorization
- PR #115: controlled OIDC discovery/JWKS/provider verification with SSRF-safe remote-request validation
- PR #116: tenant-bound OIDC authorization/callback runtime with state, nonce, PKCE, ID-token validation and safe linking to existing tenant users only
- PR #117: verified workspace discovery, `OPTIONAL`/`REQUIRED` tenant SSO policy and tenant-admin password break-glass protection
- PR #118: browser SSO UX plus short-lived, opaque, single-use backend-to-frontend session handoff
- PR #119: tenant-admin Authentication workspace, provider enable/disable/re-enable lifecycle, policy UX and tenant-scoped OIDC success/failure auditing

## SSO security invariants

- one IdP configuration is tenant scoped; current protocol is OIDC
- client secrets are write-only and AES-256-GCM encrypted with `IDENTITY_FEDERATION_ENCRYPTION_KEY`
- changing provider configuration or rotating its secret invalidates verification
- disabled providers re-enable only to `DRAFT`; they never silently regain `VERIFIED`
- issuer/provider endpoints must be HTTPS and public-routable; redirects are disabled and destinations are revalidated before remote requests
- authorization transactions hash state/nonce at rest and encrypt PKCE verifiers
- callback transactions are single-use before external token exchange
- ID tokens are validated for signature/algorithm, issuer, audience/authorized party, time claims and nonce
- federation never auto-provisions users; first link requires verified provider email matching an existing active user in the same tenant
- `REQUIRED` SSO is permitted only with a verified IdP and an active password-capable tenant-admin break-glass path
- provider invalidation safely returns policy to `OPTIONAL`
- browser completion receives only a short-lived opaque one-time handoff code, never platform tokens in the redirect URL
- audit records avoid provider tokens, codes, state, nonce, PKCE, client secrets and sensitive provider payloads

## Authentication modes

Verified workspace discovery can return:

```text
PASSWORD_ONLY
PASSWORD_OR_SSO
SSO_ONLY
SSO_REQUIRED
```

The backend remains authoritative. Frontend mode-specific rendering is UX only.

## Database checkpoint

Portable common migrations extend through **V43**:

```text
V37 outbound webhook endpoints + event subscriptions
V38 outbound webhook events + durable deliveries
V39 outbound webhook delivery attempts
V40 tenant identity-provider configuration
V41 OIDC authorization transactions + tenant federated identities
V42 tenant SSO policy
V43 OIDC browser session handoffs
```

Never rewrite an applied migration.

## Billing/provider status

### Stripe

**Working and validated in deployed Test Mode.** Hosted checkout, signed lifecycle webhooks, provider-side cancellation and reconciliation are implemented and validated. Managed Product/Price provisioning/versioning remains implemented.

### Razorpay

**Application integration/catalog provisioning implemented; recurring Test Mode authorization remains provider-sandbox blocked.** Keep Razorpay available, but do not treat sandbox-card authorization failures as unfinished core billing architecture.

## Deployment requirements for SSO

Required server-side federation configuration includes:

```text
IDENTITY_FEDERATION_ENCRYPTION_KEY
OIDC_REDIRECT_URI
OIDC_FRONTEND_COMPLETION_URI
OIDC_AUTHORIZATION_TRANSACTION_MINUTES
OIDC_SESSION_HANDOFF_MINUTES
```

For hosted deployment, `OIDC_REDIRECT_URI` must be the backend callback and must also be registered with the IdP. `OIDC_FRONTEND_COMPLETION_URI` is the frontend `/auth/oidc/complete` route.

See `guides/enterprise-sso-foundation.md` and `wiki/Production-Deployment.md`.

## Verification checkpoint

PR #119 passed Backend, PostgreSQL/Flyway, Frontend formatting/tests/lint/build, Repository Hygiene, Security/Trivy, Container CI and Qodana on its final head before merge.

## Documentation/Wiki

`wiki/*.md` remains canonical Wiki source and is automatically published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Next platform milestone

Start **authorization delegation and explain-access**: controlled permission delegation plus an auditable explanation of why a user can access a resource.

After that: backup/restore drills, monitoring/alerts/runbooks, broader load/failure-recovery testing and production R2 verification. Optional SAML and notification expansion remain separate demand-driven work.
