# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-14
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`

This file is the **single repository-side source of truth for current project status**. Do not create additional progress/checkpoint mirrors.

## Current phase

**Product Experience & Work Management Enrichment**

The platform foundation is broad enough that current development should prioritize daily user value and product depth rather than additional infrastructure expansion.

Delivered product-enrichment milestones now include:

- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131
- contextual favorite controls — #132
- My Work attention queue — #133
- server-backed Saved Views — #134
- capability-aware Dashboard Refresh — #136
- authorization-safe Calendar / Deadline View — #137
- Calendar workspace UI refresh — #138
- task-relationship backend foundation — #139
- Task Planning UI — #140

The next implementation slice is **recurring work + project/task templates**.

## Completed application foundations

Major established capabilities include:

- billing/catalog lifecycle through PR #106
- tenant-configurable outbound webhooks through PR #112
- enterprise OIDC SSO / identity federation through PR #119
- authorization Explain Access/delegation through PR #125
- authorization milestone closure through PR #126
- product vision / Wild Thoughts audit through PR #127
- documentation/engineering-governance consolidation through PR #128
- discovery/navigation/personal-work productivity through PRs #129–#138
- task hierarchy/dependencies/labels through backend PR #139 and frontend PR #140
- projects/tasks/collaboration, R2/S3-compatible attachments, durable notifications/email, API keys, usage metering/quotas, tenant/platform audit, PostgreSQL/Flyway correctness, production hardening and CI/security gates

## Work-management checkpoint

### Search, navigation and personal productivity

- tenant-aware Global Search across accessible projects, tasks and people
- capability-aware Command Palette and shared workspace navigation contract
- Favorites + Recently Viewed
- My Work personal attention queue
- server-backed Saved Views
- capability-aware Dashboard composition
- Calendar / Deadline View over authorized task due dates

### Task relationships and labels

Backend PR #139 establishes an explicit `taskrelationships` domain rather than extending the already-large `ProjectTaskService`.

V1 semantics:

- one optional parent per task
- hierarchy remains same-tenant/same-project
- self-parenting and ancestry cycles rejected
- parent traversal capped at 64
- dependencies are directed `blocking task -> dependent task`
- self-dependencies, duplicates and directed cycles are prevented
- dependency validation capped at 1,000 project edges
- relationship reads capped at 200 items per direction with explicit truncation flags
- labels are project-scoped, normalized unique by project, capped at 200/project and 20/task
- relationship metadata does **not** automatically change task status
- existing task read/manage authorization remains authoritative

Backend shape:

```text
TaskRelationshipController
        ├── TaskRelationshipQueryService
        ├── TaskGraphService
        └── TaskLabelService
                  ↓
       TaskRelationshipTaskGateway
                  ↓
       existing project/task persistence

relationship changes
        ↓
TaskRelationshipChangeSink
        ↓
audit + task activity
```

Frontend PR #140 adds a separate `features/task-relationships` domain and `/task-planning` workspace rather than expanding `ProjectTasksSection`, Dashboard, Calendar or the collaboration drawer.

Task Planning:

- selects tasks through authorization-safe Global Search
- shows parent/direct subtasks, blockers/dependents and labels
- supports bounded same-project parent/blocker picking
- supports project-label create/edit/delete and task assignment
- derives mutation capability from the existing task-management permission plus established project-lead membership fallback
- remains read-only when the actor cannot manage the selected task context
- is registered in shared workspace navigation, so Command Palette and Dashboard quick actions inherit it without duplicate shell logic

Detailed contract: `guides/task_relationships.md`.

## Database checkpoint

Portable common Flyway migrations extend through **V47**.

Recent milestone migrations:

```text
V40 tenant identity-provider configuration
V41 OIDC authorization transactions + tenant federated identities
V42 tenant SSO policy
V43 OIDC browser session handoffs
V44 authorization delegation provenance + authorization.delegate
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
```

Dashboard #136 and Calendar #137 required no schema change. Never rewrite an applied migration.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning are implemented. Recurring Test Mode authorization remains provider-sandbox blocked. Keep the provider integration available; live readiness is a separate track.

## Product-core gaps

The largest remaining user-facing gaps are now:

- recurring work
- project/task templates
- bulk actions and CSV import/export
- custom fields/forms
- workflows/approvals
- first-class knowledge/documents
- user-facing analytics/reporting
- smoother workspace switching, onboarding and personalization

`guides/Wild_Thoughts.md` contains the broader audited idea vault and differentiated experiments.

## Engineering-health checkpoint

The codebase remains feasible for continued feature development without a rewrite, but future growth must actively control coupling.

Current priority debt:

1. inconsistent backend domain/package boundaries in older code
2. large legacy application services accumulating orchestration dependencies
3. tenant isolation relying partly on repository/query discipline in older areas
4. manually duplicated backend/frontend API contracts
5. growing frontend route/navigation aggregation points
6. scale/load characteristics not yet measured comprehensively
7. deferred operational maturity: backup/restore drills, broader recovery/load validation and production R2 verification

Canonical assessment and rules: `guides/ENGINEERING_STANDARDS.md`.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Recent product work demonstrates the intended pattern:

- Global Search: coordinator + contributor contracts + domain-owned query adapters
- Personal Workspace: personal state coordinator + project/task resolver adapters
- My Work: attention coordinator + narrow `MyWorkTaskSource`
- Saved Views: persistence/definition domain + narrow context-validator SPI
- Dashboard: frontend composition of existing authorized contracts
- Calendar: calendar coordinator + narrow deadline-source SPI + task-owned authorized adapter
- Task Relationships: query/graph/label services + narrow task gateway/change sink; separate frontend `features/task-relationships`

The next recurring-work/template slice must continue this pattern. Recurrence scheduling and template lifecycle must not be added to `TaskGraphService`, `TaskLabelService`, `ProjectTaskService`, Calendar, or the application shell merely because they touch tasks.

## Next product sequence

1. **recurring work + project/task templates**
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics + selected differentiated experiments
5. onboarding/workspace-switching/personalization polish as product flows deepen

For recurring work/templates, decide ownership and lifecycle semantics before schema work: schedule/timezone behavior, recurrence materialization/idempotency, template versioning/copy semantics, authorization, and bounded generation. Prefer a dedicated owning domain that calls narrow task/project contracts rather than another expansion of existing task services.

## Deferred platform work

Production Operations & Disaster Recovery remains important but intentionally deferred from the immediate product sequence:

- PostgreSQL backup/restore drills
- health/readiness/metrics review and alerting
- incident/recovery runbooks
- broader failure-recovery/load validation
- production R2 verification

Optional SAML/SCIM, MFA/passkeys/device management and richer notification channels remain demand-driven.

## Documentation ownership

- `CHECKPOINT.md` — current status
- `HANDOFF.md` — current resume/next action
- `AGENTS.md` — persistent engineering contract
- `guides/README.md` — documentation ownership/index
- `guides/current_architecture.md` — canonical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical-health assessment and quality rules
- `guides/task_relationships.md` — task hierarchy/dependency/label contract
- focused guides — domain-specific behavior
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.
