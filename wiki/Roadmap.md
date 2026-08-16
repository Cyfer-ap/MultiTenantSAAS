# Roadmap

## Current

**Step 40 — Transaction & Concurrency Hardening**

Completed first slice:

- subscription state serialization
- tenant-row locking for subscription creation

## Next

1. invitation race hardening
2. failed-login counter hardening
3. session-version/password/logout-all concurrency
4. integrity-race API normalization
5. PostgreSQL concurrency tests
6. deadlock/lock-order review

## After Step 40

Potential next production-focused areas:

- payment-provider integration
- provider webhook idempotency
- operational observability
- production database backup/restore
- background jobs
- notifications/email delivery
- additional performance/load verification

Large new feature areas should not be layered on unstable write semantics.
