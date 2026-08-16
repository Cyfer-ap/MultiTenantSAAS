# Step 40 — Transaction and concurrency hardening

Step 40 addresses stale decisions, lost updates, duplicate work, and inconsistent error behavior under overlapping requests.

## Slice 40.1 — subscription state serialization

Existing quota-sensitive growth operations use a pessimistic write lock on tenant subscription state.

Plan changes and lifecycle updates now load mutable subscription state under the same write-lock discipline.

Subscription creation locks the tenant row before performing the one-subscription-per-tenant existence check/insert sequence.

## Locking rule

```text
existing subscription state/quota invariant
-> lock tenant-subscription row

subscription creation invariant
-> lock tenant row before check/insert
```

Use database-backed locks, not application-process mutexes.

## Remaining slices

1. invitation single-use/replacement races
2. tenant/system-admin failed-login counters
3. session-version/password/logout-all lost updates
4. duplicate/integrity race normalization
5. PostgreSQL concurrency integration tests
6. lock-order/deadlock review
