# Guide index

## Source-of-truth order

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical notes

Never modify an applied migration.

## Current guides

- `current_architecture.md`
- `data_model.md`
- `security_model.md`
- `authorization_model.md`
- `subscription_billing.md`
- `postgresql_and_migrations.md`
- `frontend_architecture.md`
- `frontend_testing.md`
- `collaboration_and_notifications.md`
- `DEFERRED_PLATFORM_WORK.md`
- `CHECKPOINT.md`
- `HANDOFF.md`

`progress.md`, Step 39/40 notes and older plans are retained as history; their old status claims are not authoritative.

## Current phase

The project is in **billing provider validation and operational hardening**, reviewed through PR #90.

Delivered billing/platform milestones now include:

- provider-neutral billing and Stripe/Razorpay adapters
- signed, durable webhook ingestion and subscription synchronization
- provider-backed cancellation and read-only reconciliation
- operations visibility
- durable usage metering
- tenant API keys and isolated external API authentication
- per-plan external API quotas
- tenant paid-plan discovery and hosted checkout
- checkout recovery from a read-only workspace

Stripe hosted checkout and webhook-driven subscription synchronization work end to end in the deployed Test Mode environment. Razorpay hosted Test Mode checkout opens but payment authorization still fails inside Razorpay. Live mode remains deferred.

Portable Flyway migrations extend through **V33**.

## Verification baseline

```powershell
cd multitenant-saas
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify

cd ..\multitenant-saas-frontend
npm run format:check
npm run lint
npm test
npm run build
```

GitHub Actions is authoritative where Docker is unavailable. Stripe has also passed a real deployed Test Mode smoke test; Razorpay E2E remains outstanding.

## Wiki

Version-controlled Wiki source lives under `wiki/`. Publish with:

```powershell
.\scripts\publish-wiki.ps1
```
