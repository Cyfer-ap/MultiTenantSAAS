# Tenancy and Data Model

## Isolation strategy

The platform uses:

```text
shared application
shared database
shared schema
tenant foreign keys + tenant-scoped access patterns
```

The tenant ID is a security boundary, not merely a filter convenience.

## Tenant-owned domains

Tenant-scoped data includes:

- application users
- invitations
- organizational units and assignments
- authorization assignments/policies
- projects
- project memberships
- tasks
- tenant audit data
- tenant subscription state

## Platform-owned domains

System administrators and platform administration data belong to the system control plane rather than tenant membership.

## Query rule

Prefer tenant-qualified repository queries such as:

```text
tenant_id + entity_id
```

instead of an unscoped lookup followed by a tenant check after the entity is loaded.

## Referential integrity

Cross-tenant references must be rejected through the earliest reliable combination of:

- tenant-scoped query design
- service validation
- foreign keys / unique constraints
- transaction locking when invariants are concurrency sensitive

## Organization model

Authorization can be scoped through organizational units, subtrees, direct-report relationships, projects and self scope.

Organizational reporting relationships and the separate `primaryAssignment` invariant should be validated explicitly rather than inferred from one another.

## Source of truth

For exact columns, indexes and constraints, use:

1. Flyway migrations
2. JPA entities
3. repository/service tests
4. focused documentation
