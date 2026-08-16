# Current data model

The tenant is the principal workspace isolation boundary.

## Identity

Tenant application users and system administrators are separate identities.

## Tenant domain

Tenant-scoped data includes:

- users/memberships
- invitations
- authorization assignments
- organization structures
- projects
- project memberships
- tasks
- audit data
- subscription state

## Authorization domain

The authorization domain includes:

- permissions
- roles
- role permissions
- user/subject assignments
- policies/rules
- scope/organization-aware evaluation

## Subscription domain

The subscription domain separates plan configuration, lifecycle state, evaluated access, and quota use.

Evaluated access reasons include:

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

## Schema authority

Exact columns/constraints/indexes are defined by entities and Flyway migrations.
