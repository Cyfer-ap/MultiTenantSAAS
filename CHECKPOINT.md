# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-16
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch target: `main` after PR #152 merge

This file is the **single repository-side source of truth for current project status**. Do not create duplicate progress/checkpoint mirrors.

## Current phase

**Differentiated Work Platform Sequence**

Delivered milestones now include:

- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- capability-aware Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- recurring work + task/project templates — #141/#143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145
- Collaborative Whiteboard foundation + persisted visual workspace — #146/#147
- Project Health / Risk Radar — #148
- **Forms -> Workflow Engine — #152**

**Approval Workflows is the active next feature.**

Live whiteboard presence/cursors remain an optional later enhancement and do not block the committed sequence. Bulk/CSV, broader custom fields, knowledge/documents and analytics remain parked unless explicitly reprioritized.

## Forms -> Workflow Engine checkpoint — #152

Forms is complete as a bounded, authenticated internal intake layer that composes with the existing task and workflow domains.

### Ownership and boundaries

The explicit `forms` backend domain owns definitions, fields, submissions and submission provenance. Cross-domain behavior stays narrow:

```text
forms
    -> ProjectAccessPort             -> project-owned access adapter
    -> TaskCreationPort              -> task-owned creation adapter
    -> WorkflowFormSubmissionPort    -> workflow-owned entry/runtime adapter
```

Forms does not inject `ProjectService`, `ProjectTaskService` or workflow repositories/services, and it never writes task/project/workflow persistence directly.

Frontend ownership lives under `features/forms` and is surfaced inside the existing Work Automation & Templates workspace rather than adding another top-level navigation aggregation point.

### V52 persistence and lifecycle

V52 creates:

- `form_definitions`
- `form_fields`
- `form_submissions`

Definitions are tenant/project scoped, versioned and bounded. Lifecycle is `DRAFT -> ACTIVE -> PAUSED -> ACTIVE`; active definitions must be paused before editing, and only active definitions accept submissions.

