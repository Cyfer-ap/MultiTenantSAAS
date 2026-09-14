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

Completed through PR #143.

Provides V48 recurrence/task-template persistence, V49 tenant project templates, task-owned `TaskCreationPort`, project-owned `ProjectCreationPort`, timezone-aware bounded materialization and the `/work-automation` workspace.

### Visual Workflow Builder

Completed through PR #144 once its final green head is merged.

Provides:

- V50 `workflow_definitions`, `workflow_nodes`, `workflow_edges`, `workflow_executions`
- explicit `workflows` domain
- `DRAFT` / `ACTIVE` / `PAUSED` lifecycle
- bounded 2–50-node / 1–100-edge validated graphs
- exactly one trigger, DAG/reachability validation and typed configuration
- task-created / task-status-changed triggers
- task-priority / task-status equality conditions
- task-priority / task-status actions
- after-commit task-domain event ingestion
- idempotent execution recording and success/failure/skipped explanation
- task-owned `TaskAutomationMutationPort` with current-authority re-check
- non-recursive workflow-driven mutations in v1 to avoid accidental feedback loops
- dependency-free draggable visual canvas inside `/work-automation`
- active-workflow read-only behavior until paused
- tenant-wide recent workflow execution history

The backend graph is authoritative; the visual canvas does not bypass graph validation or task authorization.

## Current major product milestone

### Differentiated Work Platform Sequence

The older plan to move directly into bulk actions/CSV remains deliberately paused. The committed sequence continues in this order unless a production/security issue or explicit product decision reprioritizes it.

1. ✅ **Visual Workflow Builder** — completed through #144.
2. 🚧 **Project Simulation / What-If Engine** — active next. Private scenario changes for dates, owners and dependencies with downstream schedule/workload impact before applying anything.
3. **Collaborative Whiteboard** — visual planning canvas whose nodes/stickies can become real tasks/projects; later add live presence/cursors.
4. **Project Health / Risk Radar** — explainable risk signals from overdue work, blockers, stale work, dependency criticality and workload pressure.
5. **Forms -> Workflow Engine** — structured internal/public intake that creates authorized work and can launch workflows.
6. **Approval Workflows** — reusable human review/approve/reject stages that compose with the workflow engine.
7. **Client / Guest Portal** — bounded external visibility, comments, review requests and approvals without broad tenant membership.
8. **Team Workload Engine** — capacity planning, overload detection and reassignment support without employee-surveillance scoring.
9. **Workspace Knowledge Graph** — permission-aware graph connecting projects, tasks, people, decisions, documents and dependencies.
10. **AI / Agent Teammates** — bounded agent work only after workflow, knowledge and authorization context are mature; human checkpoints remain mandatory for consequential actions.

`guides/Wild_Thoughts.md` is the detailed idea vault and records overlap with earlier experiments such as Scenario/Sandbox Mode, Deadline Reality Check, Risk Inbox, Change Blast-Radius Preview and Human Checkpoints for Automation/AI.

## Feature 2 — Project Simulation / What-If Engine — ACTIVE NEXT

The simulation feature must be a private advisory layer over live work, not an alternate mutation path.

First milestone:

- explicit simulation/scenario owning domain
- authorization-safe snapshot/read contract for project, task and dependency state
- private scenario sessions owned by a user/tenant/project
- hypothetical overrides for dates and ownership without changing authoritative rows
- deterministic downstream schedule/dependency impact calculation
- workload impact summary using explicit work assignments/capacity inputs only
- explainable change/blast-radius output
- comparison between live baseline and scenario
- discard/reset behavior
- explicit human **Apply** action only after current authorization and domain invariants are re-checked
- audit trail for applied scenario changes

Guardrails:

- simulation never silently writes live state
- no production/project task mutation merely to calculate a scenario
- no opaque employee productivity score
- no broad service injection into a simulation god-service
- scenario reads and apply operations must use narrow domain-owned contracts
- stale scenarios must detect live-state drift before apply

## Product Experience & Work Management Enrichment — PARKED BEHIND THE COMMITTED SEQUENCE

Already completed foundations include:

- global authorized search
- command palette and quick actions
- favorites/recent items
- My Work
- saved views
- capability-aware dashboard
- calendar/deadline view
- subtasks/dependencies/labels
- Task Planning
- recurring work
- project/task templates
- Work Automation workspace
- Visual Workflow Builder

Still valuable later:

- bulk actions and CSV import/export
- custom fields
- knowledge/documents beyond attachments
- user-facing analytics/reporting
- richer onboarding/workspace-switching/personalization

## Immediate sequence

1. **Project Simulation / What-If Engine**
2. Collaborative Whiteboard
3. Project Health / Risk Radar
4. Forms -> Workflow Engine
5. Approval Workflows
6. Client / Guest Portal
7. Team Workload Engine
8. Workspace Knowledge Graph
9. AI / Agent Teammates
10. resume remaining parked backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics

For each slice, choose the owning domain and narrow cross-domain contracts before implementation. Do not implement new behavior by expanding legacy project/task god-services.

## Core product gaps to keep visible

These remain useful but sit behind the committed sequence unless they become prerequisites:

- smooth post-login multi-workspace switching
- bulk productivity and import/export
- custom fields
- first-class knowledge/documents
- user-facing analytics/reporting
- richer onboarding, personalization, timezone and locale UX

## Deferred platform work

### Production Operations & Disaster Recovery

Still important, but deliberately deferred from the immediate product sequence.

Target capabilities remain:

- PostgreSQL backup/export and retention strategy
- repeatable isolated restore drills
- health/readiness/metrics review
- actionable alerts
- incident/recovery runbooks
- secret/key rotation procedures
- broader failure-recovery/load validation
- production R2 verification

### Optional enterprise expansion

- SAML where required
- SCIM/directory provisioning where required
- richer session/device/MFA/passkey controls when prioritized

### Optional notification expansion

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

New user-facing features must remain permission-aware and tenant-safe. Search, Favorites/Recent resolution, My Work, Saved Views, Calendar, Task Planning, recurrence/templates, workflows, simulation, analytics and future AI must filter through authoritative authorization boundaries rather than attempting to repair access after retrieval.

New functionality must stay inside explicit domain modules and cross boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views, Calendar, Task Relationships, `TaskCreationPort`, `ProjectCreationPort`, task-domain workflow events and `TaskAutomationMutationPort` are current reference implementations.
