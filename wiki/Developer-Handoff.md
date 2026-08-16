# Developer Handoff

Use this page to resume development without depending on prior chat/session history.

## Repository

```text
Cyfer-ap/MultiTenantSAAS
default branch: main
```

Always sync `main` before starting a new branch.

## Read first

1. [[Home]]
2. [[Architecture]]
3. [[Security-and-Authentication]]
4. [[Authorization]]
5. [[PostgreSQL-and-Flyway]]
6. [[Transaction-and-Concurrency]]
7. [[Operations-and-Observability]]
8. [[Roadmap]]

Repository-side focused notes remain under `guides/`.

## Architecture boundaries to preserve

Do not collapse these concepts:

```text
authentication
tenant isolation
authorization
subscription lifecycle
quota enforcement
domain invariants
```

System-admin identity must remain separate from tenant-user identity.

## Completed hardening milestones

### Transaction/concurrency hardening

Database-backed work covers:

- subscription state serialization
- invitation single-use/replacement races
- failed-login/account-lock races
- session and credential concurrency
- account-state/session-version updates
- PostgreSQL concurrency integration tests
- integrity-race normalization and lock review

### Operational observability

Implemented capabilities include:

- `X-Request-ID` correlation
- correlated completion logs
- secured Actuator metrics
- subscription restriction counters
- authentication/login counters
- account-lock counters
- public-auth rate-limit rejection counters

## Migration invariant

```text
db/migration   -> historical H2 V1-V17
db/postgresql  -> PostgreSQL V17 current-schema baseline
db/common      -> future portable V18+
```

Never rewrite an already-applied migration.

## Safe change checklist

Before a PR:

```text
backend:
  .\mvnw.cmd spotless:apply
  .\mvnw.cmd spotless:check
  .\mvnw.cmd test
  .\mvnw.cmd -DskipTests verify

frontend when touched:
  npm run format
  npm run format:check
  npm run lint
  npm test
  npm run build

repository:
  git diff --check
  git status
```

For PowerShell targeted Maven tests with multiple classes, quote the entire property:

```powershell
.\mvnw.cmd "-Dtest=FirstTest,SecondTest" test
```

## Next priorities

The immediate product issue is the Primary Organizational Assignment path. After that, high-value production work includes database backup/restore and recovery documentation, external monitoring/alerting, load/failure testing, background jobs/notifications, and provider billing/webhook integration.
