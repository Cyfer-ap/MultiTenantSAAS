# Developer Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #112 (`8324ae9`)
Date: 2026-09-07

## Current phase

**Tenant-configurable outbound webhooks complete at application level; enterprise SSO next**

Billing/catalog lifecycle remains complete. Stripe is the validated deployed Test Mode path. Razorpay application integration and managed Plan provisioning are implemented, but recurring Test Mode authorization remains externally provider-sandbox blocked.

Do not extend application billing just to work around the Razorpay sandbox.

## Outbound webhook result

Delivered through PRs #108–#112:

- tenant-scoped endpoint/event-subscription management
- generated high-entropy signing secrets, rotation and AES-256-GCM at-rest encryption
- HTTPS/public-routable SSRF validation with delivery-time DNS revalidation
- durable immutable event envelopes and endpoint-specific deliveries
- stable event IDs and exact request bodies across retries/replay
- HMAC-SHA256 signing over `timestamp.eventId.body`
- lease-safe workers, retries/backoff/timeouts and stale-lease recovery
- transactional project/task/comment/member/subscription event publication
- immutable V39 attempt history
- tenant-scoped delivery history/detail APIs and guarded manual replay
- permission-gated tenant Integrations UX

## Preserve these boundaries

- authentication, tenant isolation, authorization, subscription access and quotas are independent
- endpoint administration uses `tenant.update`
- webhook endpoints/deliveries/attempts remain tenant isolated
- outbound signing secrets stay server-side and plaintext appears only on create/rotation
- redirects remain disabled and endpoint DNS/SSRF validation is repeated immediately before dispatch
- retries/replay retain original event identity/body
- archived, disabled or missing endpoints cannot be replayed
- product event publication stays transactional with the domain mutation
- provider billing webhooks remain authoritative for normal local subscription lifecycle
- provider IDs, API keys and secrets remain server-side
- tenant API keys authenticate only `/api/external/**` and never impersonate users

## Database checkpoint

Common Flyway migrations extend through **V39**. Never rewrite an applied migration.

## Resume sequence

1. treat billing/catalog lifecycle as closed at application level
2. treat tenant-configurable outbound webhooks as closed at application level
3. keep Stripe/Razorpay live readiness separate from feature development
4. start enterprise SSO / identity federation
5. then consider explain-access/delegation and deeper operational hardening

Recommended SSO sequence:

1. tenant identity-provider configuration and secure secret storage
2. provider-neutral federation boundary
3. OIDC first
4. safe existing-user account linking and tenant/domain discovery
5. optional/enforced SSO policy with recovery/break-glass protections
6. login/admin UX, auditing and regression coverage
7. SAML through the same boundary when enterprise requirements justify it

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana before merge.
