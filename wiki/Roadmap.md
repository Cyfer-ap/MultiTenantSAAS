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

### Recurring Work + Project/Task Templates

Backend recurring/task-template foundation was established in PR #141; PR #143 closes the broader milestone.

Provides:

- V48 `recurring_task_definitions`
- V48 `recurring_task_occurrences`
- V48 `project_task_templates`
- V49 `project_templates`
- V49 `project_template_tasks`
- explicit `recurringwork`, `tasktemplates`, and `projecttemplates` backend domains
- task-owned `TaskCreationPort` reused by recurrence and both template flows
- project-owned `ProjectCreationPort` shared by ordinary and template-driven project creation
- project quota, active actor/tenant validation, initial `PROJECT_LEAD`, audit, and project lifecycle/webhook invariants preserved through the project-owned adapter
- timezone-aware `DAILY` / `WEEKLY` / `MONTHLY` recurrence
- pause/resume/edit/end/count semantics and occurrence history
- bounded scheduler discovery/catch-up with database idempotency and pessimistic materialization locking
- project-scoped task-template catalog with normalized uniqueness and bounded size
- tenant-scoped project-template catalog with maximum 50 ordered starter-task snapshots/template
- transactional project-template instantiation through narrow project/task creation contracts
- standalone `/work-automation` frontend workspace with recurring-rule, task-template and project-template management

The milestone is complete. Recurrence remains outside Calendar and template generation remains outside legacy project/task god-services.

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
- ✅ recurring work
- ✅ project-scoped task templates
- ✅ tenant-scoped project templates
- ✅ Work Automation & Templates workspace
- existing Kanban task board should be iterated rather than rebuilt
- **next: bulk actions and CSV import/export**

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

1. **bulk actions + CSV import/export**
2. custom fields/forms
3. workflows/approvals + knowledge/documents
4. user-facing analytics/reporting
5. selected differentiated experiments after the core product layer is strong

For each new slice, choose the owning domain and narrow cross-domain contracts before implementation. Do not add bulk/import/custom-field behavior by expanding existing god-services.

## Core product gaps to keep visible

Before calling the product layer mature, revisit:

- smooth post-login multi-workspace switching
- bulk productivity and import/export
- custom fields/forms
- workflow/approval engine
- first-class knowledge/documents
- user-facing analytics/reporting
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

New functionality must stay inside explicit domain modules and cross domain boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views, Calendar, Task Relationships, `TaskCreationPort`, and `ProjectCreationPort` are current reference implementations; Dashboard remains the frontend composition reference.
