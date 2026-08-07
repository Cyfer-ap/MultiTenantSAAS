# Step 40 - Transaction and concurrency hardening

Step 40 hardens write paths that are correct under ordinary sequential use but can produce stale decisions, lost updates, duplicate work, or inconsistent error contracts when requests overlap.

## Scope

The work is intentionally split into small slices. Each slice should add or strengthen a database-backed concurrency boundary and tests before moving to the next area.

## Slice 40.1 - Subscription state serialization

### Existing protection

User/project quota growth already calls the tenant-subscription repository with a pessimistic write lock before evaluating capacity. Because the caller is transactional, that lock remains held until the surrounding user/project mutation commits or rolls back.

This prevents two quota-increasing requests for the same tenant from both observing the same remaining slot and consuming it concurrently.

### Gap fixed in this slice

Subscription administration previously loaded mutable subscription state without the same write lock.

That allowed concurrent plan/lifecycle requests to read the same old row and then overwrite one another. It also meant a quota-sensitive mutation could be evaluating a subscription row while a lifecycle/plan mutation changed it independently.

`changePlan` and `updateLifecycle` now load the tenant subscription through the existing pessimistic-write repository query before they validate and mutate it.

### Subscription creation race

Starting a subscription used an application-level sequence equivalent to:

```text
check whether subscription exists
-> create subscription
```

The database unique constraint on `tenant_subscriptions.tenant_id` is still the final invariant, but two concurrent start requests could previously both pass the existence check and leave one request failing only at insert time.

The start flow now locks the tenant row before the existence check. Starts for the same tenant therefore serialize while starts for different tenants can proceed independently.

## Locking rule introduced

Use the narrowest stable row that represents the invariant being protected:

- existing subscription state/quota decisions: lock the tenant-subscription row
- creation of the tenant's one-and-only subscription: lock the tenant row before checking/inserting

Do not introduce application-process mutexes for these invariants. Database locks work across multiple application instances; in-memory locks do not.

## Verification added

`TenantSubscriptionServiceLockingTest` verifies that:

1. subscription start obtains the locked tenant lookup before the duplicate check;
2. plan changes load subscription state through the write-lock query;
3. lifecycle changes load subscription state through the write-lock query.

The full backend suite and PostgreSQL/Testcontainers path remain the regression gate.

## Remaining Step 40 slices

After Slice 40.1 is green, continue with:

1. invitation single-use acceptance and replacement races;
2. tenant/system-admin failed-login counter races;
3. session-version/password/logout-all lost-update protection;
4. normalization of database duplicate/integrity races into stable API errors;
5. targeted PostgreSQL concurrency integration tests and a lock-order/deadlock review.

Avoid adding broad new product features until these state-transition paths have explicit concurrency semantics.
