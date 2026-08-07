# Current data model

This is a conceptual map of the current domain. The entities and Flyway migrations remain authoritative for exact columns, constraints, and indexes.

## Tenant core

The tenant is the principal isolation boundary for workspace data.

Tenant-scoped areas include users/memberships, invitations, authorization assignments, projects/tasks, audit data, and subscription state.

Tenant identifiers must be carried through repository/service/controller flows rather than inferred from unscoped records.

## Identity

The codebase distinguishes tenant-facing application users from system-administrator identity.

This prevents system administration from being implemented as a special tenant membership and keeps the control plane separate from normal tenant authorization.

## Authorization domain

The authorization model includes concepts for:

- permission catalog entries
- tenant-defined authorization roles
- role-to-permission assignments
- user/subject role assignments
- authorization policies/rules
- organization-aware or tenant-aware decisions where applicable

Exact tables and relationships should be read from the current migrations and entities.

## Collaboration/work domain

The application currently models:

- projects
- project memberships
- tasks

These records are tenant-scoped and must remain protected by both tenant isolation and authorization.

## Subscription domain

The subscription model separates:

- subscription plans
- plan entitlements/limits
- tenant subscription lifecycle
- evaluated access state
- quota usage/enforcement

The current subscription lifecycle includes states such as `TRIALING`, `ACTIVE`, `PAST_DUE`, `CANCELLED`, and `EXPIRED` where represented by the current enum/model.

The access evaluator uses its own reason model. Current `SubscriptionAccessReason` values are:

- `ACTIVE`
- `TRIAL_ACTIVE`
- `PAST_DUE_GRACE`
- `NO_SUBSCRIPTION`
- `PLAN_INACTIVE`
- `CANCELLED`
- `EXPIRED`
- `PERIOD_EXPIRED`
- `TRIAL_EXPIRED`

Do not substitute lifecycle status names for access-reason names in tests or API documentation.

## Audit domain

Audit logging records tenant-sensitive actions and supports filtering/sorting through the current audit API.

## Schema authority

For exact schema details, consult:

1. `db/migration` for the historical H2 migration chain
2. `db/postgresql/V17__current_schema_baseline.sql` for the PostgreSQL baseline
3. `db/common` for all future shared migrations beginning with V18
