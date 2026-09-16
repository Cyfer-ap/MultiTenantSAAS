# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder, Project Simulation / What-If Engine, Collaborative Whiteboard, Project Health / Risk Radar and **Forms -> Workflow Engine** are established through PR #152.

**Approval Workflows is active next.** Live whiteboard presence/cursors remain a later optional enhancement rather than a prerequisite for the next committed feature.

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
9. `guides/project_simulation.md`
10. `guides/collaborative_whiteboard.md`
11. `guides/project_risk_radar.md`
12. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, Saved Views validation SPI, Calendar deadline-source contracts, Task Relationships gateways, `TaskCreationPort`, `ProjectCreationPort`, workflow task events/`TaskAutomationMutationPort`, Forms project/task/workflow ports, Project Simulation source ports, Whiteboard project/task ports and Risk Radar task/dependency source ports.

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

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; task mutations cross through `TaskAutomationMutationPort`. Human approvals remain deliberately outside this domain until the next explicit approval slice.

## Resume here — Approval Workflows

Build the next feature as an explicit approval/human-decision domain that composes with the existing workflow runtime.

Initial goals:

1. bounded, reusable versioned approval definitions/stages
2. durable request and immutable decision provenance
3. tenant-scoped authorized target/workflow context
4. reviewer eligibility through a narrow authorization/membership contract, rechecked at decision time
5. narrow workflow wait/resume integration for approved/rejected outcomes
6. target mutations through existing domain-owned mutation ports after a decision
7. idempotent/concurrency-safe decisions and explicit stale/replay handling
8. internal reviewer inbox/history before external/client approvals
9. deterministic tests for authorization, tenant isolation, concurrency, bounds and workflow continuation
10. new persistence, if needed, starts at V53+

Guardrails:

- approval definitions never grant access to reviewers or target resources
- workflow administration permission does not imply approval or target-mutation permission
- do not duplicate the workflow runtime in the approval domain
- no arbitrary code/expression execution
- self-approval/separation-of-duties must be an explicit policy decision
- cancellation/expiry/reassignment/escalation semantics must be explicit before exposure
- client/guest approval belongs behind the later Client / Guest Portal boundary
- decision history remains auditable even after definition changes

After Approval Workflows continue with Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

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
