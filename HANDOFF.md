# MultiTenantSAAS — Development Handoff

Updated: 2026-09-23

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/recurring_work_and_templates.md`
6. `guides/visual_workflow_builder.md`
7. `guides/forms_workflow_engine.md`
8. `guides/approval_workflows.md`
9. `guides/client_guest_portal.md`
10. `guides/project_simulation.md`
11. `guides/collaborative_whiteboard.md`
12. `guides/project_risk_radar.md`
13. the focused guide for the domain being changed
14. `wiki/Roadmap.md` when planning product direction

## Current state

Major product milestones are complete through **Client / Guest Portal foundation #156**:

- billing/catalog — #106
- tenant outbound webhooks — #112
- enterprise OIDC SSO — #119
- authorization delegation + Explain Access — #125/#126
- product/engineering documentation governance — #127/#128
- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- recurring work + project/task templates — #141/#143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145
- Collaborative Whiteboard foundation + visual workspace — #146/#147
- Project Health / Risk Radar — #148
- Forms -> Workflow Engine — #152
- Approval Workflows — #154
- Client / Guest Portal foundation — #156

Portable PostgreSQL Flyway migrations extend through **V54**. Never modify V54 or earlier after merge/application; later persistence starts at **V55+**.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService`, `ProjectService` or `WorkflowService` merely because a new feature reads or changes projects/tasks/workflows.

## Forms -> Workflow Engine checkpoint — #152

Backend boundary:

```text
forms
    -> ProjectAccessPort
    -> TaskCreationPort
    -> WorkflowFormSubmissionPort
```

V52 owns:

- `form_definitions`
- `form_fields`
- `form_submissions`

