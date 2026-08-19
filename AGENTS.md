# AGENTS.md

## Purpose

This repository contains the MultiTenantSAAS platform. Treat this file as the persistent engineering contract for Codex/AI-assisted development.

The default operating model is autonomous, branch-first development with CI as the authoritative automated validation gate.

## Repository structure

- `multitenant-saas/` — Spring Boot backend
- `multitenant-saas-frontend/` — React + Vite + TypeScript frontend
- `compose.yaml` and related Compose files — local/container orchestration
- `.github/workflows/` — CI, security, container, Qodana, and formatting workflows
- Flyway migrations live under the backend resources and are append-only

## Core stack

### Backend

- Java 21 is the project target
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven wrapper
- Testcontainers where integration behavior requires a real PostgreSQL database

### Frontend

- React
- TypeScript
- Vite
- Vitest
- ESLint
- Prettier
- Material UI

## Development principles

1. Inspect narrowly before editing. Read only the files and symbols required for the current task.
2. Reuse existing architecture and conventions before introducing new abstractions.
3. Prefer one coherent feature or architectural objective per PR.
4. Avoid unrelated refactors during feature work.
5. Avoid unnecessary dependencies. Prefer the JDK, Spring Boot, and existing project libraries when sufficient.
6. Do not introduce distributed infrastructure such as Kafka, RabbitMQ, Redis, Kubernetes, or microservices unless the requirement clearly justifies it.
7. Keep changes production-oriented but proportionate to the current system.

## Git and PR workflow

Never commit directly to `main` unless explicitly instructed.

Default workflow:

`inspect -> design -> branch -> implement -> tests -> format -> push -> PR -> CI -> repair loop -> green -> merge -> verify`

Use descriptive branch names such as:

- `feature/...`
- `security/...`
- `fix/...`
- `ci/...`
- `observability/...`
- `docs/...`

When working autonomously:

1. Create a branch from current `main`.
2. Implement the complete slice, including tests.
3. Push changes to the branch.
4. Open a PR against `main`.
5. Inspect CI results yourself.
6. If CI fails, inspect the failing job/log, fix the branch, and repeat.
7. Merge only after required checks are green.
8. Verify the expected result exists on `main` after merge.

Do not ask the user to manually apply patches, stage files, commit, push, create PRs, or inspect routine CI failures when repository tools are available.

## Formatting

Formatting is mandatory.

### Backend

Use the repository Spotless configuration:

```bash
cd multitenant-saas
./mvnw spotless:apply
```

Spotless uses Google Java Format with the repository-defined configuration.

### Frontend

```bash
cd multitenant-saas-frontend
npm run format
```

The repository may also contain an automatic-formatting workflow for same-repository PR branches. Even when that workflow exists, produce reasonably formatted code before pushing and treat CI formatting checks as authoritative.

## Testing and validation

Tests are part of implementation, not optional follow-up work.

### Backend

Use the smallest relevant test target during development, then rely on CI for the full authoritative validation.

Typical commands:

```bash
cd multitenant-saas
./mvnw test
./mvnw verify
```

For targeted tests:

```bash
./mvnw "-Dtest=SpecificTestClass" test
```

### Frontend

Typical commands:

```bash
cd multitenant-saas-frontend
npm test
npm run test:coverage
npm run lint
npm run build
npm run format:check
```

For a single Vitest file:

```bash
npx vitest run path/to/File.test.tsx
```

### CI

Treat GitHub Actions as the authoritative automated gate. The repository CI is expected to cover, as applicable:

- Repository hygiene / whitespace
- Backend Maven verification
- PostgreSQL + Flyway integration validation
- Frontend formatting
- Frontend tests + coverage
- Frontend lint
- Frontend production build
- Security workflow
- Container CI
- Qodana/static analysis

Do not bypass a red required check merely to finish quickly.

If a failure is clearly infrastructure-related, such as GitHub 429/500/502/503 errors or action-download failures, retry rather than changing source code unnecessarily.

## Manual testing policy

Do not require the user to manually rerun the entire automated suite when CI already covers it.

Reserve manual smoke testing for behavior that automated tests may not fully model, especially:

- real browser authentication flows
- cookie/session behavior
- OTP delivery and verification
- real Brevo email delivery
- CORS and redirects
- deployment/environment configuration
- third-party integrations
- real object-storage upload/download flows

## Multi-tenant security invariants

This is a multi-tenant SaaS application. Every change must preserve strict tenant isolation.

Always consider:

- cross-tenant data leakage
- IDOR risks
- authorization boundaries
- role/permission enforcement
- tenant-scoped queries
- ownership checks
- authentication state
- secret handling
- auditability
- rate limiting where relevant
- concurrency/idempotency where relevant

