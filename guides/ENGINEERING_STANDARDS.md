# Engineering Standards and Technical Health

Reviewed: 2026-09-14

This document is the canonical repository-side guide for architecture quality, technical debt, and engineering rules that apply to future development. It is intentionally separate from the product roadmap. `CHECKPOINT.md` owns current project status, `HANDOFF.md` owns resume instructions, and `guides/Wild_Thoughts.md` owns exploratory product ideas.

## Current technical assessment

MultiTenantSAAS is a strong production-oriented modular monolith with unusually broad application capabilities for its stage: tenant isolation, scoped authorization, bounded delegation, Explain Access, billing/provider lifecycle, outbound webhooks, OIDC SSO, API keys/usage limits, collaboration, notifications, attachments, audit trails, Flyway/PostgreSQL correctness, and a mature CI/security pipeline.

The codebase is healthy enough to continue feature development without a rewrite. The primary risk is now **architecture drift caused by continued growth**, not a weak technology foundation.

### Strengths

- backend-authoritative tenant isolation and authorization
- shared authorization evaluator for enforcement and Explain Access
- append-only Flyway schema evolution with PostgreSQL integration validation
- transaction/locking/concurrency handling in sensitive flows
- provider abstractions around billing instead of provider conditionals everywhere
- durable notifications/webhooks with retry/history behavior
- production hardening and safe secret/configuration handling
- frontend feature modules with React Query, API, hooks, types and focused tests
- CI gates for backend, PostgreSQL/Flyway, frontend, security, container/static analysis and repository hygiene
- strong auditability and explicit domain history in sensitive workflows

### Known technical debt and risk areas

#### 1. Backend package boundaries are inconsistent — HIGH priority

Newer domains such as billing are grouped by domain, while much of the older backend still uses broad global packages such as `service`, `controller`, `entity`, `repository` and `dto`.

This is manageable today but makes ownership and dependency direction harder to understand as the codebase grows.

**Direction:** all new functionality must use explicit domain modules. Existing domains should be migrated incrementally when touched for substantial work; do not perform a giant package rewrite.

#### 2. Application services are accumulating orchestration dependencies — HIGH priority

Some existing services coordinate persistence, audit, notifications, activity, authorization, quotas and integrations directly. This is functionally correct but increases change blast radius and produces constructor growth.

**Direction:** do not keep adding collaborators to already-large services. Prefer narrow domain/application services, explicit query/command contracts, or transactional domain/application events where decoupling is appropriate.

#### 3. Tenant isolation is partly convention-based — MEDIUM/HIGH priority

Tenant-owned repositories generally use explicit tenant-scoped methods, which is correct, but shared-schema tenancy does not physically prevent a future developer from using an unscoped repository lookup incorrectly.

**Direction:** tenant scope remains mandatory in repository/query APIs. Add architecture/static regression tests when practical so important tenant-boundary rules become machine-enforced rather than review-only conventions.

#### 4. API contracts are manually duplicated across clients — MEDIUM priority

Backend request/response DTOs and frontend TypeScript types/clients are maintained separately. This is acceptable with one web client but becomes increasingly risky with a future mobile client and external consumers.

**Direction:** keep public API DTOs stable and explicit. Move toward an OpenAPI-derived/shared TypeScript contract or generated client before mobile development becomes substantial.

#### 5. Frontend aggregation points will grow — MEDIUM priority

Feature folders are healthy, but central routing/navigation files already know about many product modules.

**Direction:** new feature modules should export route/navigation metadata or narrow integration points where that improves locality. Avoid one central application file becoming the dependency owner for every future module.

#### 6. Scale characteristics are not yet proven — MEDIUM priority

The architecture is horizontally scalable at the application tier, but real load envelopes, large-tenant query behavior, search performance, database contention and worker throughput have not yet been measured comprehensively.

**Direction:** optimize based on evidence. Do not introduce Redis, Kafka, microservices, search clusters or sharding pre-emptively. PostgreSQL-first designs remain preferred until measurement proves otherwise.

#### 7. Operational maturity is deliberately incomplete — MEDIUM priority

Backup/restore drills, broader failure recovery, load testing and production R2 verification remain deferred while the product layer is enriched.

This is a known roadmap decision, not hidden completeness debt. Do not claim full enterprise production readiness until those exercises exist.

#### 8. Documentation previously duplicated project state — FIXED BY POLICY

The repository accumulated multiple checkpoint/handoff/progress/manifests describing the same status, which made drift inevitable.

**Direction:** maintain one source of truth per fact. See the documentation ownership rules below.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

This is the default design constraint for all future feature work.

### Backend domain-module shape

Prefer structures such as:

```text
com.chacha.multitenantsaas.search/
    controller/
    dto/
    query/
    service/
```

or equivalent domain-oriented packaging.

A domain may expose a small public application/query contract. Internal repositories, persistence entities, provider details and implementation services should remain internal to the domain whenever practical.

### Frontend domain-module shape

Prefer:

```text
features/search/
    api/
    components/
    hooks/
    pages/
    types/
```

Feature-specific state, API functions and UI should remain local. Shared primitives belong in genuinely shared infrastructure only when multiple domains need them.

## Cross-domain dependency rules

