# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Application state reviewed through: PR #112 (8324ae9)
Date: 2026-09-07
Current phase: outbound webhooks complete at application level
Recommended next product milestone: enterprise SSO / identity federation
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/outbound-webhook-events.md`
4. `guides/outbound-webhook-delivery-history.md`
5. `guides/outbound-webhook-admin-ux.md`
6. `wiki/Production-Deployment.md`
7. `wiki/Testing-and-CI.md`
8. `wiki/Roadmap.md`

## Current result

Billing/catalog lifecycle remains complete at application level. Stripe is the validated deployed Test Mode payment path; Razorpay application integration and managed Plan provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

Tenant-configurable outbound webhooks are now also complete through PR #112.

Implemented webhook capabilities include:

- tenant-scoped endpoint registration, update, enable/disable and soft archive
- event-subscription selection from a bounded server catalogue
- generated signing secrets, one-time plaintext exposure and secret rotation
- AES-256-GCM at-rest secret encryption
- HTTPS/public-routable URL validation with delivery-time DNS/SSRF revalidation
- durable immutable event envelopes and endpoint-specific delivery records
- HMAC-SHA256 signing over timestamp, stable event ID and exact request body
- lease-safe asynchronous processing with retries, exponential backoff, timeouts and stale-lease recovery
- transactional publication from project, task, comment, membership and subscription mutations
- immutable delivery-attempt history
- tenant-scoped history/detail APIs and terminal-delivery replay
- permission-gated Integrations UX with endpoint lifecycle, one-time secret handling, filters, payload/attempt detail and replay

## Outbound webhook boundaries to preserve

- `tenant.update` remains the management authorization boundary
- endpoints, deliveries and attempts remain tenant isolated
- secrets stay server-side and plaintext is shown only on create/rotation
- redirects stay disabled
- public-routable DNS validation occurs both when configuring and immediately before dispatch
- event ID/body do not change across retries or replay
- archived/disabled/missing endpoints cannot be replayed
- event publication occurs transactionally with the domain mutation
- avoid unbounded outbox growth when no enabled endpoint subscribes

## Database checkpoint

Common migrations extend through **V39**. Never rewrite an applied Flyway migration.

- V37: outbound webhook endpoints/event subscriptions
- V38: durable events/deliveries
- V39: immutable delivery-attempt ledger

## Billing/provider boundary

- provider webhooks remain authoritative for normal local billing lifecycle synchronization
- verified provider lookup/reconciliation may repair stale terminal state
- managed Stripe/Razorpay catalog mappings stay server-side and environment scoped
- provider live readiness is independent of feature development
- do not resume Razorpay sandbox-card debugging as core application work

## Next action

Start **enterprise SSO / identity federation**.

Recommended rollout:

1. tenant identity-provider configuration model, secret handling and authorization
2. provider-neutral federation boundary with OIDC as the first concrete provider/protocol path
3. domain/email discovery and safe account linking to existing tenant users
4. tenant policy for optional vs enforced SSO with recovery/break-glass protections
5. login/admin UX, audit events, tests and deployment documentation
6. add SAML through the same boundary only where enterprise requirements justify it

Then consider authorization delegation/explain-access and deeper operational recovery/load testing.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana to pass. Wiki source changes should also pass `Wiki Sync / Validate Wiki Source`.
