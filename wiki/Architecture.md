# Architecture

Reviewed through PR #106 on 2026-09-07.

## Stack and planes

The platform uses Java 21/Spring Boot 4.0.8, React 19.2/TypeScript 6/Vite 8 and PostgreSQL 17/Flyway. Tenant and system-admin identities remain separate control planes.

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

## Billing/catalog architecture

```text
system-admin application plan
        ↓
provider-neutral catalog coordination
        ↓
Stripe Product/Price and/or Razorpay Plan
        ↓
durable TEST/LIVE provider mapping
        ↓
hosted provider checkout
        ↓
signed provider webhook
        ↓
durable billing event + replay protection
        ↓
locked tenant subscription synchronization
        ↓
immutable subscription history
        ↓
access, entitlement and quota evaluation
```

Application plans and provider catalogs remain separate but are now synchronized for enabled managed providers.

### Stripe

Paid plan creation can provision a Product and recurring Price. Economic edits create replacement Prices instead of rewriting prior terms. Retiring a plan removes active catalog objects from new sale while preserving historical references.

### Razorpay

Paid plan creation can provision a Razorpay Plan. Provider-visible edits create replacement Plans and archive prior local mappings; historical Plan IDs remain resolvable.

Legacy provider mappings remain server-side compatibility/import paths.

## Plan lifecycle/history

- `ACTIVE`: purchasable and usable
- `INACTIVE`: administrative hard-disable
- `RETIRED`: terminal/no new checkout; existing valid subscriptions continue through their current paid period and provider renewals are scheduled to stop

Purchased terms are snapshotted independently of mutable catalog rows. V36 records immutable material subscription history for tenant and system-admin views.

## Cancellation/reconciliation boundary

Cancellation delegates to the verified linked provider with period-end/cycle-end semantics. Ownership/history recovery can repair stale linkage. Normal lifecycle state is webhook-authoritative; verified provider state may repair a stale terminal local row defensively.

Checkout remains permitted for a read-only workspace as a recovery action, but tenant authorization is still required.

## API keys and metering

Tenant API keys are revealed once, stored as hashes and accepted only under `/api/external/**`. Accepted calls record attributed `API_REQUESTS` usage and consume plan-period limits atomically.

## Operational boundary

**Billing/catalog lifecycle is complete at application level through PR #106.** Stripe remains the validated deployed Test Mode payment path. Razorpay integration/catalog provisioning are implemented but recurring Test Mode authorization remains provider-sandbox blocked. Live-provider readiness remains separate operational work.
