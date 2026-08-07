# Current architecture

This guide describes the current architecture after the authorization, subscription, central read-only, and PostgreSQL-readiness work.

## Repository

```text
MultiTenantSAAS/
├── docker-compose.postgres.yml
├── guides/
├── multitenant-saas/             Spring Boot backend
└── multitenant-saas-frontend/    React/Vite frontend
```

## Backend stack

- Java 21
- Spring Boot 4.0.6
- Spring Security and JWT resource-server support
- Spring Data JPA / Hibernate
- Flyway
- H2 for the default local/test path
- PostgreSQL 17 production-readiness profile
- Testcontainers for PostgreSQL integration verification
- Maven

The backend normally runs on port `8081`.

## Frontend stack

- React 19
- TypeScript
- Vite
- Redux Toolkit / RTK Query
- React Router
- Material UI
- Vitest

## Platform boundaries

The application has two administration planes:

1. **Tenant plane** - tenant-scoped users, invitations, organization/authorization, projects/tasks, audit logs, and subscription visibility.
2. **System plane** - system administrator identity and system-level plan/subscription administration.

System administrators are not merely tenant users with a stronger tenant role. The identity and controller surfaces are deliberately separated.

## Security/enforcement layers

Business requests may be constrained by several independent layers:

1. authentication - who is the caller?
2. tenant isolation - which tenant may the caller act within?
3. authorization - does the caller have the required role/permission/policy outcome?
4. subscription lifecycle - may this workspace perform normal mutations?
5. quota enforcement - may this specific resource grow further?

These layers should not be collapsed into one generic permission check because they produce different contracts and recovery behavior.

## Central subscription read-only behavior

Normal tenant business mutations pass through the subscription mutation interceptor.

When the subscription evaluator says mutations are not allowed, ordinary tenant mutations are rejected with HTTP `409` and a restriction equivalent to:

```text
restriction = WORKSPACE_READ_ONLY
resource    = workspace
```

Read operations remain available. Explicit cleanup/recovery operations can opt out through the read-only-allowed annotation and may still be subject to their own service-level checks.

## Major implemented areas

- tenant onboarding/lifecycle
- authentication, refresh tokens, password flows, and account lockout
- tenant users and memberships
- system-admin authentication and administration
- invitations
- organization/authorization model
- projects, project membership, and tasks
- audit logs
- plans, entitlements, tenant subscriptions, access evaluation, and quotas
- central subscription read-only enforcement
- PostgreSQL profile, baseline, and Testcontainers schema verification

## Next architecture focus

Step 40 should harden transactional behavior and concurrency rather than adding another broad feature area immediately.
