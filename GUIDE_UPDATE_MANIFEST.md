# Guide Update Manifest

Current documentation refresh:

```text
Repository: Cyfer-ap/MultiTenantSAAS
Reviewed application state: post-PR #112 (8324ae9)
Snapshot date: 2026-09-07
Phase: tenant-configurable outbound webhooks complete at application level
Next recommended product milestone: enterprise SSO / identity federation
```

## Updated status documents

Root:

- `readme.md`
- `CHECKPOINT.md`
- `HANDOFF.md`
- `PACKAGE_INDEX.md`
- `MANIFEST.json`
- `GUIDE_UPDATE_MANIFEST.md`

Guides:

- `guides/README.md`
- `guides/progress.md`
- `guides/CHECKPOINT.md`
- `guides/HANDOFF.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `guides/outbound-webhook-events.md`
- `guides/outbound-webhook-delivery-history.md`
- `guides/outbound-webhook-admin-ux.md`

Wiki source:

- `wiki/Home.md`
- `wiki/Developer-Handoff.md`
- `wiki/Roadmap.md`
- `wiki/Notifications.md`

## Status recorded

This refresh advances the repository from the post-#106 billing/catalog checkpoint through the completed outbound-webhook sequence.

PRs #108–#112 add:

- V37 tenant-scoped endpoint/event-subscription configuration
- generated/rotatable signing secrets encrypted at rest
- HTTPS/public-routable SSRF protections
- V38 immutable outbound events and durable endpoint-specific deliveries
- HMAC-SHA256 signing with stable event identity/body
- lease-safe retries/backoff/timeouts/stale-lease recovery
- transactional project/task/comment/member/subscription event publication
- V39 immutable delivery-attempt history
- tenant-scoped delivery history/detail and guarded manual replay
- permission-gated tenant Integrations endpoint/delivery UX

Billing/provider status is unchanged: Stripe remains the validated deployed Test Mode payment path. Razorpay integration and managed Plan provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

Common Flyway migrations now extend through V39.

No credentials, outbound signing secrets or provider plan/price IDs are recorded in documentation.

## Wiki synchronization

`wiki/*.md` remains canonical Wiki source. After this refresh reaches `main`, the `Wiki Sync` workflow automatically publishes the source to `MultiTenantSAAS.wiki.git`; manual Wiki publishing remains fallback-only.
