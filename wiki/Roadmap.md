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

Completed through PR #144.

Provides V50 workflow definitions/nodes/edges/executions, typed bounded DAGs, after-commit task event ingestion, task-owned automation mutations and the visual workflow canvas inside `/work-automation`.

### Project Simulation / What-If Engine

Completed through PR #145.

Provides:

- explicit `projectsimulation` domain
- narrow task-owned and Task Relationships-owned snapshot/read ports
- project-authorized baseline endpoint
- advisory simulation endpoint
- hypothetical due-date and assignee overrides
- hypothetical dependency add/remove operations
- downstream dependency exposure analysis
- baseline/new/resolved deadline-conflict comparison
- assignee workload-delta reporting
- bounded/cycle-safe scenario validation
- private `/projects/:projectId/simulation` What-If workspace
- no persistence or implicit apply/mutation path

The first version intentionally does not invent task durations or predicted project completion dates because the task model does not contain duration/effort estimates.

### Collaborative Whiteboard

Completed at the persisted application-workspace level through PRs #146 and #147.

Provides:

- explicit `whiteboards` domain
- V51 `whiteboards`, `whiteboard_nodes`, `whiteboard_edges`
- project-scoped, tenant-isolated boards
- bounded `STICKY`, `TEXT`, `SHAPE` nodes with stable keys and geometry
- bounded connectors with valid visual cycles
- board-level optimistic versioning and structured 409 stale-write conflicts
- project lifecycle through project-owned `ProjectAccessPort`
- sticky/text -> task conversion through task-owned `TaskCreationPort`
- persisted `linked_task_id` so conversion is durable and non-repeatable
- archived-project mutation protection
- private `/projects/:projectId/whiteboards` workspace
- board create/select/delete
- drag/resize, connectors, pan/zoom, multi-select and local undo/redo
- committed-edit autosave with explicit stale/conflict reload recovery
- backend-aligned project-lead/task-manage edit authority
- no WebSocket/STOMP persistence coupling

Live presence/cursors remain a later optional enhancement. The persisted document model is deliberately transport-independent and that enhancement does not block the next committed product feature.

## Current major product milestone

### Differentiated Work Platform Sequence

The older plan to move directly into bulk actions/CSV remains deliberately paused. The committed sequence continues in this order unless a production/security issue or explicit product decision reprioritizes it.

1. ✅ **Visual Workflow Builder** — completed through #144.
2. ✅ **Project Simulation / What-If Engine** — completed through #145.
3. ✅ **Collaborative Whiteboard** — persisted backend + project-facing visual workspace completed through #146/#147; live cursors/presence are optional later enhancement work.
4. 🚧 **Project Health / Risk Radar** — **ACTIVE NOW**. Explainable project-level risk signals from overdue work, blockers, stale work, dependency criticality and bounded workload pressure.
5. **Forms -> Workflow Engine** — structured internal/public intake that creates authorized work and can launch workflows.
6. **Approval Workflows** — reusable human review/approve/reject stages that compose with the workflow engine.
7. **Client / Guest Portal** — bounded external visibility, comments, review requests and approvals without broad tenant membership.
8. **Team Workload Engine** — capacity planning, overload detection and reassignment support without employee-surveillance scoring.
9. **Workspace Knowledge Graph** — permission-aware graph connecting projects, tasks, people, decisions, documents and dependencies.
10. **AI / Agent Teammates** — bounded agent work only after workflow, knowledge and authorization context are mature; human checkpoints remain mandatory for consequential actions.

`guides/Wild_Thoughts.md` is the detailed idea vault and records overlap with earlier experiments such as Scenario/Sandbox Mode, Deadline Reality Check, Risk Inbox, Change Blast-Radius Preview and Human Checkpoints for Automation/AI.

## Feature 4 — Project Health / Risk Radar — ACTIVE

Risk Radar should answer a concrete question:

> **Why does this project need attention right now?**

The first slice should remain advisory and explainable rather than producing an opaque project or employee score.

### Initial signal families

- overdue open work
- currently blocked work
- stale open work with no recent meaningful progress signal
- dependency criticality / downstream exposure
- unowned or ambiguous critical work where the domain already exposes explicit responsibility
- bounded workload pressure only from explicit assignment/capacity data

### Architecture direction

Use an explicit risk/health owning domain and narrow authorization-safe source/projection contracts for project, task, dependency and later workload state.

Do **not** inject legacy `ProjectService`, `ProjectTaskService` and relationship services into one risk god-service.

Risk output should contain:

- signal type
- severity/weight only where deterministic and documented
- concrete contributing task/dependency/entity references
- human-readable explanation
- enough structured data for UI drill-down

### Guardrails

- advisory/read-only first
- no automatic task/project mutations
- no employee productivity ranking
- no secret activity scoring
- no broad tenant retrieval followed by post-hoc access filtering
- bounded/cycle-safe graph analysis
- explanations must expose the facts that produced each signal
- authorization remains authoritative before data reaches risk calculation

## Collaborative Whiteboard — optional later live-collaboration slice

A later whiteboard enhancement may add replaceable real-time transport, reconnect/resynchronization, presence and cursors.

Guardrails remain:

- canvas state is not stored as arbitrary fields on project/task rows
- whiteboard objects do not become authoritative project work until explicit conversion succeeds
- WebSocket/session infrastructure does not own whiteboard domain invariants
- board access remains project-scoped and backend-authorized
- conversion to task/project state uses narrow domain-owned creation contracts
- optimistic conflicts never silently overwrite newer board state

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
- Project Simulation / What-If Engine
- Collaborative Whiteboard

Still valuable later:

- bulk actions and CSV import/export
- custom fields
- knowledge/documents beyond attachments
- user-facing analytics/reporting
- richer onboarding/workspace-switching/personalization

## Immediate sequence

1. **Project Health / Risk Radar**
2. Forms -> Workflow Engine
3. Approval Workflows
4. Client / Guest Portal
5. Team Workload Engine
6. Workspace Knowledge Graph
7. AI / Agent Teammates
8. resume remaining parked backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics

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

New user-facing features must remain permission-aware and tenant-safe. Search, Favorites/Recent resolution, My Work, Saved Views, Calendar, Task Planning, recurrence/templates, workflows, simulation, whiteboards, Risk Radar, analytics and future AI must filter through authoritative authorization boundaries rather than attempting to repair access after retrieval.

New functionality must stay inside explicit domain modules and cross boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views, Calendar, Task Relationships, `TaskCreationPort`, `ProjectCreationPort`, workflow events/`TaskAutomationMutationPort`, Project Simulation source ports and Whiteboard `ProjectAccessPort`/`TaskCreationPort` usage are current reference implementations.
