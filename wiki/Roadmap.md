# Roadmap

## Current

The billing/catalog lifecycle milestone is closed at application level through PR #106.

The tenant-configurable outbound webhook milestone is also closed at application level through PR #112.

### Completed outbound-webhook capabilities

- tenant-admin endpoint registration/update/enable-disable/archive
- event subscription selection from a server-owned catalogue
- generated signing secrets and secret rotation
- AES-256-GCM secret encryption at rest
- HMAC-SHA256 outbound delivery with timestamps and stable event IDs
- HTTPS-only SSRF-safe endpoint validation and delivery-time DNS revalidation
- durable event/delivery/attempt persistence
- retries, exponential backoff, timeouts, leases, stale-lease recovery and terminal failure
- transactional publication from projects, tasks, comments/replies, membership and selected subscription lifecycle changes
- tenant-scoped delivery history/detail and immutable attempt history
- guarded manual replay
- tenant isolation and `tenant.update` authorization
- permission-gated tenant Integrations UX

The implementation sequence was completed as:

1. PR #108 — endpoint/subscription/signing foundation
2. PR #109 — durable delivery engine
3. PR #110 — domain event integration
4. PR #111 — history/attempts/replay
5. PR #112 — tenant-admin endpoint/delivery UX

## Next major product milestone

### 1. Enterprise SSO / identity federation

Recommended next feature.

Target capabilities:

- tenant-scoped identity-provider configuration
- secure server-side storage for client/provider secrets
- provider-neutral federation abstraction
- OIDC as the first protocol/provider path
- tenant/domain discovery before authentication
- safe account linking to existing tenant users
- clear policy for optional SSO vs SSO-enforced tenants
- recovery/break-glass path that cannot be disabled accidentally
- state/nonce/PKCE and redirect validation
- login-attempt auditing and security event visibility
- tenant-admin configuration/verification UX
- provider metadata/discovery caching with safe refresh behavior
- SAML support through the same boundary when enterprise requirements justify it

Recommended implementation sequence:

1. tenant identity-provider model + secret/configuration safety
2. provider-neutral federation service + OIDC login callback flow
3. discovery/account-linking and tenant policy enforcement
4. admin/login UX + audit/observability
5. hardening, regression tests and deployment documentation
6. optional SAML adapter

Preserve local/break-glass administration so a bad IdP configuration cannot permanently lock a tenant out.

## Following platform work

2. authorization delegation and explain-access
3. backup/restore drills, monitoring, alerts and operational runbooks
4. broader load/failure-recovery and production R2 verification
5. optional notification expansion such as digests/live browser delivery

## Independent provider/live-readiness track

- preserve the validated Stripe Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately from feature development
- production outbound-webhook consumers should validate HMAC signatures, timestamp policy, receiver TLS/DNS behavior and failure runbooks separately from core application completeness

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, webhook-authoritative normal billing state, verified provider reconciliation, immutable purchased/history data, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.
