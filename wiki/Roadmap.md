# Roadmap

## Current

The active phase is **Razorpay Test Mode validation and billing operational hardening**.

Billing foundations completed through PR #84:

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

## Active blocker

Razorpay hosted Test Mode checkout opens but every attempted card fails before recurring authorization. A real activation webhook and local activation have not been observed. Live plans and keys remain deferred.

## Near term

1. isolate the failure with a Dashboard-created Razorpay subscription
2. capture structured provider diagnostics and escalate to Razorpay Support if needed
3. optionally add a feature-flagged, system-admin-only billing simulator
4. complete deployed Razorpay activation, charged/pending/halted, cancellation, replay and reconciliation tests
5. rotate any exposed test credentials
6. move to live-mode readiness only after the sandbox lifecycle succeeds

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
