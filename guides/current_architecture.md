# Current architecture

Reviewed through PR #84 on 2026-08-26.

## Platform

- Java 21 / Spring Boot 4.0.7 backend
- React 19.2 / TypeScript 6 / Vite 8 frontend
- PostgreSQL 17 with Flyway and Testcontainers
- separate tenant and system-admin identity/control planes

## Enforcement pipeline

```text
authentication
-> tenant isolation
-> authorization
-> subscription lifecycle
-> resource/API quotas
-> domain invariant
-> transaction/database constraint
```

Checkout is an explicit subscription-recovery mutation; it bypasses lifecycle read-only blocking only, not tenant authorization.

## Major subsystems

- authentication, users, invitations and organization hierarchy
- scoped permission authorization
- projects, tasks, collaboration, attachments and notifications
- internal subscription/entitlement/access evaluation
- provider-neutral billing with Stripe and Razorpay adapters
- signed durable billing webhooks and lifecycle synchronization
- cancellation, operations visibility and read-only reconciliation
- durable usage metering and plan limits
- tenant API keys and isolated external API authentication
- audit logs, observability and production profiles

## Billing data flow

```text
app plan + server-side provider mapping
        -> hosted provider checkout
        -> signed provider webhook
        -> durable billing event
        -> locked tenant subscription update
        -> access and quota evaluation
```

Provider secrets and external plan IDs never cross into the frontend.

## Current operational boundary

The application-side Razorpay flow is implemented, but real Test Mode payment authorization is failing at Razorpay checkout. Live billing is not enabled and the integration is not production-ready.
