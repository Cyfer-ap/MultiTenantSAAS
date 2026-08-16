# Testing and CI

## Backend local verification

Use the Maven Wrapper:

```powershell
cd multitenant-saas

.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd -DskipTests verify
```

For multiple targeted tests in PowerShell, quote the property containing commas:

```powershell
.\mvnw.cmd "-Dtest=FirstTest,SecondTest" test
```

## Backend coverage priorities

Tests should cover:

- tenant isolation
- authentication/session behavior
- authorization scopes
- account-state invariants
- invitation lifecycle
- projects/tasks
- subscription access and quota behavior
- concurrency
- PostgreSQL/Flyway compatibility
- operational filters/metrics where behavior matters

## Frontend verification

```powershell
cd multitenant-saas-frontend

npm run format
npm run format:check
npm run lint
npm test
npm run build
```

## PostgreSQL

PostgreSQL integration tests use Testcontainers. When Docker is available, the database path should execute rather than silently relying on H2 semantics.

## GitHub quality gates

Repository CI/security coverage includes:

```text
Repository Hygiene
Backend
PostgreSQL & Flyway
Frontend
Trivy filesystem/source scan
Qodana

Container CI:
Compose Validation
Backend Container
Frontend Container
```

Qodana is useful as static analysis and can be treated as advisory unless repository policy deliberately makes it required.

## Repository hygiene

Before finalizing a change:

```powershell
git diff --check
git status --short
```
