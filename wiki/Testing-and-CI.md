# Testing and CI

## Backend

Run:

```powershell
cd multitenant-saas
.\mvnw.cmd test
```

Coverage should include:

- authentication
- tenant isolation
- authorization
- sessions/passwords
- invitations
- projects/tasks
- subscription access
- quota enforcement
- concurrency locks
- PostgreSQL schema compatibility

## Frontend

Run:

```powershell
cd multitenant-saas-frontend
npm run lint
npm run test
npm run build
```

## PostgreSQL

When Docker is available, the Testcontainers PostgreSQL path should execute rather than being silently ignored.

## CI quality

Repository CI includes backend/frontend/database/security-quality gates.

Qodana is configured at repository level.

## Hygiene

Always run:

```powershell
git diff --check
git status --short
```

before finalizing a documentation or code checkpoint.
