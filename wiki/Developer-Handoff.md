# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder, Project Simulation / What-If Engine, Collaborative Whiteboard, Project Health / Risk Radar, Forms -> Workflow Engine and **Approval Workflows** are established through PR #154.

**Client / Guest Portal is active next.** Live whiteboard presence/cursors remain a later optional enhancement rather than a prerequisite for the next committed feature.

## Read first

Inside the Wiki:

1. [[Architecture]]
2. [[Authorization]]
3. [[Security-and-Authentication]]
4. [[Roadmap]]
5. [[Testing-and-CI]]

Inside the repository:

1. `AGENTS.md`
2. `CHECKPOINT.md`
3. `HANDOFF.md`
4. `guides/current_architecture.md`
5. `guides/ENGINEERING_STANDARDS.md`
6. `guides/recurring_work_and_templates.md`
7. `guides/visual_workflow_builder.md`
8. `guides/forms_workflow_engine.md`
9. `guides/approval_workflows.md`
10. `guides/project_simulation.md`
11. `guides/collaborative_whiteboard.md`
12. `guides/project_risk_radar.md`
13. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, Saved Views validation SPI, Calendar deadline-source contracts, Task Relationships gateways, `TaskCreationPort`, `ProjectCreationPort`, workflow task events/`TaskAutomationMutationPort`, Forms project/task/workflow ports, Approval checkpoint/reviewer/resolution contracts, Project Simulation source ports, Whiteboard project/task ports and Risk Radar task/dependency source ports.

## Forms -> Workflow Engine checkpoint

Boundary:

```text
forms
    -> ProjectAccessPort
    -> TaskCreationPort
    -> WorkflowFormSubmissionPort
```

V52 owns project-scoped form definitions, fields and submissions. V1 supports bounded `TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `BOOLEAN` and `SELECT` fields; arbitrary executable logic and public/anonymous intake are excluded.

Accepted submissions create normal tasks through task-owned `TaskCreationPort`. An optional `TRIGGER_FORM_SUBMITTED` entry reuses the existing workflow runtime through `WorkflowFormSubmissionPort` and is dispatched after commit. Forms never writes task/project/workflow persistence directly.

The Work Automation workspace provides definition editing, lifecycle, internal submission and recent submission history.

Detailed rules live in `guides/forms_workflow_engine.md`.

## Existing differentiated checkpoints

### Project Health / Risk Radar

Risk Radar is read-only and explainable. V1 reports overdue, blocked, stale, unassigned HIGH/URGENT and dependency-bottleneck signals, with explicit response bounds and no opaque people/productivity score.

### Collaborative Whiteboard

V51 owns whiteboards/nodes/connectors. The `/projects/:projectId/whiteboards` workspace uses optimistic document versions and node -> task conversion through task-owned creation behavior. Live presence/cursors remain optional later work.

### Project Simulation

The private `/projects/:projectId/simulation` workspace supports hypothetical due-date, assignee and dependency changes. It remains advisory and has no apply action in v1.

### Visual Workflow Builder

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; task mutations cross through `TaskAutomationMutationPort`. V53 Approval Workflows now composes with this runtime through checkpoint/resolution contracts rather than moving human-decision state into the workflow domain.

## Approval Workflows checkpoint

V53 owns project-scoped approval definitions/stages/reviewer configuration plus durable request and reviewer snapshots. Reviewer eligibility is revalidated at decision time; stored configuration never grants authority.

Workflow `ACTION_REQUEST_APPROVAL` nodes pause the same execution in `WAITING_APPROVAL` and continue through explicit `APPROVED` / `REJECTED` branches after terminal decisions. Downstream mutations still re-enter owning-domain ports.

The internal approval workspace includes definition/stage configuration, reviewer inbox, approve/reject decisions and request history. External/guest approval remains outside this internal slice.

Detailed rules live in `guides/approval_workflows.md`.

## Resume here — Client / Guest Portal

Create a separate external-access domain; do not represent guests as tenant members.

Initial goals:

1. bounded, revocable tenant/project/resource-scoped external grants
2. hashed/rotatable invitation or access credentials and separate guest sessions
3. a deliberately small grant-scoped project/task/review read model
4. explicit comment/review mutation ports rather than direct repository writes
5. external approval only through an approval-owned contract that intersects grant scope with the approval request
6. deterministic expiry/revocation/session invalidation and immutable provenance
7. anti-enumeration, cross-tenant/resource-substitution protection, rate limits and abuse controls
8. deterministic tests for grant scope, revocation, stale sessions, public endpoint isolation and external decision boundaries
9. new persistence, if needed, starts at V54+

Guardrails:

- external grants are capabilities, not tenant RBAC or project membership
- a grant never exposes an entire tenant/project by default
- guest identity is never accepted by normal tenant-authenticated APIs
- every resource ID is re-bound to the active grant before read/mutation
- revoke/expire cuts off subsequent access even with a retained browser session
- external comments/reviews/approvals retain guest and grant provenance
- portal access alone never implies approval reviewer authority
- no public arbitrary search, tenant/user directory, admin, billing or workflow-management surface

After Client / Guest Portal continue with Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored workflow/whiteboard/risk/form/approval definitions never grant resource access
- Simulation, Whiteboard, Risk, Form and Approval data are authorized before exposure or action
- simulation and risk remain advisory until a separately authorized action exists
- generated work goes through domain-owned creation behavior
- workflow administration permission does not imply target-resource mutation permission
- approval decisions do not bypass target-domain mutation contracts
- graph/document/form/approval/batch work remains bounded
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.
