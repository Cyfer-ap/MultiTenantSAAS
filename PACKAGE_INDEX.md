# Documentation Package Index

Current snapshot: post-PR #112 (`8324ae9`), 2026-09-07.

## Primary status documents

- `readme.md` — platform overview, billing/outbound-integration status and next milestone
- `CHECKPOINT.md` — concise verified application checkpoint
- `HANDOFF.md` — session-independent resume instructions
- `guides/subscription_billing.md` — billing contracts and provider catalog lifecycle
- `guides/outbound-webhook-events.md` — outbound event catalogue and payload contracts
- `guides/outbound-webhook-delivery-history.md` — delivery/attempt history and replay
- `guides/outbound-webhook-admin-ux.md` — tenant Integrations UX and API mapping
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining platform/live-readiness work
- `wiki/Home.md` — version-controlled Wiki entry point
- `wiki/Roadmap.md` — current platform sequence
- `wiki/Wiki-Maintenance.md` — automatic Wiki synchronization policy
- `MANIFEST.json` — machine-readable documentation inventory

## Completed platform milestones

### Billing/catalog

Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.

Stripe remains the validated deployed Test Mode payment path and supports managed Product/Price provisioning/versioning. Razorpay application integration and managed Plan provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

### Tenant outbound webhooks

Tenant-configurable outbound webhooks are complete at application level through PR #112.

Delivered capabilities include tenant endpoint/event-subscription management, generated/rotatable encrypted signing secrets, SSRF-safe HTTPS validation, durable immutable event/delivery/attempt records, HMAC-SHA256 delivery signing, lease-safe retry/backoff processing, transactional domain events, delivery history/detail, guarded replay and tenant-admin Integrations UX.

Common Flyway migrations extend through **V39**.

## Wiki publishing

The main repository `wiki/` directory is canonical. `.github/workflows/wiki-sync.yml` automatically publishes merged Wiki changes to the GitHub Wiki after they reach `main`, using `scripts/publish-wiki.ps1`.

## Historical material

Older planning/recovery files remain implementation history. They are not current specifications where they conflict with code/tests, migrations, checkpoint documents or focused current guides.

## Next product milestone

Recommended: **enterprise SSO / identity federation**, followed by advanced authorization/explain-access and deeper operational recovery/load validation. Prefer OIDC first behind a provider-neutral federation boundary, with SAML added where required.
