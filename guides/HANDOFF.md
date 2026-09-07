# Development Handoff

Repository: `Cyfer-ap/MultiTenantSAAS`
Default branch: `main`
Reviewed state: post-PR #112 (`8324ae9`)
Date: 2026-09-07

## Current phase

**Tenant-configurable outbound webhooks complete at application level; enterprise SSO next**

Billing/catalog lifecycle remains closed. Stripe is the validated deployed Test Mode payment path. Razorpay application integration and managed Plan provisioning are complete, while recurring Test Mode authorization remains externally provider-sandbox blocked.

## Outbound webhook result

Delivered through PRs #108–#112:

- tenant-scoped endpoint lifecycle and event subscriptions
- generated/rotatable signing secrets encrypted with AES-256-GCM at rest
- HTTPS/public-routable endpoint validation and delivery-time DNS/SSRF revalidation
- durable immutable events/deliveries with stable event IDs and bodies
- HMAC-SHA256 signing over `timestamp.eventId.body`
- lease-safe workers, retries/backoff/timeouts and stale-lease recovery
- transactional domain-event publication for projects/tasks/comments/membership/subscriptions
- immutable V39 attempt history
- tenant delivery history/detail and terminal replay
- permission-gated Integrations UX for endpoint/delivery administration

## Boundaries

- endpoint management uses `tenant.update`
- endpoint/delivery/attempt access remains tenant isolated
- secrets stay server-side; plaintext appears only on create/rotation
- redirects stay disabled
- DNS/SSRF safety is rechecked immediately before dispatch
- retries/replay preserve the original event ID and exact body
- replay requires a terminal delivery and an active enabled endpoint
- event publication stays transactional with business mutations
- API keys remain tenant-bound and restricted to `/api/external/**`
- provider billing lifecycle remains webhook-authoritative in normal operation

## Database checkpoint

Common Flyway migrations extend through **V39**. Never rewrite an applied migration.

## Documentation/Wiki

Current root docs, focused guides and version-controlled Wiki source are refreshed through PR #112. `wiki/*.md` is canonical and publishes automatically from merged `main`.

Webhook guides:

- `guides/outbound-webhook-events.md`
- `guides/outbound-webhook-delivery-history.md`
- `guides/outbound-webhook-admin-ux.md`

## Resume steps

1. treat billing/catalog and outbound-webhook milestones as closed at application level
2. keep Razorpay sandbox behavior and provider live-mode readiness separate from feature development
3. begin enterprise SSO / identity federation
4. preserve full CI requirements on every PR

Recommended SSO sequence:

1. tenant identity-provider configuration and secure secret storage
2. provider-neutral federation boundary with OIDC first
3. safe account linking and tenant/domain discovery
4. optional vs enforced SSO policy with recovery/break-glass protections
5. login/admin UX, auditing and regression coverage
6. SAML support through the same boundary when required

## Verification

Require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana before merge. Relevant Wiki changes must also pass `Wiki Sync / Validate Wiki Source`.
