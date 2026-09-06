# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Application state reviewed through: PR #98 (87319f8)
Date: 2026-09-06
Current phase: Billing & Payments complete at application level
Recommended next product milestone: tenant-configurable outbound webhooks
```

## Read first

1. `readme.md`
2. `CHECKPOINT.md`
3. `guides/HANDOFF.md`
4. `guides/subscription_billing.md`
5. `wiki/Subscriptions-and-Quotas.md`
6. `wiki/Production-Deployment.md`
7. `wiki/Testing-and-CI.md`
8. `wiki/Roadmap.md`

## Current result

Billing & Payments is **complete at application level** through PR #98.

Implemented capabilities include provider-neutral checkout, Stripe/Razorpay adapters, signed durable webhooks, lifecycle synchronization, cancellation, provider-linkage recovery, reconciliation, metering, tenant API keys, API quotas and professional plan/provider checkout UX.

Stripe works in deployed Test Mode, including hosted checkout, signed lifecycle webhooks and provider-side cancellation. The final cancellation incident was not a failed Stripe cancellation: Stripe had cancelled successfully, but the configured webhook endpoint initially omitted `customer.subscription.deleted`, leaving local state `ACTIVE`. The endpoint now includes created/updated/deleted subscription events, and PR #98 adds idempotent recovery for already-terminal provider state.

Razorpay remains externally blocked at Test Mode recurring authorization. The application integration stays available, but attempted sandbox cards fail before recurring authorization completes. This is not considered unfinished application billing architecture.

## Plan/provider boundary

Application plans and payment-provider billing objects are separate.

Creating a new application plan through system administration does **not** automatically provision Stripe Products/Prices or Razorpay Plans. Current checkout requires explicit server-side plan-code mappings to provider IDs. Automatic provider provisioning is optional future work.

## Boundaries to preserve

- authentication, tenant isolation, authorization, subscription lifecycle and quotas remain separate
- checkout may bypass lifecycle read-only enforcement only as an explicit recovery action; normal tenant authorization still applies
- normal local subscription lifecycle is webhook-authoritative
- verified provider lookup may repair stale terminal state defensively
- provider identifiers, plan mappings and secrets remain server-side
- tenant API keys authenticate only `/api/external/**` and cannot impersonate users
- never expose or commit provider keys, secrets, webhook secrets or plan/price IDs

## Documentation/Wiki workflow

The root docs, focused guides and `wiki/*.md` source are refreshed through this checkpoint. `wiki/*.md` is canonical.

`.github/workflows/wiki-sync.yml` validates Wiki source on relevant pull requests and automatically publishes merged `main` changes to the live GitHub Wiki using `scripts/publish-wiki.ps1`. Manual Wiki publishing is a fallback only.

Historical planning files remain historical; current code/tests, migrations and focused status docs take precedence.

## Next action

Do not continue adding billing features just to work around Razorpay sandbox behavior.

Recommended next major feature: **tenant-configurable outbound webhooks**. Existing collaboration, notifications and S3/R2 attachment foundations are already implemented, so outbound integrations provide higher incremental product value.

Subsequent candidates: enterprise SSO, authorization delegation/explain-access, backup/restore/monitoring/runbooks and broader load/failure-recovery testing.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require backend, PostgreSQL/Flyway, frontend, repository hygiene, security, container and Qodana checks to pass. Relevant Wiki changes should also pass the `Wiki Sync / Validate Wiki Source` job.