1. **No new domain should reach directly into another domain's repository as its normal integration mechanism.** Use a narrow query/service contract.
2. **Do not pass JPA entities across domain boundaries unless they are part of an intentionally shared kernel.** Prefer IDs or explicit domain/application DTOs.
3. **Do not add unrelated responsibilities to an existing large service because it is convenient.** Create an orchestrator or a dedicated domain service.
4. **Side effects should be explicit.** Audit, notifications, webhooks and other integrations should use stable application services/events rather than hidden repository writes.
5. **Events are not mandatory for everything.** Use direct service calls when a synchronous invariant genuinely belongs in the same transaction; use events when the dependency is a secondary reaction and decoupling improves ownership.
6. **No circular domain dependencies.** If A needs B and B needs A, extract a small shared contract or redesign the orchestration boundary.
7. **Common/shared packages must remain small.** Do not create a dumping ground for domain logic.

## Dependency-budget rule

Constructor size is not a quality metric by itself, but it is a useful design alarm.

For new or materially modified application services:

- 1–5 collaborators: normal
- 6–7 collaborators: review whether responsibilities are still cohesive
- 8+ collaborators: decomposition or an explicit architectural justification is expected

Existing large services should not receive additional dependencies casually. Prefer extracting responsibilities during substantial changes to those areas.

## Change-blast-radius model

### Low risk

Examples:
- isolated UI component
- new read-only presentation endpoint
- provider implementation behind an existing stable interface
- local validation or formatting behavior

Expected validation: focused unit/component tests plus normal CI.

### Medium risk

Examples:
- new task/project field
- new notification or webhook event
- schema change
- public API contract change
- new search/index query

Expected validation: migration/schema tests, service/API tests, frontend contract tests, tenant-boundary checks, and compatibility review.

### High risk

Examples:
- authentication/session semantics
- tenant identity/isolation
- authorization scopes/evaluator behavior
- subscription lifecycle/access enforcement
- organization hierarchy semantics
- project/user state meanings used by authorization or billing

Expected validation: dedicated security/integration regressions, cross-tenant tests, failure-path tests, and manual smoke testing where external/browser behavior is involved.

## Security and tenancy rules

- backend authorization is always authoritative
- every tenant-owned query must establish tenant/resource ownership before returning data
- frontend permission checks are UX only
- new search, analytics, automation and future AI features must filter through authorization before exposing results; never fetch broadly and repair visibility afterward
- delegated authorization must preserve the existing non-escalation/source-revalidation invariants
- secrets and provider identifiers remain server-side unless explicitly safe for clients
- sensitive operations must remain auditable

## Database rules

- Flyway migrations are append-only
- never rewrite applied migrations
- favor PostgreSQL semantics over H2-specific behavior
- use database constraints/locking where correctness depends on concurrency
- paginate potentially unbounded reads
- add indexes based on real query patterns and measured plans, not guesswork
- avoid database-specific complexity unless it solves an observed problem

## API and client-contract rules

- expose request/response DTOs, not persistence entities
- validate input at boundaries
- preserve stable HTTP semantics and pagination conventions
- compatibility matters once web/mobile/external clients share APIs
- a backend contract change must update all affected TypeScript types/clients/tests in the same PR
- before substantial mobile work, adopt a generated/shared client contract strategy rather than triplicating models manually

## Frontend rules

- preserve feature locality
- use React Query for server state and cache invalidation patterns already established in the application
- keep route guards as UX; server enforcement remains final
- every asynchronous surface should handle loading, empty, error and success states
- preserve accessibility and deep-link behavior
- avoid copying business rules from the backend when the UI only needs capability/result metadata

## Testing and quality gates

Every feature PR must add the smallest tests that protect its important behavior and then pass applicable repository gates.

At minimum consider:

- happy path
- invalid input
- authorization failure
- cross-tenant access
- lifecycle/status edge cases
- concurrency/idempotency when relevant
- provider failure/retry when relevant
- frontend cache invalidation/navigation when relevant

Important architectural invariants should increasingly be protected by architecture/static tests so they cannot silently regress.

Coverage is a signal, not the target. Prefer meaningful boundary and regression tests over artificially maximizing line coverage.

## Documentation ownership

Maintain one authoritative source per category:

- `readme.md` — stable public platform overview; avoid volatile snapshot duplication
- `CHECKPOINT.md` — current repository/application status
- `HANDOFF.md` — current resume instructions and next action
- `AGENTS.md` — persistent autonomous-development engineering contract
- `guides/current_architecture.md` — canonical technical architecture
- `guides/ENGINEERING_STANDARDS.md` — canonical quality rules and technical-health debt register
- focused `guides/*.md` — detailed domain contracts/operations
- `guides/Wild_Thoughts.md` — idea vault, not committed roadmap
- `wiki/*.md` — canonical source for the published reader-facing Wiki; summarize rather than duplicate internal checkpoint text
- `wiki/Roadmap.md` — product direction and deferred milestones

Do not create another checkpoint, handoff, progress mirror, package-status manifest or milestone-summary file unless a genuinely different consumer requires it.

Historical decisions remain available through Git history and focused archival guides; they do not need active status mirrors.

## Refactor trigger

Do not stop feature development for a repository-wide rewrite. Refactor incrementally when one of these is true:

- a feature would add another dependency to an already-large service
- the same cross-domain lookup/validation is duplicated in multiple places
- a package has unclear ownership
- a change requires touching unrelated domains because boundaries are missing
- public contracts are duplicated across multiple clients
- a performance problem is measured rather than guessed

The preferred pattern is **feature work plus local architectural improvement**, not large speculative rewrites.

## Immediate application: Global Search

Global Search should be the first feature built under these rules.

It should become a dedicated `search` domain rather than another generic service. It should consume narrow permission-aware query contracts from projects/tasks/users, return only authorized tenant-scoped results, and expose a reusable search contract for web now and mobile/command-palette/AI consumers later.

That implementation will be the template for the architecture direction of subsequent product-enrichment modules.
