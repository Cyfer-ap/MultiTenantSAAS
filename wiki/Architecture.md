# Architecture

Reviewed through PR #90 on 2026-08-27.

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
    + server-side provider-plan mapping
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

Cancellation delegates to the linked provider and waits for a signed webhook. Reconciliation fetches a provider snapshot and reports differences without overwriting webhook-authoritative local state.

Checkout is permitted for a read-only workspace as a recovery action, but tenant authorization remains required.

## API keys and metering

Tenant API keys are revealed once, stored as hashes and accepted only under `/api/external/**`. Accepted calls record attributed `API_REQUESTS` usage and consume plan-period limits atomically.

## Operational boundary

Mocked provider contracts are covered in CI. Stripe's deployed Test Mode path works through hosted Checkout and signed webhook synchronization. Real Razorpay Test Mode authorization currently fails at the hosted provider page, so Razorpay's external lifecycle is not provider-validated.
