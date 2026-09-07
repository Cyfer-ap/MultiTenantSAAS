# Current architecture

Reviewed through PR #106 on 2026-09-07.

## Platform

- Java 21 / Spring Boot 4.0.8 backend
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
- verified provider-aware cancellation, linkage recovery and stale-state repair
- managed Stripe/Razorpay provider catalogs with TEST/LIVE durable mappings
- safe terminal plan retirement and period/cycle-end provider cancellation
- immutable purchased-plan snapshots and durable tenant subscription history
- tenant/system-admin billing-history UX
- operations visibility and read-only reconciliation
- durable usage metering and plan limits
- tenant API keys and isolated external API authentication
- audit logs, observability and production profiles

## Billing/catalog data flow

```text
system-admin application plan
        -> provider-neutral catalog coordinator
        -> Stripe Product/Price and/or Razorpay Plan
        -> durable environment-specific provider mapping
        -> hosted provider checkout
        -> signed provider webhook
        -> durable billing event
        -> locked tenant subscription update
        -> immutable history snapshot
        -> access and quota evaluation
```

Provider secrets and external plan/price IDs never cross into the tenant-facing frontend.

### Stripe catalog semantics

New paid plans can create Product + recurring Price automatically. Price/currency/billing-interval edits create replacement Prices; historical Price mappings remain retained while prior Prices are not used for new sale. Retirement deactivates active Stripe catalog objects for future purchase and preserves historical provider references.

### Razorpay catalog semantics

New paid plans can create Razorpay Plans automatically. Provider-visible edits create replacement Plans and archive prior local mappings. Historical Plan IDs remain resolvable for existing subscriptions/history. Legacy configured Plan IDs remain compatibility/import paths.

## Plan lifecycle

- `ACTIVE`: purchasable/usable
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal/no new checkout; valid existing subscriptions continue through the purchased period and are scheduled not to renew

Retirement uses durable V35 operations so provider cleanup can retry without making the catalog plan purchasable again.

## History boundary

V34 purchased-plan snapshots detach purchased terms from mutable catalog rows. V36 tenant-subscription history records immutable material subscription states. Tenant-facing history omits internal provider subscription references; system-admin history can expose them operationally.

## Cancellation/reconciliation boundary

Cancellation delegates to the verified linked provider and uses period-end semantics. Recovery can verify ownership and durable provider history when linkage is stale. Normal lifecycle writes remain webhook-authoritative; verified provider state may repair stale terminal local state defensively.

## Current operational boundary

Billing/catalog lifecycle is complete at application level through PR #106.

Stripe remains the validated deployed Test Mode payment path. Razorpay integration/catalog provisioning are implemented, while real recurring Test Mode authorization remains provider-sandbox blocked. Live-provider readiness is a separate deployment/operations track.
