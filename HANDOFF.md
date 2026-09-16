# MultiTenantSAAS — Development Handoff

Updated: 2026-09-16

This is the **single repository-side resume document**. Current status lives in `CHECKPOINT.md`; architecture/quality rules live in `AGENTS.md` and `guides/ENGINEERING_STANDARDS.md`.

## Read first

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `guides/current_architecture.md`
4. `guides/ENGINEERING_STANDARDS.md`
5. `guides/recurring_work_and_templates.md`
6. `guides/visual_workflow_builder.md`
7. `guides/forms_workflow_engine.md`
8. `guides/project_simulation.md`
9. `guides/collaborative_whiteboard.md`
10. `guides/project_risk_radar.md`
11. the focused guide for the domain being changed
12. `wiki/Roadmap.md` when planning product direction

## Current state

Major product milestones are complete through **Forms -> Workflow Engine #152**:

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

Portable PostgreSQL Flyway migrations extend through **V52**. Never modify V52 or earlier after merge/application; later persistence starts at **V53+**.

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

## Resume here — Approval Workflows

The next committed feature is **Approval Workflows**.

Build it as an explicit human-decision domain that composes with the existing workflow runtime. Do not bolt approval state into `ProjectTaskService` or turn `WorkflowService` into a cross-domain god-service.

Recommended first slice:

1. define bounded, reusable approval definitions/stages with explicit versioning and lifecycle
2. define durable approval request/decision records with immutable reviewer/outcome/timestamp provenance
3. keep approval targets tenant-scoped and tie each request to an explicit authorized work/workflow context
4. resolve eligible reviewers through a narrow authorization/membership contract; approval configuration itself must never grant authority
5. add a narrow workflow-owned waiting/resume contract so a workflow can create an approval checkpoint and continue on approved/rejected outcome without creating a second workflow runtime
6. ensure any mutation after approval still crosses the target domain's narrow mutation port and re-checks current authority
7. make decision handling concurrency-safe and idempotent so one logical stage cannot be approved/rejected twice by racing requests
8. expose an internal reviewer inbox/history surface before considering guest/client approvals
9. add deterministic tests for tenant isolation, reviewer eligibility, stale/replayed decisions, workflow resume semantics, bounds and auditability
10. if persistence is required, start at **V53+**; never edit V52 or earlier

Guardrails and design decisions that must remain explicit:

- no approval definition may grant project/task/workflow access
- workflow administration permission does not imply authority to approve or mutate the target resource
- reviewer eligibility is checked at decision time, not assumed forever from definition creation
- self-approval/separation-of-duties behavior must be an explicit policy choice, not an accidental side effect
- approval stages/reviewer sets/payloads remain bounded
- no arbitrary expressions or user-supplied executable code
- cancellation/expiry/reassignment/escalation semantics must be explicit before they are exposed
- external/client approvals belong behind the later Client / Guest Portal security boundary
- immutable decision history must remain auditable even if the underlying workflow definition changes later

After Approval Workflows continue with Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

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
