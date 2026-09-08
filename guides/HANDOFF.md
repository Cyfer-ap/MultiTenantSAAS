# Development Handoff

Snapshot date: 2026-09-08  
Reviewed state: post-PR #119 (`c36de3f`)

## Current phase

**Enterprise OIDC SSO complete at application level; authorization delegation/explain-access next.**

Billing/catalog and tenant-configurable outbound-webhook milestones remain closed.

## Resume from

Read:

1. `../CHECKPOINT.md`
2. `enterprise-sso-foundation.md`
3. `authorization_model.md`
4. `../wiki/Security-and-Authentication.md`
5. `../wiki/Roadmap.md`

## OIDC SSO completed sequence

- #114 provider model + encrypted secret
- #115 provider verification
- #116 secure tenant-bound OIDC runtime/linking
- #117 discovery + optional/required policy + break-glass
- #118 browser completion + opaque one-time session handoff
- #119 admin Authentication UX + provider lifecycle recovery + audit visibility

Preserve no-auto-provisioning, tenant-bound identity linking, fresh SSRF-safe provider validation, single-use authorization/handoff state, server-only secrets/tokens and backend-authoritative policy.

## Deployment

Hosted SSO needs a stable `IDENTITY_FEDERATION_ENCRYPTION_KEY`, exact backend `OIDC_REDIRECT_URI` registered with the IdP, and frontend `OIDC_FRONTEND_COMPLETION_URI`. See `enterprise-sso-foundation.md`.

## Provider truth

Stripe is working. Razorpay is the provider whose recurring Test Mode card authorization remains sandbox-blocked. Do not reverse these statuses in future documentation.

## Next action

Implement **authorization delegation and explain-access** without weakening current scoped permission evaluation or tenant isolation. Prefer an auditable explanation model over frontend-only permission inference.
