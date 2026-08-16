# Transaction and Concurrency Hardening

## Objective

Sequential correctness is insufficient for a production SaaS backend. Concurrent requests can otherwise produce duplicate creation, lost updates, stale quota decisions, inconsistent lifecycle transitions, or unstable error contracts.

## Hardened areas

Database-backed transaction/concurrency work covers:

- subscription state and quota-sensitive writes
- invitation single-use/replacement races
- failed-login/account-lock updates
- session-version and credential-changing operations
- duplicate/integrity race normalization
- PostgreSQL-focused concurrency tests

Sensitive write paths prefer database constraints and locking over JVM-local mutexes.

## Why database coordination

Database locks coordinate across multiple application instances.

A JVM-local mutex protects only one process and therefore cannot enforce a distributed invariant after horizontal scaling.

## Error behavior

Temporary database conflicts use a dedicated retryable error contract where the operation is safe to retry.

Unsafe mutations are not transparently replayed merely because a transient conflict occurred.

## Design review rule

For quota-, subscription-, authentication-, invitation-, and organization-sensitive writes, review:

```text
transaction boundary
lock acquisition order
unique/foreign-key constraints
concurrent duplicate behavior
retry safety
tenant isolation
```

## Testing requirement

Concurrency tests should assert database-visible invariants, not merely method invocation order.
