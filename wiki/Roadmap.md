# Roadmap

## Completed application milestones

### Billing/catalog lifecycle

Closed through PR #106.

### Tenant-configurable outbound webhooks

Closed through PR #112.

### Enterprise OIDC SSO / identity federation

Closed through PR #119.

Completed capabilities:

- tenant-scoped OIDC provider configuration
- encrypted write-only client secrets
- controlled discovery/JWKS verification with SSRF-safe provider requests
- tenant-bound state/nonce/PKCE authorization runtime
- strict ID-token validation
- safe linking to existing active tenant users only; no federation auto-provisioning
- verified workspace auth-mode discovery
- persisted `OPTIONAL`/`REQUIRED` SSO policy
- guarded tenant-admin password break-glass path
- safe policy fallback when provider verification is invalidated
- browser SSO UX using opaque single-use session handoff
- tenant-admin Authentication configuration/verification/lifecycle UX
- federation success/failure audit visibility without sensitive provider material

SAML remains an optional adapter to add only when enterprise requirements justify it. Provider metadata/JWKS caching is also optional performance work; the current runtime favors fresh validated provider data.

## Next major product milestone

### 1. Authorization delegation and explain-access

Target capabilities:

- controlled delegation of existing permissions without bypassing backend authorization
- explicit scope, delegator/delegatee and revocation semantics
- optional expiry where useful
- an explain-access service that traces effective tenant/scoped grants for a user/resource/action
- stable reason codes suitable for admin UX and audit logs
- no leakage of secrets or unrelated tenant authorization data
- tenant-admin APIs/UX behind appropriate authorization
- cross-tenant and privilege-escalation regression coverage

Recommended sequence:

1. define delegation invariants/data model and effective-permission interaction
2. implement backend delegation lifecycle and audit events
3. implement explain-access decision model/service
4. add permission-gated admin UX
5. hardening, tests and documentation

## Following platform work

2. backup/restore drills, monitoring, alerts and operational runbooks
3. broader load/failure-recovery and production R2 verification
4. optional SAML/SCIM where concrete enterprise requirements exist
5. optional notification expansion such as digests/live browser delivery

## Independent provider/live-readiness track

- preserve Stripe as the working/validated Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, verified provider reconciliation, webhook-authoritative normal billing state, immutable history, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.
