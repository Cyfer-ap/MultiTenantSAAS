# Testing and CI

## Backend local verification

Use the Maven Wrapper:

```powershell
cd multitenant-saas

.\mvnw.cmd spotless:apply
.\mvnw.cmd spotless:check
.\mvnw.cmd test
.\mvnw.cmd verify
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
- collaboration and notification recipient/deep-link behavior
- subscription access and quota behavior
- concurrency
- PostgreSQL/Flyway compatibility
- operational filters/metrics where behavior matters

## Cross-module critical tenant journey

The post-PR #65 hardening checkpoint adds `CriticalTenantJourneyIntegrationTest`.

The purpose of this test is not to duplicate every focused assertion. It protects the seams between major user-facing subsystems in one realistic flow:

```text
tenant onboarding + admin login
        ↓
invite/accept member + member login
        ↓
create project + add member
        ↓
assign task
        ↓
create mentioned comment + reply
        ↓
verify notification types and exact deep links
        ↓
change task status + verify notification read state
        ↓
logout-all + verify session credentials are revoked
```

Run only this journey with:

```powershell
cd multitenant-saas
.\mvnw.cmd "-Dtest=CriticalTenantJourneyIntegrationTest" test
```

Focused tests remain the authoritative place for detailed edge cases and security boundaries.

## Browser E2E status

The frontend currently uses Vitest + Testing Library and already has focused project/Kanban/collaboration/deep-link regression coverage. A dedicated browser E2E runner such as Playwright is **not** introduced in this checkpoint so a new dependency, browser installation and CI job do not get mixed into a documentation/hardening change.

Browser E2E can be added later as a standalone testing-infrastructure slice when its maintenance cost is justified.

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

Qodana is useful static analysis; repository branch policy determines whether a specific Qodana result is merge-blocking.

## Repository hygiene

Before finalizing a change:

```powershell
git diff --check
git status --short
```
