# Developer Handoff

Use this page as the reader-facing Wiki pointer for resuming development. Repository-internal current status lives in `CHECKPOINT.md`; resume instructions live in `HANDOFF.md`.

## Current phase

**Differentiated Work Platform Sequence**

Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Relationships/Task Planning, recurring work, project/task templates, Work Automation and **Visual Workflow Builder** are established through PR #144 once its final green head is merged.

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
6. `guides/task_relationships.md`
7. `guides/recurring_work_and_templates.md`
8. `guides/visual_workflow_builder.md`
9. the focused guide for the domain being changed

## Engineering rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Reference implementations include Search contributor contracts, Personal Workspace resolver adapters, `MyWorkTaskSource`, the Saved Views validator SPI, Calendar's deadline-source contract, Task Relationships' task gateway/change sink, `TaskCreationPort`, `ProjectCreationPort`, task-domain workflow events and `TaskAutomationMutationPort`.

## Workflow checkpoint

V50 establishes:

- `workflow_definitions`
- `workflow_nodes`
- `workflow_edges`
- `workflow_executions`
- tenant-scoped draft/active/paused definitions
- bounded DAG/reachability validation
- typed trigger/condition/action operations
- after-commit task-event execution
- idempotency per tenant/workflow/event
- task-owned authorization re-check before automated mutation
- non-recursive automated task mutation in v1
- auditable success/failure/skipped execution explanations
- a dependency-free draggable canvas and recent execution history in `/work-automation`

The visual canvas edits a backend-authoritative graph; it never replaces backend validation. Active workflows must be paused before editing.

Detailed rules live in `guides/visual_workflow_builder.md`.

## Work-generation checkpoint

V48/V49 remain responsible for recurrence/task-template/project-template generation through `TaskCreationPort` and `ProjectCreationPort`. Calendar remains deadline projection and Task Relationships remains hierarchy/dependency/label ownership.

Detailed rules live in `guides/recurring_work_and_templates.md`.

## Resume here

Start from current `main`. Portable PostgreSQL migrations extend through **V50** after #144; new persistence begins at **V51+**.

The next committed product feature is **Project Simulation / What-If Engine**.

Before implementation:

1. choose an explicit scenario/simulation owning domain
2. read authorized live project/task/dependency state through narrow contracts
3. keep scenario overrides private and separate from authoritative rows
4. compute schedule/workload/blast-radius effects without live mutation
5. require an explicit human apply action that re-checks current authorization/invariants

After that continue with Collaborative Whiteboard, Project Health / Risk Radar, Forms -> Workflow Engine, Approval Workflows, Client / Guest Portal, Team Workload Engine, Workspace Knowledge Graph and AI / Agent Teammates.

## Preserve these system invariants

- tenant isolation precedes resource access
- backend authorization is authoritative
- stored favorites/recents/saved-view/workflow definitions never grant resource access
- Calendar, Task Planning and Work Automation data are authorized before exposure
- workflow administration permission does not imply permission to mutate a target task
- workflow/task generation retries remain idempotent
- graph traversal, reads and generation batches remain bounded
- automated workflow mutations do not recursively trigger workflows in v1
- project-template creation enforces ordinary quota/actor/lead/lifecycle rules
- Explain Access and enforcement use the same evaluator
- delegated authority never exceeds its current direct parent authority
- public APIs expose DTOs rather than persistence entities
- provider secrets remain server-side

## Deferred work

Production Operations & Disaster Recovery remains deliberately deferred behind the committed product sequence. It still includes restore drills, alert/runbook work, broader load/failure-recovery validation and production R2 verification.

See [[Roadmap]] for product direction and repository guide `guides/DEFERRED_PLATFORM_WORK.md` for deferred platform work.
