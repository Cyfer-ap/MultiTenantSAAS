# Production Deployment

## Spring profiles

Production deployment uses:

```text
SPRING_PROFILES_ACTIVE=postgres,production
```

## Production environment template

Use:

```text
.env.production.example
```

as the variable inventory, but never commit real secrets.

Important variables include:

- PostgreSQL JDBC URL
- PostgreSQL username/password
- JWT secret
- CORS origins
- system-admin bootstrap controls
- password-reset settings
- public endpoint rate-limit settings
- invitation expiration
- frontend API base URL

## Production Spring hardening

The production profile:

- hides HTTP exception details
- disables Open Session in View
- validates Hibernate schema
- disables Flyway clean
- limits Actuator endpoints
- hides health components/details
- enables health probes
- avoids verbose Hibernate/security logs

## Deployment readiness boundary

Configuration support is not the same as full production maturity.

Still verify:

- concurrency semantics
- database backups/restores
- observability/alerts
- secrets management
- TLS/proxy behavior
- provider integrations
- failure recovery
- load/performance behavior
