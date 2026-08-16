# Tenancy and Data Model

## Isolation model

The platform uses a shared application, shared database, and shared schema with tenant foreign keys and tenant-scoped access patterns.

The tenant ID is an explicit security boundary.

## Tenant-scoped areas

- users
- invitations
- organization and authorization data
- projects
- project memberships
- tasks
- audit data
- tenant subscription state

## Separate platform identity

System-administrator records belong to the platform control plane rather than tenant membership.

## Referential rules

Cross-tenant foreign/reference relationships must be rejected at service or database boundaries.

## Source of truth

For exact fields, foreign keys, constraints, indexes, and enum mappings, use:

1. entities
2. Flyway migrations
3. focused current guides
