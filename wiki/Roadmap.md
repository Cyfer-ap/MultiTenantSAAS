# Roadmap

## Current

The billing/catalog lifecycle milestone is closed at application level through PR #106.

Completed capabilities now include:

- provider-neutral Stripe/Razorpay checkout
- professional plan/provider checkout UX
- signed durable webhooks
- webhook-driven subscription lifecycle
- verified provider-aware cancellation and stale-state repair
- durable TEST/LIVE provider catalog mappings
- automatic Stripe Product/Price provisioning and immutable Price replacement
- automatic Razorpay Plan provisioning and replacement mappings
- safe terminal plan retirement with existing paid-period entitlement preserved
- durable retirement operations and provider cleanup retry
- immutable purchased-plan snapshots and tenant subscription history
- tenant/system-admin billing-history UX
- operations visibility and reconciliation
- durable usage metering
- tenant API keys and plan-level API request quotas

Stripe is validated in deployed Test Mode for hosted Checkout, signed lifecycle synchronization and provider-side cancellation.

Razorpay integration and managed Plan provisioning are implemented, but recurring Test Mode authorization remains provider-sandbox blocked. This is not an active application-development blocker.

## Next major product milestone

### 1. Tenant-configurable outbound webhooks

Recommended next feature.

Target capabilities:

- tenant-admin endpoint registration
- event subscription selection
- generated signing secrets and secret rotation
- HMAC-signed outbound deliveries with timestamps and event IDs
- SSRF-safe endpoint validation
- durable outbox/delivery/attempt persistence
- retries, exponential backoff, timeouts, leases and idempotency
- delivery history and failure visibility
- manual replay
- tenant isolation and scoped authorization
- initial event catalogue for projects, tasks, collaboration, membership and selected subscription events

Recommended implementation sequence:

1. endpoint/subscription/signing foundation
2. durable delivery engine
3. domain event integration + replay
4. tenant-admin endpoint/delivery UX

This reuses proven platform patterns from durable notifications and inbound billing webhooks while exposing a high-value integration surface to tenant systems.

## Following platform work

2. enterprise SSO / identity federation
3. authorization delegation and explain-access
4. backup/restore drills, monitoring, alerts and operational runbooks
5. broader load/failure-recovery and production R2 verification
6. optional notification expansion such as digests/live browser delivery

## Independent provider/live-readiness track

- preserve the validated Stripe Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, webhook endpoints, tax/compliance and production operational runbooks separately from feature development

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, webhook-authoritative normal billing state, verified provider reconciliation, immutable purchased/history data, Flyway invariants, database-backed concurrency, auditability and server-only secrets/provider identifiers.
