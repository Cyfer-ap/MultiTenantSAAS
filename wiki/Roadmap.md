# Roadmap

## Current

The platform foundation, PostgreSQL path, transaction/concurrency hardening, staging deployment, and initial observability work are in place.

The immediate product-level issue is:

```text
Primary Organizational Assignment
```

Normal organization assignment and reporting-option population work; the primary-assignment path requires focused PostgreSQL/service-path verification.

## Near term

1. fix and regression-test Primary Organizational Assignment
2. verify adjacent organization/reporting edge cases
3. keep documentation and Wiki source synchronized
4. add database backup/restore procedure and recovery drill
5. add external monitoring/alerting
6. perform focused load/failure-recovery verification

## Deferred until selected

- real payment-provider checkout
- provider webhook reconciliation/idempotency
- production notification/email delivery
- distributed rate limiting for horizontal scale
- background jobs that are not yet required by a concrete feature
- production-grade hosting/SLA work

New work should continue to preserve tenant isolation, stable API contracts, Flyway migration invariants, and transaction safety.
