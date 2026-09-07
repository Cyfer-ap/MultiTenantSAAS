# Documentation Package Index

Current snapshot: post-PR #106 (`486f592`), 2026-09-07.

## Primary status documents

- `readme.md` — platform overview, provider/catalog status and next milestone
- `CHECKPOINT.md` — concise verified application checkpoint
- `HANDOFF.md` — session-independent resume instructions
- `guides/subscription_billing.md` — billing contracts, provider catalog lifecycle and closure status
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining platform/live-readiness work
- `wiki/Home.md` — version-controlled Wiki entry point
- `wiki/Roadmap.md` — current platform sequence
- `wiki/Wiki-Maintenance.md` — automatic Wiki synchronization policy
- `MANIFEST.json` — machine-readable documentation inventory

## Billing/catalog milestone status

**Billing, cancellation hardening and managed provider catalogs are complete at application level through PR #106.**

Stripe remains the validated deployed Test Mode payment path and now supports managed Product/Price provisioning/versioning. Razorpay application integration and managed Plan provisioning are implemented, while recurring Test Mode authorization remains provider-sandbox blocked.

System-admin paid-plan management now uses durable TEST/LIVE provider mappings. Safe terminal retirement preserves existing paid-period entitlement, immutable purchased-plan snapshots/history preserve prior terms, and tenant/system-admin billing history is available in the UI.

Common Flyway migrations extend through **V36**.

## Wiki publishing

The main repository `wiki/` directory is canonical. `.github/workflows/wiki-sync.yml` automatically publishes merged Wiki changes to the GitHub Wiki after they reach `main`, using `scripts/publish-wiki.ps1`.

## Historical material

Older planning/recovery files remain implementation history. They are not current specifications where they conflict with code/tests, migrations, checkpoint documents or focused current guides.

## Next product milestone

Recommended: **tenant-configurable outbound webhooks**, followed by enterprise SSO, advanced authorization/explain-access and deeper operational recovery/load validation.
