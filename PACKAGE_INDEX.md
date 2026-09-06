# Documentation Package Index

Current snapshot: post-PR #98 (`87319f8`), 2026-09-06.

## Primary status documents

- `readme.md` — platform overview, current provider status and next milestone
- `CHECKPOINT.md` — concise verified application checkpoint
- `HANDOFF.md` — session-independent resume instructions
- `guides/README.md` — guide routing and source-of-truth rules
- `guides/subscription_billing.md` — billing contracts, provider boundaries and closure status
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining platform/live-readiness work
- `wiki/Home.md` — version-controlled Wiki entry point
- `wiki/Roadmap.md` — current platform sequence
- `wiki/Wiki-Maintenance.md` — automatic Wiki synchronization policy
- `MANIFEST.json` — machine-readable documentation inventory

## Billing milestone status

**Billing & Payments is complete at application level.**

Stripe is the validated deployed Test Mode provider for hosted checkout, signed lifecycle synchronization and cancellation. The Stripe webhook endpoint now includes `customer.subscription.deleted`, and PR #98 provides idempotent recovery when provider state is already terminal but local state is stale.

Razorpay remains implemented but Test Mode recurring authorization is provider-sandbox blocked. This does not keep the application billing milestone open.

Application plan creation remains separate from provider catalog provisioning: a system-admin-created plan is not automatically created as a Stripe Product/Price or Razorpay Plan.

## Wiki publishing

The main repository `wiki/` directory is canonical. `.github/workflows/wiki-sync.yml` automatically publishes merged Wiki changes to the GitHub Wiki after they reach `main`, using `scripts/publish-wiki.ps1`.

## Historical material

Step 39/40 notes, Authorization V2 plans, `Plan.txt`, `Details.txt`, `Wild_Thoughts.md` and other older planning/recovery files are retained for implementation history. They are not rewritten to masquerade as current specifications. Where historical material conflicts with current code/tests, migrations, checkpoint documents or focused current guides, the current sources win.

## Next product milestone

Recommended: **tenant-configurable outbound webhooks**, followed by enterprise SSO, advanced authorization/explain-access and deeper operational recovery/load validation.
