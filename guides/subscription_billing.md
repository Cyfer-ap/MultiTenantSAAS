# Subscription and billing foundation

The current implementation provides the platform subscription foundation: plans, entitlements/limits, tenant subscriptions, access evaluation, quota enforcement, and central read-only behavior.

It is not yet a complete production payment-provider integration.

## Responsibilities

The subscription area is responsible for answering distinct questions:

1. Which plan is attached to this tenant?
2. What lifecycle state is the tenant subscription in?
3. What access does that state currently allow?
4. Which limits/entitlements apply?
5. Has the tenant consumed a resource limit?

## Access evaluation

The access evaluator returns a reason separate from the raw subscription lifecycle status.

Current `SubscriptionAccessReason` values are:

- `ACTIVE`
- `TRIAL_ACTIVE`
- `PAST_DUE_GRACE`
- `NO_SUBSCRIPTION`
- `PLAN_INACTIVE`
- `CANCELLED`
- `EXPIRED`
- `PERIOD_EXPIRED`
- `TRIAL_EXPIRED`

Code and tests should use these exact reason values where the API exposes an access reason.

## Central mutation guard

The subscription mutation interceptor protects ordinary tenant business mutations.

If the access evaluator reports that mutations are not allowed, the lifecycle guard throws a `SubscriptionRestrictionException` with:

```text
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

The exception also carries the evaluated access reason when available.

This central guard intentionally runs before many resource-specific service quota checks. Therefore a cancelled/expired/read-only workspace can return `WORKSPACE_READ_ONLY` instead of an older service-level `SERVICE_UNAVAILABLE` expectation.

## Quotas

When the workspace itself permits mutations, resource-specific quota guards can still block growth.

Examples currently covered by integration tests include user and project limits.

Typical quota restrictions include:

- `USER_LIMIT_REACHED`
- `PROJECT_LIMIT_REACHED`

Cleanup operations such as deactivating users or archiving projects can remain available so the tenant can reduce usage.

## Dashboard warning

Subscription warning UI is scoped to the dashboard rather than being a global application-shell banner.

The warning is intended for relevant trial/cancellation/grace boundaries that are approaching soon, while the full subscription state belongs on the subscription page.

## Provider boundary

The current code establishes provider-oriented fields/foundation but a full real payment provider flow is still future work.

Do not document checkout, webhooks, invoices, or provider reconciliation as production-complete until the corresponding integration is implemented and tested.

## Important tests

Subscription integration tests should cover at least these categories:

- access-state evaluation
- mutation blocking for read-only workspaces
- quota growth rejection
- cleanup/reduction still permitted where designed
- resource-specific restriction contracts
- PostgreSQL schema compatibility

## Next billing-related work

Payment-provider abstraction/integration should follow transaction/concurrency and PostgreSQL hardening rather than being layered onto unstable write semantics.
