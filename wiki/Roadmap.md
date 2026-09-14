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

### 1. Differentiated Work Platform Sequence

The previous plan to move directly into bulk actions/CSV is deliberately paused. The next product sequence is now committed in this order and should be completed before returning to the older backlog unless a production/security issue forces reprioritization.

1. **Visual Workflow Builder** — drag/connect `Trigger -> Condition -> Action` automations with execution history, guardrails and explainability.
2. **Project Simulation / What-If Engine** — private scenario changes for dates, owners and dependencies with downstream schedule/workload impact before applying anything.
3. **Collaborative Whiteboard** — visual planning canvas with nodes/stickies that can become real projects/tasks and later support live collaboration.
4. **Project Health / Risk Radar** — explainable project-risk signals for overdue work, blockers, dependency criticality, stale work and workload pressure.
5. **Forms -> Workflow Engine** — internal/public structured intake that creates authorized work and can trigger workflows.
6. **Approval Workflows** — reusable human review/approve/reject stages that compose with the workflow engine.
7. **Client / Guest Portal** — bounded external project visibility, comments, review requests and approvals without broad tenant membership.
8. **Team Workload Engine** — capacity planning, overload detection and reassignment support without employee surveillance scoring.
9. **Workspace Knowledge Graph** — permission-aware graph of projects, tasks, people, decisions, documents and dependencies.
10. **AI / Agent Teammates** — assign bounded work to agents only after workflow, knowledge and authorization context are mature; human checkpoints remain mandatory for consequential actions.

`guides/Wild_Thoughts.md` is the detailed idea vault and records overlap with older experiments such as Scenario/Sandbox Mode, Deadline Reality Check, Risk Inbox, Change Blast-Radius Preview and Human Checkpoints for Automation/AI.

### Feature 1 — Visual Workflow Builder — ACTIVE

Start with a product workflow engine, not a general BPMN clone.

First milestone:

- tenant-scoped workflow definitions
- explicit trigger, condition and action model
- draft/active/paused lifecycle
- safe bounded graph validation
- execution history with success/failure/skipped reasons
- idempotent event handling
- permission-aware action execution through narrow domain-owned ports
- audit/explanation for every execution
- frontend visual canvas for nodes/edges after the backend contract is stable
- runtime guardrails/circuit-breaker foundations rather than unrestricted arbitrary code

Initial trigger/action coverage should stay intentionally narrow and task/project-oriented so the engine remains understandable and testable.

### 2. Product Experience & Work Management Enrichment — PAUSED BEHIND THE SEQUENCE ABOVE

#### Phase A — discoverability and personal productivity

- ✅ global authorized search
- ✅ command palette and quick actions
- ✅ favorites/recent items
- ✅ contextual favorite controls
- ✅ My Work attention queue
- ✅ saved views
- ✅ capability-aware dashboard refresh
- 🟡 onboarding/empty-state/quick-create polish — continue incrementally only when it supports active work

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
- bulk actions and CSV import/export remain valuable but are no longer the immediate next slice

#### Phase C — tenant adaptability

- custom fields
- forms — now scheduled as committed feature #5
- workflow/approval automation — now split across committed features #1 and #6
- knowledge/documents beyond attachments
- user-facing analytics/reporting

#### Phase D — selected differentiators

Use `guides/Wild_Thoughts.md` as the idea vault. Candidate experiments remain available, but the ten-feature sequence above is now an explicit roadmap commitment rather than a loose experiment list.

## Immediate sequence

1. **Visual Workflow Builder** — active now
2. **Project Simulation / What-If Engine**
3. **Collaborative Whiteboard**
4. **Project Health / Risk Radar**
5. **Forms -> Workflow Engine**
6. **Approval Workflows**
7. **Client / Guest Portal**
8. **Team Workload Engine**
9. **Workspace Knowledge Graph**
10. **AI / Agent Teammates**
11. Resume remaining product backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics after this sequence unless priorities explicitly change.

For each slice, choose the owning domain and narrow cross-domain contracts before implementation. Do not implement workflow actions by injecting existing god-services into a central automation service.

## Core product gaps to keep visible

These remain useful but sit behind the committed sequence unless they are prerequisites for one of its features:

- smooth post-login multi-workspace switching
- bulk productivity and import/export
- custom fields
- first-class knowledge/documents
- user-facing analytics/reporting
- richer onboarding, personalization, timezone and locale UX

## Deferred platform work

### 3. Production Operations & Disaster Recovery

Still important, but deliberately deferred from the immediate sequence while the product is enriched.

Target capabilities remain:

- PostgreSQL backup/export and retention strategy
- repeatable isolated restore drills
- health/readiness/metrics review
- actionable alerts
- incident/recovery runbooks
- secret/key rotation procedures

### 4. Broader failure-recovery/load and production R2 verification

Follow the operations/DR baseline later.

### 5. Optional enterprise expansion

- SAML where required
- SCIM/directory provisioning where required
- richer session/device/MFA/passkey controls when prioritized

### 6. Optional notification expansion

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
