# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation, Visual Workflow Builder, Project Simulation / What-If Engine, Collaborative Whiteboard and **Project Health / Risk Radar** are established through PR #148.

**Forms -> Workflow Engine is active next.** Live whiteboard presence/cursors remain a later optional enhancement rather than a prerequisite for the next committed feature.

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
8. `guides/project_simulation.md`
9. `guides/collaborative_whiteboard.md`
10. `guides/project_risk_radar.md`
11. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, Saved Views validation SPI, Calendar deadline-source contracts, Task Relationships gateways, `TaskCreationPort`, `ProjectCreationPort`, workflow task events/`TaskAutomationMutationPort`, Project Simulation source ports, Whiteboard project/task ports and Risk Radar task/dependency source ports.

## Risk Radar checkpoint

Boundary:

```text
projectrisk
    -> ProjectRiskTaskSource
    -> ProjectRiskDependencySource
```

API:

```text
GET /api/tenants/{tenantId}/projects/{projectId}/risk
```

Project surface:

```text
/projects/{projectId}?view=risk
```

Risk Radar is read-only and explainable. V1 reports overdue, blocked, stale, unassigned HIGH/URGENT and dependency-bottleneck signals, with explicit response bounds and no opaque people/productivity score.

Detailed rules live in `guides/project_risk_radar.md`.

## Existing differentiated checkpoints

### Collaborative Whiteboard

V51 owns whiteboards/nodes/connectors. The `/projects/:projectId/whiteboards` workspace uses optimistic document versions and node -> task conversion through task-owned creation behavior. Live presence/cursors remain optional later work.

### Project Simulation

The private `/projects/:projectId/simulation` workspace supports hypothetical due-date, assignee and dependency changes. It remains advisory and has no apply action in v1.

### Visual Workflow Builder

V50 owns workflow definitions/nodes/edges/executions. Task lifecycle reaches workflows through task-domain events; task mutations cross through `TaskAutomationMutationPort`.

## Resume here — Forms -> Workflow Engine

Build the next feature as an explicit form/intake domain that composes with existing work and workflow boundaries.

Initial goals:

1. versioned bounded form definitions
2. safe internal field schema and server-side validation
3. tenant/project-scoped authorized submissions
4. submission provenance/audit context
5. authorized work creation through narrow domain-owned ports
6. optional workflow entry through a narrow contract into the existing workflow runtime
7. internal builder + submission UI before public/external intake
8. deterministic tests for tenant isolation, validation, authorization and bounds

Guardrails:

- no arbitrary code/expression execution
- no direct task/project/workflow repository writes from the form domain
- form definitions never grant target access
- created work follows ordinary quota, authorization, audit and lifecycle rules
- public intake requires a separately designed rate-limit/authentication/abuse boundary
- do not duplicate the workflow engine inside Forms

After Forms -> Workflow Engine continue with Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored workflow/whiteboard/risk/form definitions never grant resource access
- Simulation, Whiteboard, Risk and Form data are authorized before exposure or action
- simulation and risk remain advisory until a separately authorized action exists
- generated work goes through domain-owned creation behavior
- workflow administration permission does not imply target-resource mutation permission
- graph/document/form/batch work remains bounded
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.
