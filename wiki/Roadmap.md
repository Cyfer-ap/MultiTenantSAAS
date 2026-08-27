# Roadmap

## Current

The active phase is **billing provider validation and operational hardening**.

Billing foundations completed through PR #90:

- provider-neutral checkout
- Stripe and Razorpay adapters
- signed durable webhooks
- webhook-driven subscription lifecycle
- cancellation, operations visibility and reconciliation
- durable usage metering
- tenant API keys and external API authentication
- plan-level API request quotas
- tenant plan discovery and hosted-checkout UI
- checkout recovery for read-only workspaces

Stripe Test Mode is validated end to end in deployment: hosted Checkout completes and signed webhook synchronization updates local subscription state.

## Active blocker

Razorpay hosted Test Mode checkout opens but every attempted card fails before recurring authorization. A real activation webhook and local activation have not been observed. Live plans and keys remain deferred.

## Near term

1. preserve and monitor the working Stripe Test Mode path
2. isolate the failure with a Dashboard-created Razorpay subscription
3. capture structured provider diagnostics and escalate to Razorpay Support if needed
4. optionally add a feature-flagged, system-admin-only billing simulator
5. complete deployed Razorpay activation, charged/pending/halted, cancellation, replay and reconciliation tests
6. rotate any exposed test credentials
7. move each provider to live-mode readiness only after provider-specific review

## Next platform work

- backup/restore drills, monitoring, alerts and operational runbooks
- tenant-configurable outbound webhooks
- broader load/failure-recovery and production R2 verification
- enterprise SSO
- authorization delegation and explain-access
- optional notification expansion

API keys and durable usage metering are implemented and must not be listed as future foundations.

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, webhook-authoritative billing state, Flyway invariants, database-backed concurrency, auditability and server-only secret/provider mappings.
