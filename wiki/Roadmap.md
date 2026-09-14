# Roadmap

## Completed application milestones

### Billing/catalog lifecycle

Closed through PR #106.

### Tenant-configurable outbound webhooks

Closed through PR #112.

### Enterprise OIDC SSO / identity federation

Closed through PR #119.

### Authorization delegation and Explain Access

Closed through PR #125, with milestone documentation closed in PR #126.

### Engineering/documentation governance

Consolidated through PR #128.

### Global Search

Completed through PR #129.

### Command Palette

Completed through PR #130.

### Favorites + Recently Viewed

Completed through PR #131, with contextual favorite controls in PR #132.

### My Work

Completed through PR #133.

### Saved Views

Completed through PR #134 with V46 persistence.

### Capability-aware Dashboard Refresh

Completed through PR #136.

### Calendar / Deadline View

Completed through PR #137, with UI refinement in PR #138.

### Task Relationships + Task Planning

Backend foundation completed through PR #139 and user-facing Task Planning through PR #140.

Provides V47 bounded parent hierarchy, directed dependencies, project-scoped labels and the separate `/task-planning` workspace.

### Recurring Work + Task Templates — backend foundation

PR #141 establishes the first backend half of the broader recurring/templates milestone.

Provides:

- V48 `recurring_task_definitions`
- V48 `recurring_task_occurrences`
- V48 `project_task_templates`
- explicit `recurringwork` and `tasktemplates` backend domains
- task-owned `TaskCreationPort` reused by recurrence and templates
- timezone-aware `DAILY` / `WEEKLY` / `MONTHLY` recurrence
- pause/resume/edit/end/count semantics
- bounded scheduler discovery and catch-up
- pessimistic materialization locking and database occurrence idempotency
- project-scoped task-template catalog with normalized uniqueness and bounded size
- ordinary task lifecycle side effects preserved for generated/template tasks

This does **not** close the overall recurring/templates milestone. Tenant-scoped project templates and user-facing recurring/template management remain pending.

## Current major product milestone

### 1. Product Experience & Work Management Enrichment

#### Phase A — discoverability and personal productivity

- ✅ global authorized search
- ✅ command palette and quick actions
- ✅ favorites/recent items
- ✅ contextual favorite controls
- ✅ My Work attention queue
- ✅ saved views
- ✅ capability-aware dashboard refresh
- 🟡 onboarding/empty-state/quick-create polish — continue incrementally

#### Phase B — deeper work management

- ✅ calendar/deadline view
- ✅ subtasks
- ✅ directed task dependencies
- ✅ project-scoped labels/tags
- existing Kanban task board should be iterated rather than rebuilt
- 🟡 recurring work — backend foundation complete; frontend management pending
- 🟡 task templates — project-scoped backend foundation complete; frontend pending
- 🟡 project templates — tenant-scoped backend + frontend pending
- bulk actions and CSV import/export

The immediate work is to finish the recurring/templates milestone rather than start another domain.

Project-template creation must use a project-owned narrow creation contract preserving project quota, actor validation, owner membership, audit and lifecycle behavior. Do not inject the full legacy `ProjectService` into a template god-service.

Do not put recurrence generation inside Calendar or task-relationship graph services.

#### Phase C — tenant adaptability

- custom fields
- forms
- workflow/approval automation
- knowledge/documents beyond attachments
- user-facing analytics/reporting

#### Phase D — selected differentiators

Use `guides/Wild_Thoughts.md` as the idea vault. Candidate experiments include Permission Lens, Context Capsules, Change Blast-Radius Preview, Alternate-Reality Planning, Responsibility Gap Detector, Assumption Register, Contradiction Radar, Context Compression Checkpoints, Project Necromancer, Bureaucracy Detector, Reality-vs-Plan Drift and Human Checkpoints for automation/AI.

No experiment becomes a roadmap commitment merely because it is listed.

## Immediate sequence

1. **finish recurring work + project/task templates: project templates + frontend UX**
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics/reporting
5. selected differentiated experiments after the core product layer is strong

## Core product gaps to keep visible

Before calling the product layer mature, revisit:

- smooth post-login multi-workspace switching
- project-template and recurring/template UX completion
- custom fields/forms
- workflow/approval engine
- first-class knowledge/documents
- user-facing analytics/reporting
- import/export and bulk productivity
- richer onboarding, personalization, timezone and locale UX

## Deferred platform work

### 2. Production Operations & Disaster Recovery

Still important, but deliberately deferred from the immediate sequence while the product is enriched.

Target capabilities remain:

- PostgreSQL backup/export and retention strategy
- repeatable isolated restore drills
- health/readiness/metrics review
- actionable alerts
- incident/recovery runbooks
- secret/key rotation procedures

### 3. Broader failure-recovery/load and production R2 verification

Follow the operations/DR baseline later.

### 4. Optional enterprise expansion

- SAML where required
- SCIM/directory provisioning where required
- richer session/device/MFA/passkey controls when prioritized

### 5. Optional notification expansion

- digests
- live browser delivery
- web/mobile push where justified

## Independent provider/live-readiness track

- preserve Stripe as the working/validated Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, delegation non-escalation, verified provider reconciliation, webhook-authoritative normal billing state, immutable history, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.

New user-facing features must remain permission-aware and tenant-safe. Search, favorites/recent resolution, My Work, saved views, calendar projections, Task Planning, recurring work, templates, analytics, automation and future AI must filter through the same authorization boundary rather than attempting to repair access after data retrieval.

New functionality must stay inside explicit domain modules and cross domain boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views, Calendar, Task Relationships and the V48 work-generation boundary are backend reference implementations; Dashboard remains the frontend composition reference.
