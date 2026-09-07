# Multi-Tenant SaaS Platform — Current Progress

Snapshot date: 2026-09-07
Reviewed state: post-PR #106 (`486f592`)
Current stage: **billing/catalog lifecycle complete; outbound webhooks next**

This page is a concise progress index. Code, tests, migrations, `CHECKPOINT.md` and focused guides are authoritative.

## Completed foundations

- tenant and system-admin control planes
- secure browser authentication/session lifecycle
- tenant isolation and scoped permission authorization
- organization hierarchy, projects, tasks and collaboration
- R2/S3-compatible attachments
- durable notifications and email delivery
- subscription lifecycle, read-only restrictions and resource quotas
- PostgreSQL/Flyway/Testcontainers path
- production profiles, observability, CI, security and container checks

## Billing and API platform — complete at application level

- provider-neutral Stripe/Razorpay billing boundary
- professional plan/provider/hosted-checkout UX
- signed durable provider webhooks and replay protection
- webhook-driven subscription synchronization
- provider-aware period-end cancellation and provider-linkage recovery
- cross-provider overwrite protection
- read-only reconciliation and stale-terminal-state repair
- `ACTIVE` / `INACTIVE` / terminal `RETIRED` plan lifecycle
- durable TEST/LIVE provider catalog mappings
- automatic Stripe Product/Price provisioning and immutable Price replacement
- automatic Razorpay Plan provisioning and replacement mappings
- safe plan retirement with existing subscription entitlement preserved through current period
- immutable purchased-plan snapshots
- immutable subscription history plus tenant/system-admin history UI
- durable usage metering
- tenant API-key lifecycle/authentication/metering
- per-plan API request quotas with atomic enforcement
- checkout recovery for read-only workspaces
- billing/security/startup regression coverage

### Stripe

Stripe is the validated deployed Test Mode payment path. Hosted checkout, Test cards, signed lifecycle webhooks, provider-side cancellation and reconciliation have been validated. Managed plan creation/update/retirement now provisions and versions Stripe catalog objects automatically.

### Razorpay

Razorpay application integration and managed Plan provisioning are implemented. Test Mode recurring authorization remains externally blocked: hosted checkout opens, while attempted sandbox cards fail before recurring authorization. This remains a provider sandbox limitation rather than unfinished application architecture.

### Provider catalog

Application plans remain separate from provider objects but are connected by durable environment-specific mappings. Legacy configured IDs remain supported for compatibility/import. Historical mappings are retained so existing subscriptions and billing history can resolve prior provider objects safely.

## Database

Common Flyway migrations extend through **V36**:

- V34 provider catalog mappings and purchased-plan snapshots
- V35 durable retirement operations
- V36 immutable tenant subscription history

## Next

Recommended next major product milestone: **tenant-configurable outbound webhooks**.

Suggested sequence: endpoint/signing foundation → durable delivery engine → product event integration/replay → tenant-admin delivery UX.

Then consider enterprise SSO, authorization delegation/explain-access, backup/restore drills, monitoring/alerts and broader load/failure-recovery testing. Provider live-mode readiness remains an independent deployment/operations track.
