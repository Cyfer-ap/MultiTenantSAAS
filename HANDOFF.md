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
7. `guides/project_simulation.md`
8. `guides/collaborative_whiteboard.md`
9. `guides/project_risk_radar.md`
10. the focused guide for the domain being changed
11. `wiki/Roadmap.md` when planning product direction

## Current state

Major product milestones are complete through **Project Health / Risk Radar #148**:

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

Portable PostgreSQL Flyway migrations extend through **V51**. Never modify V51 or earlier after merge/application; later persistence starts at **V52+**.

Stripe remains the validated deployed Test Mode billing path. Razorpay integration/catalog provisioning remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Backend features should use explicit domain packages. Frontend features should preserve locality under `features/<domain>/...`.

Do not expand `ProjectTaskService` or `ProjectService` merely because a new feature reads or changes projects/tasks.

## Project Health / Risk Radar checkpoint

Backend boundary:

```text
projectrisk
    -> ProjectRiskTaskSource        -> task-owned adapter
    -> ProjectRiskDependencySource  -> Task Relationships-owned adapter
```

API:

```text
GET /api/tenants/{tenantId}/projects/{projectId}/risk
```

Project surface:

```text
/projects/{projectId}?view=risk
```

V1 is advisory/read-only and reports explainable overdue, blocked, stale, unassigned HIGH/URGENT and dependency-bottleneck signals. Calculations are bounded to 500 tasks, 1,000 dependency edges and 200 returned signals. Completed/cancelled work is excluded from open-risk calculations.

Do not add employee ranking, hidden productivity scores or assignment-count-as-capacity heuristics. Workload pressure requires an explicit future capacity/availability contract.

Detailed rules: `guides/project_risk_radar.md`.

## Existing differentiated boundaries to preserve

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

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; automated mutations cross through task-owned `TaskAutomationMutationPort`.

### Existing work generation

Recurring work and templates continue through task-owned `TaskCreationPort`; project-template creation crosses through project-owned `ProjectCreationPort`.

## Resume here — Forms -> Workflow Engine

The next committed feature is **Forms -> Workflow Engine**.

Build it as an explicit form/intake domain that composes with existing task/project/workflow contracts instead of adding form logic to legacy project/task services.

Recommended first slice:

1. define a versioned form definition with a bounded field schema
2. support safe field types first: text, textarea, number, date, boolean, select and optionally user/project references where authorization is explicit
3. keep form submission tenant/project scoped and backend-authorized
4. turn accepted submissions into authorized work through narrow creation/orchestration ports rather than direct repository writes
5. allow an optional workflow trigger/entry contract without duplicating the workflow runtime
6. persist submission provenance and validation/audit context
7. expose a simple internal form builder + submission surface before considering public/external intake
8. add deterministic validation, tenant isolation, authorization and abuse/bounds tests

Guardrails:

- no arbitrary executable code or expressions in fields
- no direct repository writes into task/project/workflow domains
- public intake, if added later, must have a separately designed authentication/rate-limit/abuse boundary
- form definitions never grant permission to the submitted target
- all created work re-enters normal quota, authorization, audit and lifecycle behavior
- keep field count/options/payload size bounded

After Forms -> Workflow Engine continue with Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

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
- stored workflow/whiteboard/risk/form definitions do not bypass resource authorization
- Simulation/Risk/Form data is authorized before exposure or action
- simulation and risk remain advisory unless a separately authorized human action exists
- generated tasks/projects go through domain-owned creation behavior
- graph, form and batch work remain bounded
- Explain Access and enforcement share authorization semantics
- delegated authority never exceeds current direct source authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred platform work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.
