# Roadmap

## Current

The platform foundation, PostgreSQL path, transaction/concurrency hardening, staging deployment, initial observability work, and GitHub Wiki publication flow are in place.

The PostgreSQL Primary Organizational Assignment issue has been fixed and regression-tested. The current phase is feature selection and incremental platform expansion rather than another broad hardening pass.

## Near term

1. keep documentation and Wiki source synchronized
2. select the next product/platform feature
3. harden tenant isolation, authorization, concurrency, and observability at each new feature boundary
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
