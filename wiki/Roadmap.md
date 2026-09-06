# Roadmap

## Current

The billing milestone is closed at application level through PR #98.

Completed billing/platform capabilities include:

- provider-neutral Stripe/Razorpay checkout
- professional plan/provider checkout UX
- signed durable webhooks
- webhook-driven subscription lifecycle
- cancellation, provider-linkage recovery and stale-state repair
- operations visibility and reconciliation
- durable usage metering
- tenant API keys and external API authentication
- plan-level API request quotas
- checkout recovery for read-only workspaces

Stripe is validated in deployed Test Mode for hosted Checkout, signed lifecycle synchronization and provider-side cancellation.

Razorpay application integration is implemented, but recurring Test Mode authorization remains provider-sandbox blocked. This is no longer an active application-development blocker.

## Next major product milestone

### 1. Tenant-configurable outbound webhooks

Recommended next feature.

Target capabilities:

- tenant-admin endpoint registration
- event subscription selection
- encrypted/signing-secret management
- HMAC-signed outbound deliveries
- durable delivery records
- retry/backoff/lease/idempotency semantics
- delivery history and failure visibility
- manual replay
- tenant isolation and scoped authorization
- initial domain-event catalogue for projects, tasks, collaboration, membership and selected subscription events

This reuses proven platform patterns from durable notifications and inbound billing webhooks while exposing a high-value integration surface to tenant systems.

## Following platform work

2. enterprise SSO
3. authorization delegation and explain-access
4. backup/restore drills, monitoring, alerts and operational runbooks
5. broader load/failure-recovery and production R2 verification
6. optional notification expansion such as digests/live browser delivery

## Independent provider/live-readiness track

- preserve the working Stripe Test Mode path
- keep Razorpay integration available while its sandbox authorization remains blocked
- rotate any exposed test credentials
- enable live keys/plans only after provider-specific readiness review
- automatic Stripe/Razorpay plan provisioning may be evaluated later; current system-admin plan creation does not create provider Products/Prices/Plans

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, webhook-authoritative normal billing state, verified provider reconciliation, Flyway invariants, database-backed concurrency, auditability and server-only secret/provider mappings.
