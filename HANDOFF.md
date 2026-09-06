# MultiTenantSAAS — Development Handoff

Use this document to resume without relying on chat history.

## Repository checkpoint

```text
Repository: Cyfer-ap/MultiTenantSAAS
Branch: main
Reviewed state: post-PR #98 (87319f8)
Date: 2026-09-06
Current phase: billing complete at application level; next product milestone selection
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

Implemented capabilities include provider-neutral checkout, Stripe/Razorpay adapters, signed durable webhooks, lifecycle synchronization, cancellation, provider-linkage recovery, reconciliation, metering, API keys, API quotas and professional plan/provider checkout UX.

Stripe works in deployed Test Mode, including successful hosted checkout, signed lifecycle webhooks and provider-side cancellation. The final cancellation bug was not a failed Stripe cancel request: Stripe had cancelled correctly, but the configured webhook endpoint initially omitted `customer.subscription.deleted`, so local state remained `ACTIVE`. The Stripe endpoint now includes created/updated/deleted subscription events, and PR #98 adds idempotent repair for already-terminal provider subscriptions.

Razorpay remains externally blocked at Test Mode recurring authorization. The application integration stays enabled/available, but attempted sandbox cards fail before recurring authorization completes. This is not considered unfinished application billing architecture.

## Plan/provider boundary

Application plans and payment-provider billing objects are intentionally separate.

Creating a new application plan through system administration does **not** automatically provision Stripe Products/Prices or Razorpay Plans. Current provider checkout requires server-side plan-code mappings to provider IDs. Automatic provider provisioning may be designed later as a separate feature.

## Boundaries to preserve

- authentication, tenant isolation, authorization, subscription lifecycle and quotas remain separate
- checkout may bypass lifecycle read-only enforcement only as an explicit recovery action; `tenant.update` is still required
- local subscription lifecycle is webhook-authoritative during normal operation
- reconciliation/provider lookup may repair stale terminal state only through verified provider state
- provider and plan mapping comes only from server-side configuration/verified metadata
- API keys authenticate only `/api/external/**`, are tenant-bound and cannot impersonate users
- never expose or commit provider keys, secrets, webhook secrets or plan/price IDs

## Next action

Do not continue adding billing features simply to work around Razorpay sandbox behavior.

Recommended next major product milestone: **tenant-configurable outbound webhooks**. Existing collaboration, notifications and S3/R2 attachment foundations are already implemented, so outbound integrations provide higher incremental product value.

Subsequent roadmap candidates: enterprise SSO, authorization delegation/explain-access, and deeper backup/restore/load/failure-recovery operations.

## Verification

GitHub Actions remains authoritative where local Docker is unavailable. Before merge require backend, PostgreSQL/Flyway, frontend, repository hygiene, security, container and Qodana checks to pass.
