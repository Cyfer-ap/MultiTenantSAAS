# Production Deployment

## Current staging topology

```text
Render Static Site (frontend)
        |
        v
Render Web Service (backend)
        |
        v
Neon PostgreSQL 17
```

The hosted path has been used to verify HTTPS, authentication, CORS, Flyway/Hibernate validation, persistence, and core tenant/system flows.

## Spring profiles

Hosted backend execution uses:

```text
SPRING_PROFILES_ACTIVE=postgres,production
```

## Environment template

Use:

```text
.env.production.example
```

as the variable inventory, but never commit real secrets.

Important values include:

- PostgreSQL JDBC URL
- PostgreSQL username/password
- JWT secret
- allowed CORS origins
- system-admin bootstrap controls
- password-reset exposure controls
- public endpoint rate limits
- invitation expiration
- frontend API base URL

Recommended hosted rules include:

```text
SYSTEM_ADMIN_BOOTSTRAP_ENABLED=false
PASSWORD_RESET_EXPOSE_TOKEN=false
CORS_ALLOWED_ORIGINS=<hosted frontend only>
FORWARD_HEADERS_STRATEGY=framework
```

## Production Spring hardening

The production profile:

- hides internal exception detail
- disables Open Session in View
- uses Hibernate schema validation
- disables Flyway clean
- restricts Actuator exposure
- suppresses health details/components
- avoids verbose security/database logging

## Frontend deployment

Render Static Site configuration:

```text
Root Directory: multitenant-saas-frontend
Build: npm ci && npm run build
Publish: dist
```

`VITE_API_BASE_URL` points to the hosted backend. SPA routes rewrite to `/index.html`.

## Remaining production-readiness work

Hosted staging is not the same as a production SLA. Remaining work includes:

- database backup/restore drills
- external monitoring and alerting
- secrets rotation/management
- load and failure-recovery verification
- payment-provider integration and webhook idempotency
- background jobs/notifications when required
