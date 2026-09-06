# Architecture

Reviewed through PR #98 on 2026-09-06.

## Stack and planes

The platform uses Java 21/Spring Boot 4.0.7, React 19.2/TypeScript 6/Vite 8 and PostgreSQL 17/Flyway. Tenant and system-admin identities remain separate control planes.

## Enforcement pipeline

```text
authentication
    ↓
tenant boundary
    ↓
authorization and scope
    ↓
subscription lifecycle
    ↓
resource/API quota
    ↓
domain invariant
    ↓
transaction/database constraint
```

## Billing architecture

```text
application plan
    + server-side provider-plan/price mapping
        ↓
provider-neutral checkout service
        ↓
Stripe or Razorpay hosted checkout
        ↓
signed webhook
        ↓
durable billing event + replay protection
        ↓
locked tenant subscription synchronization
        ↓
access, entitlement and quota evaluation
```

The application plan catalogue and provider billing catalogues are separate. System-admin plan creation does not automatically create Stripe Products/Prices or Razorpay Plans. Provider checkout requires explicit server-side mappings.

Cancellation delegates to the linked provider, with ownership/history recovery when linkage is stale. Normal lifecycle state is webhook-authoritative. Verified provider state may repair a stale terminal local row when a cancellation succeeded but its terminal webhook was missed.

Stripe's final cancellation issue exposed that exact case: Stripe had cancelled subscriptions, but `customer.subscription.deleted` was not enabled on the endpoint. The endpoint configuration is now correct and PR #98 provides idempotent repair.

Checkout is permitted for a read-only workspace as a recovery action, but tenant authorization remains required.

## API keys and metering

Tenant API keys are revealed once, stored as hashes and accepted only under `/api/external/**`. Accepted calls record attributed `API_REQUESTS` usage and consume plan-period limits atomically.

## Operational boundary

**Billing & Payments is complete at application level.** Stripe is validated in deployed Test Mode for hosted Checkout, lifecycle webhooks and cancellation. Razorpay application integration is implemented but real recurring Test Mode authorization remains provider-sandbox blocked. Live-provider readiness remains separate operational work.
