# Architecture

## Repository layers

The repository is organized around a Spring Boot backend, React frontend, documentation, and deployment configuration.

```text
MultiTenantSAAS/
├── guides/
├── multitenant-saas/
├── multitenant-saas-frontend/
├── compose.yaml
├── docker-compose.postgres.yml
├── .env.example
└── .env.production.example
```

## Backend boundaries

The backend separates:

- web/API transport
- authentication/security
- tenant isolation
- authorization
- subscription lifecycle enforcement
- quota enforcement
- domain services
- persistence
- auditing

A service should not infer a tenant from an unscoped entity lookup.

## Control planes

Tenant and system administration are separate identity/security surfaces.

This avoids implementing platform administration as a special tenant role.

## Persistence

The current production-readiness database is PostgreSQL 17.

H2 remains useful for the historical local/test path.

Flyway owns schema evolution.

## Design rule

Cross-cutting controls are intentionally compositional. A single generic `canDoEverything()` check is not an acceptable replacement for tenant isolation, authorization, subscription lifecycle, and quota enforcement.
