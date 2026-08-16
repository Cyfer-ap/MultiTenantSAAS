# Subscriptions and Quotas

## Model responsibilities

Subscription code answers five independent questions:

1. which plan applies?
2. what is the subscription lifecycle state?
3. what access does that state currently permit?
4. what entitlements/limits apply?
5. has resource capacity been consumed?

## Access reasons

```text
ACTIVE
TRIAL_ACTIVE
PAST_DUE_GRACE
NO_SUBSCRIPTION
PLAN_INACTIVE
CANCELLED
EXPIRED
PERIOD_EXPIRED
TRIAL_EXPIRED
```

## Workspace read-only mode

The central mutation interceptor blocks ordinary business mutations when access evaluation says mutations are unavailable.

Standard contract:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

## Quotas

When the workspace is writable, resource-specific capacity checks can still fail.

Examples:

```text
USER_LIMIT_REACHED
PROJECT_LIMIT_REACHED
```

## Recovery

Selected cleanup operations can remain enabled so customers can reduce usage.

## Billing boundary

Provider checkout, webhooks, invoices, and reconciliation are future/partial work until explicitly implemented and tested.
