# Multi-Tenant SaaS Platform — Current Progress

Snapshot date: 2026-09-07
Reviewed state: post-PR #112 (`8324ae9`)
Current stage: **outbound webhooks complete; enterprise SSO next**

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

Stripe is the validated deployed Test Mode payment path. Hosted checkout, Test cards, signed lifecycle webhooks, provider-side cancellation and reconciliation have been validated. Managed plan creation/update/retirement provisions and versions Stripe catalog objects automatically.

### Razorpay

Razorpay application integration and managed Plan provisioning are implemented. Test Mode recurring authorization remains externally blocked: hosted checkout opens, while attempted sandbox cards fail before recurring authorization. This remains a provider sandbox limitation rather than unfinished application architecture.

## Tenant outbound webhooks — complete at application level

PRs #108–#112 deliver:

- tenant endpoint registration, event subscriptions, enable/disable and archive lifecycle
- generated/rotatable signing secrets with AES-256-GCM storage
- HTTPS-only SSRF-safe URL validation with delivery-time DNS revalidation
- V38 durable immutable events and endpoint-specific deliveries
- HMAC-SHA256 request signing with stable event identity/body
- lease-safe workers, bounded retries, exponential backoff, timeouts and stale-lease recovery
- transactional project/task/comment/member/subscription event publication
- V39 immutable delivery-attempt history
- tenant-scoped delivery history/detail and guarded manual replay
- permission-gated tenant Integrations UX for endpoint and delivery administration

Initial event catalogue covers project, task, comment/reply, membership and selected subscription lifecycle events.

## Database

Common Flyway migrations extend through **V39**:

- V34 provider catalog mappings and purchased-plan snapshots
- V35 durable retirement operations
- V36 immutable tenant subscription history
- V37 outbound webhook endpoints/event subscriptions
- V38 outbound webhook events/deliveries
- V39 outbound webhook delivery attempts

## Next

Recommended next major product milestone: **enterprise SSO / identity federation**.

Suggested sequence: tenant identity-provider configuration → provider-neutral federation boundary → OIDC first → account linking/domain discovery → optional/enforced SSO policy with recovery controls → admin/login UX and audit coverage. Add SAML through the same boundary only when enterprise requirements justify it.

Then consider authorization delegation/explain-access, backup/restore drills, monitoring/alerts and broader load/failure-recovery testing. Provider live-mode readiness remains an independent deployment/operations track.