The v1 form schema is bounded and non-executable (`TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `BOOLEAN`, `SELECT`). Definitions are tenant/project scoped and versioned, and lifecycle is `DRAFT -> ACTIVE -> PAUSED -> ACTIVE`.

Accepted submissions create tasks through task-owned `TaskCreationPort`. An optional form-specific workflow entry uses `TRIGGER_FORM_SUBMITTED` through workflow-owned `WorkflowFormSubmissionPort`, dispatched after commit. Generic `TRIGGER_TASK_CREATED` behavior remains unchanged.

The Work Automation workspace provides form definition editing, task mapping, optional compatible workflow selection, activation/pause, authenticated submission and history. Public/anonymous intake is deliberately excluded until a separate security/rate-limit/abuse boundary is designed.

Exact numeric text is preserved through the browser contract and normalized to backend `BigDecimal`; focused frontend/backend tests lock that behavior.

Detailed rules: `guides/forms_workflow_engine.md`.

## Existing differentiated boundaries to preserve

### Project Health / Risk Radar

```text
projectrisk
    -> ProjectRiskTaskSource
    -> ProjectRiskDependencySource
```

Risk Radar remains read-only/advisory and must not become an opaque employee/productivity score.

### Collaborative Whiteboard

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter
    -> TaskCreationPort  -> task-owned adapter
```

V51 remains the persisted whiteboard model. The `/projects/:projectId/whiteboards` workspace uses optimistic concurrency and explicit node -> task conversion; live presence/cursors remain optional later work.

### Project Simulation

```text
projectsimulation
    -> ProjectSimulationTaskSource -> task-owned adapter
    -> ProjectSimulationDependencySource -> Task Relationships-owned adapter
```

The `/projects/:projectId/simulation` workspace remains advisory with no hidden apply path.

### Visual Workflow Builder

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; automated mutations cross through task-owned `TaskAutomationMutationPort`. Forms extends the runtime with a typed form-submitted entry rather than duplicating it.

### Existing work generation

Recurring work and templates continue through task-owned `TaskCreationPort`; project-template creation crosses through project-owned `ProjectCreationPort`.

## Approval Workflows checkpoint — #154

Backend boundary:

```text
workflow runtime
    -> ApprovalCheckpointPort
    -> approvals domain

approvals
    -> ApprovalReviewerEligibilityPort
    -> project-owned reviewer eligibility adapter

approvals
    -> ApprovalResolvedEvent
    -> workflow-owned resume listener/runtime
    -> TaskAutomationMutationPort
```

V53 owns reusable project-scoped approval definitions/stages/reviewer configuration plus durable request, stage-snapshot and reviewer-snapshot provenance. Approval nodes use explicit `APPROVED` / `REJECTED` branches and pause the same workflow execution as `WAITING_APPROVAL`.

Reviewer authority is never derived from stored configuration alone: current project eligibility is revalidated at decision time. Self-approval is an explicit stage policy. Racing/replayed decisions are guarded by request locking/versioning. Any downstream task mutation re-enters `TaskAutomationMutationPort`.

The internal approval workspace provides definition/stage editing, reviewer selection, inbox decisions and immutable history. Guest/client approvals, expiry, escalation, reassignment, quorum and parallel stages remain deliberately outside v1.

Detailed rules: `guides/approval_workflows.md`.

## Client / Guest Portal foundation — #156

The grant/session and read-only portal foundation is complete.

Backend boundary:

```text
externalaccess
    -> ExternalProjectProjectionPort -> project-owned adapter
    -> ExternalTaskProjectionPort    -> task-owned adapter
```

V54 owns:

- `external_access_grants`
- `external_access_grant_capabilities`
- `external_guest_sessions`

Guests are not `AppUser` records, tenant members or RBAC subjects. The first capability set is `PROJECT_READ` plus optional `TASK_READ`. Raw invitation/session tokens are never stored; one-time invitation exchange and every subsequent guest request revalidate grant/session state. Revocation/expiry therefore invalidates retained sessions.

Public access is isolated under `/api/public/guest-portal/**`, uses `X-Guest-Session`, has its own rate-limit bucket and never accepts guest-provided tenant/project scope.

The frontend includes project-manager grant management and a standalone `/guest` experience using the public HTTP client only. Invitation secrets are scrubbed from the URL after exchange; guest session storage is separate from tenant authentication state.

Detailed rules: `guides/client_guest_portal.md`.

## Resume here — Client / Guest Portal mutation slices

Continue the **Client / Guest Portal** milestone from the merged #156 boundary. Do not redesign or bypass the foundation.

Next slice:

1. add a bounded external collaboration capability for guest comments/review responses; keep it typed rather than introducing a general permission DSL
2. enter task collaboration only through a new task-collaboration-owned narrow port/adapter
3. preserve immutable guest/grant provenance; do **not** fake a guest as an `AppUser`
4. do not reuse the ordinary JWT-based `TaskCollaborationService.createComment(..., Jwt)` path by manufacturing a JWT or system user
5. keep mentions, pinning, editing/deleting and attachments out of the first external-comment slice unless separately designed
6. validate grant -> tenant -> project -> task scope before the task-collaboration port is invoked
7. expose only comments/review data deliberately selected for the guest surface
8. after guest comments are green, add external approval through an approval-owned narrow contract
9. external approval authority must be the intersection of an active guest grant and the specific approval request's allowed external-review scope; portal access alone is never reviewer authority
10. retain immutable external decision provenance and concurrency/replay safety
11. add deterministic tests for cross-tenant/resource substitution, revoked/expired grants, stale sessions, duplicate/replayed mutations and owning-domain port enforcement
12. if new persistence is required, start at **V55+**; never edit V54 or earlier

Guardrails:

- no guest branches inside ordinary tenant-authenticated APIs
- no direct task-comment or approval repository writes from `externalaccess`
- no guest `AppUser`/membership/RBAC records
- no broad project mutation from a read grant
- no arbitrary guest search, directory, billing/admin or workflow-management access
- comments/approvals must remain revocable through grant invalidation
- feature-local frontend/backend ownership and narrow cross-domain contracts remain mandatory

After the full Client / Guest Portal milestone, continue with Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Validation before merge

For every slice require applicable green gates on the **final current head**:

- Repository Hygiene
- Backend build/test/verify
- PostgreSQL/Flyway
- Frontend format/tests/coverage/lint/build when frontend changes
- Security
- Container CI
- Qodana
- Wiki Sync when Wiki source changes

Do not merge around failed gates. If Auto Format creates a bot-authored head, verify it and follow it with a human commit so normal PR workflows run on the final feature state.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- automated workflows never grant authority
- stored workflow/whiteboard/risk/form/approval definitions do not bypass resource authorization
- Simulation/Risk/Form/Approval data is authorized before exposure or action
- simulation and risk remain advisory unless a separately authorized human action exists
- generated tasks/projects go through domain-owned creation behavior
- approval decisions do not bypass target-domain mutation contracts
- graph, form, approval and batch work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.