Supported v1 fields are `TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `BOOLEAN` and `SELECT`. Field count, options and payload sizes are explicitly bounded; no arbitrary expressions or executable user code are accepted.

### Task and workflow composition

Accepted submissions create tasks only through `TaskCreationPort`, preserving normal project/task authorization, actor, quota, audit and lifecycle behavior.

A form may optionally target an active workflow whose trigger is `TRIGGER_FORM_SUBMITTED`. Forms crosses into that runtime only through `WorkflowFormSubmissionPort`; dispatch occurs after the form/task transaction commits. The existing `TRIGGER_TASK_CREATED` behavior remains intact, so form-created tasks continue to participate in ordinary task workflows as well.

Workflow task mutations continue through task-owned `TaskAutomationMutationPort`; workflow administration never grants target-resource mutation authority.

### Internal UI and validation

The Forms workspace provides definition creation/editing, safe field configuration, task-field mapping, compatible workflow selection, activate/pause lifecycle, authenticated submission and recent submission history.

Regression coverage includes schema/type/bounds validation, unknown fields, project-scoped frontend API routes, active/paused behavior, task creation through the task-owned port, form-trigger workflow entry, V52 PostgreSQL assertions and exact decimal preservation from browser lexical input to backend `BigDecimal` normalization.

Public/anonymous intake, file fields, assignee/user-reference fields and arbitrary executable logic remain deliberately excluded from v1 and require separate security/abuse/authorization design before introduction.

Detailed contract: `guides/forms_workflow_engine.md`.

## Existing differentiated foundations

### Project Health / Risk Radar — #148

The explicit `projectrisk` domain provides bounded, explainable advisory signals for overdue, blocked, stale, unassigned HIGH/URGENT work and dependency bottlenecks through narrow task/dependency sources. It is read-only and does not perform employee scoring.

Project surface: `/projects/{projectId}?view=risk`.

Detailed contract: `guides/project_risk_radar.md`.

### Collaborative Whiteboard — #146/#147

V51 owns project-scoped whiteboards, nodes and connectors with optimistic versioning, bounded documents, archived-project mutation protection and sticky/text -> task conversion through task-owned `TaskCreationPort`.

Private workspace: `/projects/:projectId/whiteboards`.

Detailed contract: `guides/collaborative_whiteboard.md`.

### Project Simulation — #145

The explicit `projectsimulation` domain provides bounded, read-only due-date/assignee/dependency scenarios with downstream exposure, dependency-conflict comparison and workload deltas. It has no hidden apply path.

Private workspace: `/projects/:projectId/simulation`.

Detailed contract: `guides/project_simulation.md`.

### Visual Workflow Builder — #144

V50 owns workflow definitions/nodes/edges/executions, typed bounded graphs, after-commit task-domain events, idempotent execution history and task mutations through `TaskAutomationMutationPort`. Forms extends this runtime with the typed `TRIGGER_FORM_SUBMITTED` entry rather than duplicating workflow execution.

Detailed contract: `guides/visual_workflow_builder.md`.

### Recurring work + templates — #141/#143

V48/V49 recurrence and template generation continues through task-owned `TaskCreationPort` and project-owned `ProjectCreationPort`, preserving normal quota, authorization, audit and lifecycle rules.

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable PostgreSQL Flyway migrations extend through **V52**:

```text
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
V48 recurring task definitions/occurrences + project task templates
V49 tenant project templates + bounded starter-task snapshots
V50 visual workflow definitions/nodes/edges/executions
V51 project whiteboards/nodes/connectors
V52 project forms/fields/submissions
```

Project Simulation and Risk Radar add no migration. **V52 is immutable after merge/application.** New persistence starts at **V53+**.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include Search contributor contracts, Personal Workspace resolvers, My Work source ports, Saved Views validation SPI, Calendar deadline sources, Task Relationships gateways, `TaskCreationPort`, `ProjectCreationPort`, workflow events/`TaskAutomationMutationPort`, Project Simulation source ports, Whiteboard project/task ports, Risk Radar task/dependency sources and Forms project/task/workflow ports.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning remain implemented. Recurring Test Mode authorization remains provider-sandbox blocked; keep the integration available while treating live readiness separately.

## Engineering-health checkpoint

The codebase remains feasible for continued development without a rewrite if new work preserves explicit domains and narrow boundaries.

Priority debt remains:

1. older inconsistent backend package/domain boundaries
2. large legacy application services with accumulated orchestration dependencies
3. older tenant-isolation paths relying partly on repository/query discipline
4. duplicated backend/frontend API contracts
5. growing route/navigation aggregation points
6. comprehensive scale/load characterization still missing
7. deferred operational maturity: backup/restore drills, recovery/load validation and production R2 verification

Canonical assessment: `guides/ENGINEERING_STANDARDS.md`.

## Next committed product sequence

Continue in this order:

1. **Approval Workflows — ACTIVE NEXT**
2. Client / Guest Portal
3. Team Workload Engine
4. Workspace Knowledge Graph
5. AI / Agent Teammates
6. resume parked backlog such as bulk actions/CSV, broader custom fields, knowledge/documents and broader analytics unless priorities are explicitly changed

Approval Workflows must be an explicit human-decision domain that composes with the existing workflow runtime through narrow contracts/events. Approval configuration must never grant reviewer or target-resource authority, and any post-approval mutation must re-enter the owning domain's authorization/lifecycle rules.

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
- `guides/recurring_work_and_templates.md` — work-generation contracts
- `guides/visual_workflow_builder.md` — workflow graph/runtime/canvas contract
- `guides/forms_workflow_engine.md` — Forms intake/task/workflow contract
- `guides/project_simulation.md` — advisory What-If contract
- `guides/collaborative_whiteboard.md` — whiteboard persistence/workspace contract
- `guides/project_risk_radar.md` — Risk Radar source/bounds/signal/UI contract
- `guides/Wild_Thoughts.md` — product idea vault and committed sequence section
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.
