# Guide Update Manifest

Current documentation refresh:

```text
Repository: Cyfer-ap/MultiTenantSAAS
Reviewed application state: post-PR #106 (486f592)
Snapshot date: 2026-09-07
Phase: billing/catalog lifecycle complete at application level
Next recommended product milestone: tenant-configurable outbound webhooks
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

- `guides/progress.md`
- `guides/CHECKPOINT.md`
- `guides/subscription_billing.md`

Wiki source:

- `wiki/Home.md`
- `wiki/Developer-Handoff.md`
- `wiki/Roadmap.md`
- `wiki/Subscriptions-and-Quotas.md`

## Status recorded

This refresh supersedes the post-#98/#99 statement that provider catalog provisioning was future work.

PRs #100–#106 add:

- `ACTIVE` / `INACTIVE` / terminal `RETIRED` plan lifecycle
- durable TEST/LIVE provider catalog mappings
- immutable purchased-plan snapshots
- automatic Stripe Product/Price provisioning and Price replacement
- safe Stripe/Razorpay plan retirement with period-end/cycle-end cancellation
- durable retirement operations
- immutable tenant subscription history
- tenant/system-admin billing-history UX
- automatic Razorpay Plan provisioning/replacement and legacy mapping compatibility

Stripe remains the validated deployed Test Mode payment path. Razorpay integration and managed Plan provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

Common Flyway migrations now extend through V36.

No credentials, webhook secrets or provider plan/price IDs are recorded in documentation.

## Wiki synchronization

`wiki/*.md` remains canonical Wiki source. After this refresh reaches `main`, the `Wiki Sync` workflow automatically publishes the source to `MultiTenantSAAS.wiki.git`; manual Wiki publishing remains fallback-only.
