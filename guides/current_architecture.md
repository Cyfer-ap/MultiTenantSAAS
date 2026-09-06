# Current architecture

Reviewed through PR #98 on 2026-09-06.

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
- professional plan/provider checkout UX
- provider-aware cancellation, linkage recovery and stale-state repair
- operations visibility and read-only reconciliation
- durable usage metering and plan limits
- tenant API keys and isolated external API authentication
- audit logs, observability and production profiles

## Billing data flow

```text
application plan + server-side provider mapping
        -> hosted provider checkout
        -> signed provider webhook
        -> durable billing event
        -> locked tenant subscription update
        -> access and quota evaluation
```

Provider secrets and external plan/price IDs never cross into the frontend.

Application plans and provider billing objects remain separate. Creating an application plan does not automatically create a Stripe Product/Price or Razorpay Plan; checkout requires an explicit provider mapping.

## Cancellation/reconciliation boundary

Cancellation delegates to the provider linked to the subscription. Recovery logic can verify provider ownership and use durable webhook history when linkage is stale. Normal lifecycle writes remain webhook-authoritative.

If a provider already reports a terminal subscription but the terminal webhook was missed, the cancellation path may repair the local terminal state from verified provider data. This hardening was added after Stripe cancellations succeeded provider-side while local state remained `ACTIVE` because `customer.subscription.deleted` was missing from the webhook endpoint configuration.

## Current operational boundary

Billing & Payments is complete at application level.

Stripe works in deployed Test Mode through hosted Checkout, signed lifecycle synchronization and cancellation. Razorpay integration is implemented but real recurring Test Mode authorization remains blocked at the provider sandbox. Live-provider readiness is a separate deployment/operations track.
