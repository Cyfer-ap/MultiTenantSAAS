# Documentation Package Index

Current snapshot: post-PR #90 (`e9257b3`), 2026-08-27.

## Primary status documents

- `readme.md` — platform overview and current billing status
- `CHECKPOINT.md` — concise verified checkpoint
- `HANDOFF.md` — session-independent resume instructions
- `guides/README.md` — guide routing and source-of-truth rules
- `guides/subscription_billing.md` — billing contracts, configuration and Razorpay blocker
- `guides/DEFERRED_PLATFORM_WORK.md` — remaining work
- `wiki/Home.md` — version-controlled Wiki entry point
- `wiki/Roadmap.md` — current sequence
- `MANIFEST.json` — documentation inventory

## Historical material

Step 39/40, Authorization V2 and older planning/progress files remain for implementation history. Where they conflict with the primary status documents, current code/tests, migrations and focused current guides win.

## Current provider status

Stripe works end to end in deployed Test Mode. Razorpay hosted Test Mode checkout opens, but payment authorization still fails within Razorpay. Live-mode work remains deferred.
