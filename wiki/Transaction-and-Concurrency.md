# Transaction and Concurrency Hardening

## Objective

Sequential correctness is insufficient for a production SaaS backend. Concurrent requests can produce:

- duplicate creation
- lost updates
- stale quota decisions
- inconsistent lifecycle transitions
- unstable error contracts

## Slice 40.1

Subscription state and quota-sensitive operations use database-backed pessimistic locking.

Plan/lifecycle transitions lock subscription state.

Subscription creation locks the stable tenant row before the existence-check/insert invariant.

## Why database locks

Database locks coordinate across multiple application instances.

A JVM-local mutex protects only one process and is therefore insufficient for horizontally scaled deployment.

## Remaining areas

- invitation acceptance/replacement
- login failure counters
- session version updates
- password/logout-all races
- duplicate-key normalization
- lock ordering
- deadlock review
- PostgreSQL concurrency integration tests

## Testing requirement

Concurrency tests should assert database-visible invariants, not only method invocation order.
