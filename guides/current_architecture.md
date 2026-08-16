# Current architecture

The platform consists of a Java/Spring Boot backend and a React/Vite frontend.

## Platform boundaries

There are two control planes:

1. tenant plane
2. system-administration plane

System administrators are not tenant members.

## Backend

- Java 21
- Spring Boot 4.0.6
- Spring Security / JWT
- Spring Data JPA / Hibernate
- Flyway
- H2 historical development/test path
- PostgreSQL 17 production-readiness path
- Testcontainers
- Maven

## Frontend

- React 19
- TypeScript
- Vite
- Material UI
- React Router
- Redux Toolkit / RTK Query
- Vitest

## Enforcement pipeline

```text
authentication
-> tenant isolation
-> authorization
-> subscription lifecycle
-> quota
```

## Major implemented areas

- tenant lifecycle and users
- authentication/session/password flows
- system-admin control plane
- invitations
- authorization/organization foundation
- projects/memberships/tasks
- dashboards and audit logs
- subscriptions/entitlements/quotas
- central workspace read-only enforcement
- PostgreSQL schema validation
- production runtime hardening

## Current focus

Step 40 hardens transactional and concurrent write behavior.
