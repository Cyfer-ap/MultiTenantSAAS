# Subscriptions and Quotas

Reviewed through PR #106 on 2026-09-07.

## Milestone status

**Billing, cancellation hardening and managed provider catalogs are complete at application level.**

Provider live-readiness remains a separate deployment/operations concern.

## Separation of concerns

Keep application plans, stored subscription lifecycle, evaluated access, entitlements, usage and provider objects separate.

Application plans are linked to provider objects through durable TEST/LIVE mappings. Provider IDs and secrets remain server-side.

## Managed provider catalog

### Stripe

New paid application plans can automatically create a Stripe Product and recurring Price. Price/currency/billing-interval changes create replacement Prices rather than rewriting historical economics. Retired catalog objects are unavailable for new sale while historical references remain intact.

### Razorpay

New paid application plans can automatically create Razorpay Plans. Provider-visible edits create replacement Plans and archive the prior local mapping. Historical provider Plan IDs remain resolvable for existing subscriptions/history.

Legacy environment-configured provider IDs remain supported as compatibility/import paths.

## Plan lifecycle

- `ACTIVE`: purchasable and normally entitled
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal; no new checkout, existing valid subscriptions continue through their current paid period and provider renewals are scheduled to stop at period/cycle end

Retirement does not physically destroy used plans, provider mappings or billing history.

## Immutable purchased terms/history

V34 purchased-plan snapshots preserve the terms a tenant bought independently of later catalog changes.

V36 records immutable tenant subscription history. Tenant users see their billing lifecycle/history without internal provider subscription identifiers; system admins retain provider references for operations/troubleshooting.

## Access and recovery

Ordinary tenant writes are rejected with `WORKSPACE_READ_ONLY` when lifecycle access is unavailable. Billing checkout is an explicitly permitted recovery action, but tenant authorization is still required.

Resource limits and per-period API limits are independent of lifecycle access.

## Billing implementation

- checkout configuration discovery and hosted Stripe/Razorpay checkout
- signed durable webhooks with duplicate/replay protection
- webhook-authoritative normal lifecycle mapping
- verified provider-aware period-end cancellation
- provider-linkage/history recovery and stale-terminal-state repair
- safe plan retirement + durable retirement operations
- managed Stripe/Razorpay provider catalogs
- immutable purchased-plan/subscription history
- tenant/system-admin billing history UI
- system-admin billing operations visibility and reconciliation
- append-only usage events
- tenant API-key creation/list/revocation
- API-key authentication only under `/api/external/**`
- plan-level `API_REQUESTS` quota enforcement with atomic consumption

## Current provider status

### Stripe

Stripe is working in deployed Test Mode. Hosted Checkout, Test cards, signed subscription webhooks, provider-side cancellation and reconciliation are the validated payment path.

### Razorpay

Razorpay application integration and managed catalog provisioning are implemented, but Test Mode recurring authorization remains provider-sandbox blocked. Subscription creation/hosted redirect work; attempted test cards fail inside Razorpay before recurring authorization.

This external limitation does not invalidate application/CI coverage and does not keep billing open.

## Database checkpoint

Portable common migrations extend through **V36**.

## Live-mode boundary

Neither Test Mode validation nor mocked provider contracts constitute a production/live-readiness claim. Live credentials/catalogs and production operational checks remain a separate provider-specific track.
