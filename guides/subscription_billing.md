# Subscription and billing foundation

The implementation currently covers plan configuration, entitlements, subscription lifecycle, access evaluation, quotas, and read-only enforcement.

It is not yet a complete production payment-provider integration.

## Access evaluation

Access reasons:

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

## Central mutation guard

Ordinary tenant business writes are centrally rejected when the workspace is read-only:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

## Quotas

When lifecycle access allows mutation, resource quota guards still protect capacity.

Examples:

```text
USER_LIMIT_REACHED
PROJECT_LIMIT_REACHED
```

Cleanup/reduction operations can remain usable where explicitly designed.

## Provider boundary

Checkout, provider webhooks, invoices, reconciliation, and provider-specific production behavior should not be documented as complete until implemented and tested.
