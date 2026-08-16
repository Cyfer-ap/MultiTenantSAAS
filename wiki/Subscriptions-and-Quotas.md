# Subscriptions and Quotas

## Separation of concerns

Subscription code answers distinct questions:

1. which plan applies?
2. what lifecycle state is stored?
3. what access does that state currently permit?
4. what entitlements and limits apply?
5. what usage has already been consumed?

Do not collapse lifecycle status, evaluated access and quota usage into one boolean.

## Access reasons

Current access-reason semantics include:

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

## Workspace read-only enforcement

When evaluated subscription access does not permit ordinary tenant mutations, the central mutation guard returns a business restriction rather than pretending the user lacks authorization.

Typical contract:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

Read operations can remain available.

## Resource quotas

When the workspace is otherwise writable, resource capacity can independently reject growth.

Examples:

```text
USER_LIMIT_REACHED
PROJECT_LIMIT_REACHED
```

Quota-sensitive creation is transactionally serialized against tenant subscription state.

## Recovery

Selected cleanup/recovery operations may remain available so a tenant can reduce usage and return to compliance.

## Metrics

Blocked tenant growth is counted using:

```text
saas.subscription.restrictions
```

Tags are bounded categories such as restriction type, resource and access reason. Tenant IDs, user IDs and emails are intentionally not metric tags.

## Billing boundary

Provider checkout, invoices, webhook reconciliation and payment-provider idempotency remain separate future production work until explicitly implemented and tested.
