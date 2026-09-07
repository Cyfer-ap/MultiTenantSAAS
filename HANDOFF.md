# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Application state reviewed through: PR #106 (486f592)
Date: 2026-09-07
Current phase: billing/catalog lifecycle complete at application level
Recommended next product milestone: tenant-configurable outbound webhooks
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/subscription_billing.md`
4. `wiki/Subscriptions-and-Quotas.md`
5. `wiki/Production-Deployment.md`
6. `wiki/Testing-and-CI.md`
7. `wiki/Roadmap.md`

## Current result

Billing is complete at application level through PR #106.

Implemented capabilities include provider-neutral checkout, Stripe/Razorpay adapters, signed durable webhooks, lifecycle synchronization, verified provider-aware cancellation, reconciliation, usage/API-key metering, quotas, managed provider catalog provisioning, safe terminal plan retirement, immutable purchased terms/history and tenant/system-admin history UX.

Stripe remains the validated deployed Test Mode payment path. The prior missing `customer.subscription.deleted` webhook configuration has been corrected and stale terminal state can be repaired idempotently.

Razorpay catalog provisioning is implemented, but recurring Test Mode authorization remains externally provider-sandbox blocked. Do not treat that sandbox limitation as unfinished application architecture.

## Provider catalog lifecycle

System-admin paid-plan management now provisions enabled managed providers automatically:

- Stripe Product + recurring Price; economic edits create replacement Prices.
- Razorpay Plan; provider-visible edits create replacement Plans and archive old local mappings.
- TEST/LIVE mappings are persisted in `subscription_plan_provider_mappings`.
- legacy environment mappings remain compatibility/import paths.

Plan lifecycle semantics:

- `ACTIVE`: purchasable and usable
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal; no new checkout, existing valid subscriptions continue through the current paid period and are scheduled to stop renewing

Never hard-delete a used billing plan/provider object merely to remove it from future sales. Preserve historical references.

## History invariants

- purchased plan terms are immutable snapshots on the tenant subscription
- V36 history records material subscription state transitions
- tenant history hides provider subscription references
- system-admin history may expose provider references for operational troubleshooting
- normal provider-linked lifecycle remains webhook-authoritative

## Boundaries to preserve

- authentication, tenant isolation, authorization, subscription lifecycle and quotas remain separate
- checkout may bypass lifecycle read-only enforcement only as an explicit recovery action; normal tenant authorization still applies
- verified provider lookup/reconciliation may repair stale terminal state defensively
- provider identifiers and secrets remain server-side
- tenant API keys authenticate only `/api/external/**` and cannot impersonate users
- never expose or commit provider keys, webhook secrets or provider plan/price IDs

## Database checkpoint

Common migrations extend through **V36**. Never rewrite an applied Flyway migration.

## Next action

Start **tenant-configurable outbound webhooks** rather than extending billing around Razorpay sandbox behavior.

Suggested rollout:

1. endpoint registration, event subscriptions, signing secrets, tenant authorization and SSRF-safe URL validation
2. durable delivery/outbox, retries, HMAC signatures, idempotency and leasing
3. product-domain event integration plus delivery replay
4. tenant-admin delivery/history UX

Then consider enterprise SSO, authorization delegation/explain-access and deeper operational recovery/load testing.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require Backend, PostgreSQL/Flyway, Frontend, Repository Hygiene, Security, Container CI and Qodana to pass. Wiki source changes should also pass `Wiki Sync / Validate Wiki Source`.
