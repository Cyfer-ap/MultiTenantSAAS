# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-14
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main`

This file is the **single repository-side source of truth for current project status**. Do not create additional progress/checkpoint mirrors.

## Current phase

**Product Experience & Work Management Enrichment**

Delivered product-enrichment milestones include:

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
- recurring-task + project-scoped task-template backend foundation — #141

The **recurring work + project/task templates milestone is still open**. The next slice is tenant-scoped project templates plus feature-local frontend management for recurring work and templates.

## Established application foundations

Major capabilities now include:

- authentication, tenant isolation, invitations, password recovery and workspace discovery
- scoped authorization, bounded delegation and Explain Access
- users, organization hierarchy, projects, tasks and collaboration
- subtasks, directed task dependencies and project-scoped labels
- Global Search, Command Palette, Favorites/Recent, My Work, Saved Views, Dashboard and Calendar
- recurring-task backend scheduling/materialization and project-scoped task-template backend APIs
- R2/S3-compatible attachments
- durable notifications/email
- API keys, quotas and usage metering
- billing/subscriptions with Stripe + Razorpay provider abstractions
- tenant outbound webhooks
- enterprise OIDC SSO
- tenant/platform auditability
- PostgreSQL/Flyway correctness and CI/security/container gates

## Recurring work + task-template backend checkpoint

PR #141 introduces explicit `recurringwork` and `tasktemplates` backend domains plus a task-owned `tasks/creation` contract rather than expanding `ProjectTaskService`.

### Recurring tasks

V1 semantics:

- recurring **tasks only**
- `DAILY`, `WEEKLY`, `MONTHLY` cadence
- interval 1–52
- explicit IANA timezone per definition
- calendar/timezone-aware advancement across DST/month boundaries
- optional due offset, end instant and max occurrence count
- edits affect future generated work only
- pause/resume supported; resume skips occurrences missed while paused
- previous task completion does not gate the next occurrence
- definition discovery capped at 50/pass
- catch-up capped at 5 occurrences/definition/pass
- pessimistic per-definition materialization lock
- unique `(definition_id, scheduled_for)` occurrence key for retry/multi-instance idempotency
- generation failures pause the definition with a bounded diagnostic

### Project-scoped task templates

V1 semantics:

- project-scoped catalog
- normalized template name unique per project
- maximum 100 templates/project
- snapshots title, description, priority, optional assignee and optional due offset
- instantiation creates a normal task through `TaskCreationPort`
- template edits never mutate tasks already created from it
- no subtasks/dependencies/labels/custom fields/workflows inside task templates in this version

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable common Flyway migrations extend through **V48**.

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
V48 recurring task definitions/occurrences + project task templates
```

Dashboard #136 and Calendar #137 required no schema change. Never rewrite an applied migration.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning are implemented. Recurring Test Mode authorization remains provider-sandbox blocked. Keep the provider integration available; live readiness is a separate track.

## Product-core gaps

The largest remaining user-facing gaps are now:

- tenant-scoped project templates
- recurring-work/task-template/project-template frontend UX
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

Recent reference implementations:

- Global Search — coordinator + contributor contracts + domain-owned adapters
- Personal Workspace — personal state coordinator + project/task resolver adapters
- My Work — attention coordinator + narrow task source
- Saved Views — persistence/definition domain + validator SPI
- Calendar — calendar coordinator + deadline-source SPI
- Task Relationships — query/graph/label services + narrow task gateway/change sink
- Recurring Work / Task Templates — explicit owning domains + task-owned `TaskCreationPort`

Do not move project-template instantiation into `ProjectService`, recurrence into Calendar, or scheduling/template behavior into task-relationship graph services.

## Next product sequence

1. **finish recurring work + templates: tenant-scoped project templates + frontend UX**
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics + selected differentiated experiments
5. onboarding/workspace-switching/personalization polish

The project-template backend should cross into project creation through a **project-owned narrow creation contract** that preserves quota, owner-membership, audit and lifecycle invariants. Do not inject the full legacy `ProjectService` into a template god-service.

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
- `guides/recurring_work_and_templates.md` — recurring-task/task-template contract
- focused guides — domain-specific behavior
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.