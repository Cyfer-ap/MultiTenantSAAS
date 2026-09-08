# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Application state reviewed through: PR #119 (c36de3f)
Date: 2026-09-08
Current phase: enterprise OIDC SSO complete at application level
Recommended next product milestone: authorization delegation and explain-access
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/enterprise-sso-foundation.md`
4. `guides/authorization_model.md`
5. `wiki/Security-and-Authentication.md`
6. `wiki/Production-Deployment.md`
7. `wiki/Testing-and-CI.md`
8. `wiki/Roadmap.md`

## Current result

Completed application-level milestones now include billing/catalog lifecycle, tenant-configurable outbound webhooks and enterprise OIDC SSO.

The SSO sequence through PR #119 provides:

- tenant-scoped OIDC provider configuration with `DRAFT`, `VERIFIED` and `DISABLED` lifecycle
- AES-256-GCM encrypted, write-only client secrets
- controlled discovery/JWKS verification with SSRF-safe HTTPS/public-DNS enforcement
- tenant-bound OIDC state/nonce/PKCE authorization runtime
- strict ID-token validation
- safe account linking to existing active tenant users only; no federation auto-provisioning
- verified workspace discovery with `PASSWORD_ONLY`, `PASSWORD_OR_SSO`, `SSO_ONLY` and `SSO_REQUIRED`
- persisted `OPTIONAL`/`REQUIRED` SSO policy
- guarded tenant-admin password break-glass path
- safe fallback to `OPTIONAL` when provider verification is invalidated
- opaque single-use browser session handoff after callback
- permission-gated Authentication admin UX
- provider verify/re-verify, secret rotation, disable and recoverable re-enable-to-draft lifecycle
- tenant-scoped success/failure audit events without storing sensitive federation material

## SSO boundaries to preserve

- backend policy is authoritative; never rely on frontend guards for enforcement
- client secrets, state, nonce, PKCE verifiers, codes and provider tokens remain server-side
- do not auto-provision a tenant user from an IdP claim
- do not link identities across tenant boundaries
- provider edits/secret rotation must invalidate verification
- a disabled provider must not regain `VERIFIED` on enable
- `REQUIRED` must remain impossible without a tested password-capable tenant-admin recovery path
- invalid/untrusted OIDC state must not be attributed to a tenant audit record
- callback URLs must not carry platform access/refresh tokens
- remote provider requests keep SSRF protections, bounded responses/timeouts and disabled redirects

## Database checkpoint

Common portable migrations extend through **V43**. Never rewrite an applied Flyway migration.

- V40: tenant identity-provider configuration
- V41: OIDC authorization transactions and tenant federated identities
- V42: tenant SSO policy
- V43: one-time OIDC browser session handoffs

## Deployment checkpoint

Configure a stable Base64-encoded 32-byte `IDENTITY_FEDERATION_ENCRYPTION_KEY` plus explicit hosted `OIDC_REDIRECT_URI` and `OIDC_FRONTEND_COMPLETION_URI`. The IdP redirect/callback registration must match the backend callback exactly.

See `guides/enterprise-sso-foundation.md` for setup and test procedure.

## Billing/provider boundary

- Stripe is working and validated in deployed Test Mode
- Razorpay integration and managed Plan provisioning remain implemented, but recurring Test Mode authorization is provider-sandbox blocked
- keep both providers; live readiness remains an independent operational review

## Next action

Start **authorization delegation and explain-access**.

Recommended first slice:

1. model safe delegation boundaries and revocation rules without weakening current permission evaluation
2. build an explain-access service that traces effective tenant/scoped grants and produces a non-sensitive decision explanation
3. expose permission-gated admin APIs/UI plus audit events and cross-tenant regression coverage

After that, move to backup/restore drills, monitoring/alerts/runbooks and broader failure-recovery/load testing. SAML remains optional until a real enterprise requirement appears.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana to pass. Wiki source changes should also satisfy Wiki Sync validation.
