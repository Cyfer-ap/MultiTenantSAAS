# Developer Handoff

Current snapshot: post-PR #119 (`c36de3f`), 2026-09-08.

## Current phase

**Enterprise OIDC SSO complete at application level; authorization delegation/explain-access next.**

Billing/catalog and outbound-webhook milestones remain closed.

## Resume reading

1. [[Home]]
2. [[Enterprise-SSO]]
3. [[Security-and-Authentication]]
4. [[Authorization]]
5. [[Production-Deployment]]
6. [[Testing-and-CI]]
7. [[Roadmap]]

## Preserve these SSO invariants

- tenant-scoped provider configuration and identity linkage
- write-only encrypted client secret
- fresh HTTPS/public-routable provider validation and disabled redirects
- state/nonce/PKCE protections and single-use callback transaction
- no user auto-provisioning from IdP claims
- verified-email first linking only to an existing active user in the same tenant
- backend-authoritative optional/required policy
- tenant-admin password break-glass prerequisite for `REQUIRED`
- provider invalidation safely falling back from enforced SSO
- opaque one-time browser session handoff instead of platform tokens in URLs
- no sensitive provider material in audit/frontend/log output

Portable migrations extend through V43.

## Provider status

Stripe is working/validated in deployed Test Mode. Razorpay application/catalog integration is implemented, but recurring Test Mode authorization remains provider-sandbox blocked.

## Next

Build authorization delegation and explain-access. Do not weaken the current authorization evaluator to make delegation easier; delegation should feed a well-defined effective-permission model and remain auditable/revocable.
