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
- `outbound-webhook-events.md`
- `outbound-webhook-delivery-history.md`
- `outbound-webhook-admin-ux.md`
- `postgresql_and_migrations.md`
- `frontend_architecture.md`
- `frontend_testing.md`
- `collaboration_and_notifications.md`
- `DEFERRED_PLATFORM_WORK.md`
- `CHECKPOINT.md`
- `HANDOFF.md`

`progress.md`, Step 39/40 notes and older plans are retained as history; their old status claims are not authoritative.

## Current phase

The project has completed both **Billing & Payments** and **tenant-configurable outbound webhooks** at application level, reviewed through PR #112 (`8324ae9`).

### Billing/platform capabilities

- provider-neutral Stripe/Razorpay billing
- signed durable provider webhooks and lifecycle synchronization
- professional plan/provider checkout UX
- provider-backed cancellation with linkage recovery
- idempotent stale-terminal-state repair
- managed Stripe/Razorpay provider catalogs
- safe terminal plan retirement and immutable billing history
- operations visibility and read-only reconciliation
- durable usage metering
- tenant API keys and per-plan external API quotas

Stripe is validated in deployed Test Mode for checkout, signed lifecycle webhooks and provider-side cancellation. Razorpay remains provider-sandbox blocked at recurring Test Mode authorization; this does not keep the application billing milestone open.

### Outbound integration capabilities

- tenant endpoint/event-subscription lifecycle
- generated/rotatable signing secrets encrypted at rest
- HTTPS/public-routable SSRF protection
- durable immutable event/delivery/attempt persistence
- HMAC-SHA256 delivery signing
- lease-safe retries/backoff/timeouts and stale-lease recovery
- transactional domain event publication
- delivery history/detail and terminal replay
- permission-gated tenant Integrations UX

Portable Flyway migrations extend through **V39**.

## Next product milestone

Recommended next major feature: **enterprise SSO / identity federation**.

Use a provider-neutral federation boundary, implement OIDC first, and add SAML only where enterprise requirements justify it. Follow-up roadmap items include authorization delegation/explain-access, backup/restore drills, monitoring/alerts and broader load/failure-recovery testing.

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

Version-controlled Wiki source lives under `wiki/` and is canonical. Pull requests that change Wiki source run a no-push validation in `.github/workflows/wiki-sync.yml`. After those changes reach `main`, the same workflow automatically publishes them to the live GitHub Wiki using `scripts/publish-wiki.ps1`.

Manual publishing is only a fallback. See `wiki/Wiki-Maintenance.md` for the synchronization policy.
