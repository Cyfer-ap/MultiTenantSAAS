# Deferred Platform Work

Reviewed through PR #106 on 2026-09-07. Retire an item only when it is implemented and verified at the appropriate boundary.

## Recently delivered

- provider-neutral billing and Stripe/Razorpay adapters — PRs #67-#70
- tenant checkout API, signed webhooks and subscription synchronization — PRs #71-#73
- provider-backed cancellation, operations visibility and reconciliation — PRs #74-#76
- durable usage metering — PR #77
- tenant API-key lifecycle, authentication and metering — PRs #78-#79
- plan-level API request quotas — PR #80
- tenant checkout discovery/UI and recovery fixes — PRs #81-#84
- parallel Stripe deployment and startup hardening — PRs #89-#90
- professional subscription UX and security/regression hardening — PRs #92-#94
- provider-aware cancellation recovery and cross-provider linkage protection — PRs #95-#96
- Stripe cancellation API correction and stale-terminal-state repair — PRs #97-#98
- billing checkpoint/Wiki automation — PR #99
- provider catalog lifecycle foundation and immutable purchased snapshots — PR #100
- managed Stripe Product/Price provisioning/versioning — PR #101
- safe plan retirement and period-end provider cleanup — PR #102
- Stripe retirement startup regression fix — PR #103
- immutable tenant subscription history backend/API — PR #104
- tenant/system-admin billing-history and retired-plan UX — PR #105
- managed Razorpay Plan provisioning/versioning — PR #106

**Billing/catalog lifecycle is complete at application level.** It is no longer deferred platform work.

## Provider/live-readiness debt

### Razorpay sandbox authorization

Razorpay application integration and managed Plan provisioning are implemented, but attempted Test Mode recurring-card authorization fails inside Razorpay before authorization completes. This is an external provider-sandbox limitation and is not an active application-development blocker.

Revisit only when provider validation/live readiness is required. Preserve structured diagnostics and rotate exposed test credentials.

### Live-provider readiness

Before enabling live billing for any provider:

- complete provider-specific account/KYC/readiness requirements
- configure/verify LIVE provider catalog mappings and live webhook endpoints/secrets
- execute provider-specific checkout, lifecycle, cancellation and reconciliation smoke tests
- confirm taxes/compliance/customer communications as required
- establish operational alerting, rollback and runbook procedures

Automatic provider catalog provisioning is no longer deferred: Stripe and Razorpay managed catalog flows are implemented through PR #106.

## Remaining platform work

1. **Tenant outbound webhooks** — recommended next product milestone
   - tenant-configurable endpoints and event subscriptions
   - HMAC signing and secret rotation
   - durable retries/backoff/leases/idempotency
   - delivery logs and manual replay
   - SSRF protections and tenant authorization

2. **Enterprise SSO**
   - OIDC/SAML-style sign-in, account linking, discovery and enforcement

3. **Advanced authorization**
   - temporary/scoped delegation
   - explain-access API/UI

4. **Operational recovery and alerting**
   - database backup/restore drills
   - actionable service/database alerts and runbooks
   - broader load and failure-recovery verification
   - production R2 operational validation

5. **Optional notification expansion**
   - invitation events, digests, live browser delivery and admin observability

## Revisit rule

Revisit a deferred item when product work depends on it or before an external integration is described as production-ready.
