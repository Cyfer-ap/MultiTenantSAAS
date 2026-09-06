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

The project has completed **Billing & Payments at application level**, reviewed through PR #98.

Delivered billing/platform capabilities include:

- provider-neutral Stripe/Razorpay billing
- signed durable webhooks and lifecycle synchronization
- professional plan/provider checkout UX
- provider-backed cancellation with linkage recovery
- idempotent stale-terminal-state repair
- operations visibility and read-only reconciliation
- durable usage metering
- tenant API keys and isolated external API authentication
- per-plan external API quotas
- checkout recovery from read-only workspaces

Stripe is validated in deployed Test Mode for checkout, signed lifecycle webhooks and provider-side cancellation. Razorpay remains provider-sandbox blocked at recurring Test Mode authorization; this does not keep the application billing milestone open.

Application plan creation does not automatically provision Stripe Products/Prices or Razorpay Plans. Provider mappings remain server-side configuration.

Portable Flyway migrations extend through **V33**.

## Next product milestone

Recommended next major feature: **tenant-configurable outbound webhooks**.

Other roadmap items include enterprise SSO, authorization delegation/explain-access, backup/restore drills, monitoring/alerts and broader load/failure-recovery testing.

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

GitHub Actions remains authoritative where Docker is unavailable.

## Wiki

Version-controlled Wiki source lives under `wiki/`. Publish with:

```powershell
.\scripts\publish-wiki.ps1
```
