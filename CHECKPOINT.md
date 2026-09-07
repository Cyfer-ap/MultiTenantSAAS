# MultiTenantSAAS — Checkpoint

Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`
Date: 2026-09-07
Base reviewed state: post-PR #112 (`8324ae9`)

## Current phase

**Tenant-configurable outbound webhooks — COMPLETE at application level**

Billing/catalog lifecycle remains closed at application level. PRs #108–#112 complete the next platform milestone by adding tenant-managed outbound integrations with secure endpoint configuration, durable signed delivery, domain-event publication, replay/history and a tenant-admin Integrations UX.

Live-provider billing readiness and external webhook receiver readiness remain deployment/operations concerns rather than unfinished application architecture.

## Delivered outbound-webhook sequence

- PR #108: tenant-scoped endpoint/event-subscription model, generated/rotatable signing secrets, AES-256-GCM secret storage, `tenant.update` authorization and SSRF-safe HTTPS validation
- PR #109: V38 durable events/deliveries, HMAC-SHA256 signing, delivery-time SSRF revalidation, lease-safe workers, retries/backoff, timeouts and terminal failure
- PR #110: transactional publication from project/task/comment/member/subscription mutations with provider-neutral payloads
- PR #111: V39 immutable delivery-attempt ledger, tenant-scoped history/detail APIs and guarded manual replay
- PR #112: permission-gated Integrations route, endpoint lifecycle UX, one-time secret handling, delivery filtering/detail/attempt history and replay controls

## Outbound webhook invariants

- endpoint management is tenant scoped and authorized through `tenant.update`
- destination URLs must be HTTPS and public-routable; DNS/SSRF validation is repeated immediately before delivery
- redirects are disabled
- signing secrets are generated server-side, encrypted at rest and exposed only on create/rotation
- delivery signature uses HMAC-SHA256 over `timestamp.eventId.body`
- event IDs and exact stored payloads remain stable across retries and replay
- queue state is durable; workers use leases and stale-lease recovery
- replay is limited to terminal deliveries and requires an active/enabled endpoint
- tenant isolation applies to endpoints, deliveries, attempts and replay

## Initial outbound event catalogue

```text
project.created
project.updated
project.archived
task.created
task.updated
task.completed
comment.created
comment.replied
member.added
member.removed
subscription.updated
subscription.cancelled
```

## Billing/provider status

### Stripe

**Validated deployed Test Mode path.** Hosted checkout, Test cards, signed lifecycle webhooks, provider-side cancellation and reconciliation have been validated. Managed Product/Price provisioning/versioning remains implemented.

### Razorpay

**Application integration/catalog provisioning implemented; recurring Test Mode authorization remains provider-sandbox blocked.** Managed Plan creation/replacement remains available, but sandbox cards fail before recurring authorization completes. Keep live-readiness separate.

## Database checkpoint

Portable common migrations extend through **V39**:

```text
V28 billing foundation
V29 provider subscription linkage
V30 durable billing usage events
V31 tenant API keys
V32 API-key last-used metadata
V33 subscription-plan usage limits
V34 provider catalog mappings + purchased-plan snapshots
V35 durable plan-retirement operations
V36 immutable tenant subscription history
V37 outbound webhook endpoints + event subscriptions
V38 outbound webhook events + durable deliveries
V39 outbound webhook delivery attempts
```

Never rewrite an applied migration.

## Verification checkpoint

PRs #108–#112 were developed under Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security/Trivy, Container CI and Qodana gates. #112 additionally verifies React 19 lint rules, TypeScript/MUI 9 production compilation and frontend container build/scan on its final clean head.

## Documentation/Wiki

Focused webhook guides now include:

- `guides/outbound-webhook-events.md`
- `guides/outbound-webhook-delivery-history.md`
- `guides/outbound-webhook-admin-ux.md`

`wiki/*.md` remains canonical Wiki source and is automatically published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Next platform milestone

Start **enterprise SSO / identity federation**. Recommended scope: tenant identity configuration → OIDC first → account linking/domain discovery → login enforcement/recovery → audit/admin UX. SAML can follow through the same provider-neutral identity boundary where justified.

After SSO: authorization delegation/explain-access, backup/restore drills, monitoring/alerts/runbooks, broader failure-recovery/load validation and production R2 verification.
