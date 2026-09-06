# Guide Update Manifest

Current documentation refresh:

```text
Repository: Cyfer-ap/MultiTenantSAAS
Reviewed application state: post-PR #98 (87319f8)
Snapshot date: 2026-09-06
Phase: Billing & Payments complete at application level
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

- `guides/README.md`
- `guides/progress.md`
- `guides/CHECKPOINT.md`
- `guides/HANDOFF.md`
- `guides/subscription_billing.md`
- `guides/current_architecture.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `guides/collaboration_and_notifications.md`

Wiki source:

- `wiki/Home.md`
- `wiki/Architecture.md`
- `wiki/Developer-Handoff.md`
- `wiki/Roadmap.md`
- `wiki/Subscriptions-and-Quotas.md`
- `wiki/Production-Deployment.md`
- `wiki/Testing-and-CI.md`
- `wiki/Notifications.md`
- `wiki/Wiki-Maintenance.md`

Automation:

- `scripts/publish-wiki.ps1`
- `.github/workflows/wiki-sync.yml`

## Status recorded

The refresh closes the billing milestone at the application boundary and records the implementation/hardening sequence through PR #98.

Stripe is the validated deployed Test Mode path: hosted Checkout, signed subscription lifecycle synchronization and provider-side cancellation are working. The final cancellation synchronization problem was traced to the Stripe webhook endpoint not subscribing to `customer.subscription.deleted`; that endpoint has been corrected and PR #98 adds idempotent repair when Stripe is already terminal but local state is stale.

Razorpay remains implemented and available, but Test Mode recurring authorization is blocked by provider-side sandbox/card behavior. This is recorded as an external provider limitation rather than unfinished application billing architecture.

System-admin-created application plans do not currently auto-provision Stripe Products/Prices or Razorpay Plans. Provider plan/price mapping remains explicit server-side configuration; automatic provider catalog provisioning is optional future work.

No credentials, webhook secrets or provider plan/price IDs are recorded in documentation.

## Wiki synchronization

`wiki/*.md` remains the canonical Wiki source. After this refresh reaches `main`, the `Wiki Sync` GitHub Actions workflow automatically publishes the source to `MultiTenantSAAS.wiki.git`; manual Wiki publishing is retained only as a fallback.