Never weaken security boundaries merely to make a test pass.

Do not trust tenant IDs, project IDs, task IDs, attachment IDs, user IDs, or other resource IDs supplied by clients without validating that the authenticated principal is authorized for the corresponding tenant/resource.

## Authentication and identity

The repository has undergone substantial authentication hardening, including email-oriented workspace discovery/login, verified-email login, browser-session hardening, password recovery, rate limiting, and related security tests.

When changing authentication code:

- preserve generic failure behavior where required to avoid account enumeration
- preserve tenant/workspace authorization boundaries
- preserve rate limiting and abuse controls
- preserve secure cookie/session behavior
- avoid leaking internal authentication state
- add focused security regression tests

## Database and Flyway rules

Flyway owns schema evolution.

Rules:

1. Never edit an already-applied migration to change production behavior.
2. Create a new migration for every schema evolution.
3. Keep migration ordering valid relative to existing versions.
4. Update PostgreSQL/Flyway integration expectations when a new latest version is introduced.
5. Preserve production data semantics and backwards safety where practical.
6. Keep JPA mappings synchronized with the migrated schema.

## Object storage and attachments

The project uses/targets S3-compatible object storage, including Cloudflare R2.

Design principles:

- keep binary objects outside PostgreSQL
- store metadata and tenant/resource ownership in PostgreSQL
- use generated object keys rather than trusting raw filenames as storage identifiers
- enforce tenant/resource authorization before upload completion, listing, download, or deletion
- prefer an abstraction that can remain S3-compatible instead of coupling application logic unnecessarily to one vendor
- validate content metadata and size limits
- consider idempotency and incomplete-upload cleanup

## Email

Transactional email infrastructure uses Brevo.

Rules:

- never commit API keys or sender secrets
- use environment variables
- update example configuration when new settings are required
- keep provider-specific details behind an application abstraction where practical
- do not expose provider failures or internal details directly to end users
- test message construction and failure handling

## Configuration and secrets

Never commit real secrets.

Use environment variables and example configuration files.

When a feature introduces a new production variable:

1. wire it through typed/configured application settings where appropriate
2. document it in the relevant example environment/configuration file
3. provide safe defaults only when a safe default actually exists
4. never log secret values

## Error handling and observability

Preserve existing request correlation, structured logging, metrics, Actuator, and production-hardening conventions.

Prefer:

- actionable server logs
- generic/safe client-facing errors
- stable HTTP semantics
- metrics for operationally important flows
- correlation IDs for request tracing

Do not expose stack traces, internal exceptions, credentials, database details, or sensitive security state in production HTTP responses.

## API design

Follow existing controller/service/DTO conventions.

Prefer:

- explicit request/response DTOs
- bean validation at boundaries
- service-layer authorization and business invariants
- consistent status codes
- pagination for potentially unbounded collections
- idempotent semantics where retries are expected

Avoid returning persistence entities directly from public APIs unless the existing architecture explicitly does so and there is no data-leak risk.

## Frontend conventions

Reuse existing React, routing, React Query, MUI, form, validation, and API-client patterns.

When changing UI behavior:

- preserve accessibility
- add focused component/interaction tests
- handle loading, empty, error, and success states
- do not duplicate server authorization logic as a security mechanism; frontend checks are UX only
- preserve deep-link behavior where existing features support it

## Efficiency rules

Keep implementation and reasoning efficient.

- Do not repeatedly scan the whole repository.
- Search for the relevant symbol/file first.
- Batch independent repository reads where possible.
- Reuse information already gathered in the current task unless files changed.
- Do not re-explain settled architecture unless a new constraint changes the decision.
- Prefer targeted tests during the repair loop; let CI perform the complete suite.
- Fix the root cause of failures instead of adding broad workarounds.
- Do not perform speculative refactors while solving a focused task.

## Communication style for autonomous work

Keep updates concise and operational.

Useful checkpoints are:

- architecture confirmed
- branch created
- implementation complete
- PR opened
- CI failure + root cause
- repair pushed
- all checks green
- merged and verified

Do not narrate every trivial tool call.

## Destructive or high-impact changes

Stop and explain before proceeding when a proposed change:

- deletes or irreversibly transforms production data
- weakens authentication/authorization
- removes major functionality
- changes tenant-isolation semantics
- introduces meaningful new infrastructure cost
- requires a risky external-service migration
- rotates or invalidates production credentials

Otherwise, routine branch/implementation/test/PR/CI/merge work may proceed autonomously.
