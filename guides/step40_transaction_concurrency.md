# Step 40 — Transaction and concurrency hardening

> Historical milestone reference. Step 40 is no longer the active project phase.

Step 40 addressed stale decisions, lost updates, duplicate work and inconsistent error behavior under overlapping requests.

## Implemented protections

Database-backed hardening now covers the major paths that motivated this milestone, including:

- subscription state serialization
- one-subscription-per-tenant creation races
- invitation single-use/replacement behavior
- failed-login/account-lock races
- refresh-token rotation races
- session/credential concurrency
- PostgreSQL pessimistic-write-lock behavior
- targeted PostgreSQL concurrency integration tests

## Locking rule

```text
existing subscription state/quota invariant
-> lock tenant-subscription row

subscription creation invariant
-> lock tenant row before check/insert
```

Use database-backed locks and constraints for cross-request invariants rather than application-process mutexes.

## Current regression coverage

`PostgreSqlConcurrencyIntegrationTest` exercises representative PostgreSQL concurrency behavior, including competing pessimistic locks, concurrent subscription creation, failed-login updates and one-time refresh-token rotation.

## Remaining concurrency work

Concurrency is now treated as an invariant to preserve for every new feature rather than as a standalone numbered phase.

Future work should be added only where code review or testing identifies a concrete race, integrity-normalization issue or deadlock/lock-order risk.
