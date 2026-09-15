# Visual Workflow Builder

The Visual Workflow Builder is the tenant-scoped automation domain introduced in PR #144. It lets authorized tenant users compose a bounded task workflow as a validated graph, activate it, observe executions, and pause it before editing.

The frontend canvas is an editor for the backend contract. It is never the source of truth by itself.

## Ownership

Backend ownership is explicit under:

```text
workflows/
tasks/events/
tasks/automation/
```

Frontend ownership is explicit under:

```text
features/workflow-builder/
features/work-automation/
```

The workflow domain does not inject the full `ProjectTaskService`. Task changes cross the domain boundary only through the task-owned `TaskAutomationMutationPort`. Task lifecycle changes reach workflows through task-domain events.

## Persistence

Portable Flyway migration **V50** creates:

- `workflow_definitions`
- `workflow_nodes`
- `workflow_edges`
- `workflow_executions`

Applied migrations remain append-only. After PR #144, new persistence starts at **V51+**.

Definitions are tenant scoped and have normalized unique names. Nodes persist stable node keys and canvas coordinates. Edges reference node keys within the same tenant/workflow. Execution rows retain the workflow version, trigger operation, source entity, outcome and bounded explanation/error text.

## Graph contract

A workflow contains:

- exactly one `TRIGGER`
- zero or more `CONDITION` nodes
- one or more reachable `ACTION` nodes in practical graphs
- 2–50 total nodes
- 1–100 total edges
- no cycles
- no unreachable nodes

Branch rules:

- trigger nodes use exactly one `DEFAULT` outgoing edge
- action nodes use zero or one `DEFAULT` outgoing edge
- condition nodes use one or two outgoing `TRUE` / `FALSE` branches
- one source node cannot define the same branch twice
- self edges are rejected

The backend validates the entire graph on create/update and validates the stored graph again before activation.

## Initial operations

Trigger operations:

- `TRIGGER_TASK_CREATED`
- `TRIGGER_TASK_STATUS_CHANGED`

Condition operations:

- `CONDITION_TASK_PRIORITY_EQUALS`
- `CONDITION_TASK_STATUS_EQUALS`

Action operations:

- `ACTION_SET_TASK_PRIORITY`
- `ACTION_SET_TASK_STATUS`

Configuration is deliberately closed and typed. Operations that require a value accept exactly one `value` key. Priority/status values are normalized and validated against the task enums. Arbitrary executable code is not accepted.

## Lifecycle

Definitions move through:

```text
DRAFT -> ACTIVE -> PAUSED -> ACTIVE
```

A draft can be edited and activated. An active workflow is immutable from the editor; it must be paused before a definition change. Editing increments `definitionVersion` so execution history identifies the exact graph version that ran.

## Runtime

Runtime flow:

```text
task transaction
    -> task-domain event
    -> after-commit workflow listener
    -> active tenant workflows
    -> matching trigger
    -> deterministic branch traversal
    -> condition evaluation
    -> task-owned mutation port
    -> execution outcome
```

Events are handled after the originating task transaction commits. The runtime evaluates only active workflows and records an execution before applying actions.

Automated task mutations intentionally do **not** publish another workflow-triggering event in v1. This prevents accidental automation feedback loops until explicit chaining and circuit-breaker semantics are designed.

## Authorization

Workflow definition/list/history access is tenant scoped:

- read/history: `project.read`
- create/update/activate/pause: `project.update`

Those permissions authorize workflow administration only. They do not grant authority over a task targeted by an action.

Before an action changes a task, `TaskAutomationMutationPort` re-checks the originating actor's current task/project authority using task-owned rules. Automation therefore cannot become a privilege-escalation path.

## Idempotency and execution history

`workflow_executions` has a unique `(tenant_id, workflow_id, event_key)` key. The recorder uses that key to avoid executing the same workflow/event pair twice.

Execution states are:

- `RUNNING`
- `SUCCEEDED`
- `FAILED`
- `SKIPPED`

The record stores:

- workflow and definition version
- trigger operation
- source entity type/id
- start/completion time
- bounded explanation or error message

The Automation workspace exposes the latest tenant-wide execution history and resolves workflow names from the same tenant catalog.

## Frontend UX

`/work-automation` contains a fourth tab: **Workflow builder**.

The v1 editor provides:

- workflow list and status
- a valid starter graph
- draggable trigger/condition/action cards
- SVG connection visualization
- node inspector for operation/configuration
- `DEFAULT`, `TRUE` and `FALSE` target selection
- condition/action insertion and non-trigger removal
- 50-node / 100-edge counters
- save draft
- activate
- pause
- active-workflow read-only state
- execution history with refresh, status, version, trigger, source and explanation/error

The editor uses the existing React/MUI stack and adds no graph-library dependency.

## Deliberate v1 constraints

Not included yet:

- arbitrary scripts or expressions
- external HTTP actions
- workflow-to-workflow chaining
- loops
- timers/delays
- human approval nodes
- multi-domain actions beyond task status/priority
- opaque AI-generated execution logic

Later workflow expansion should extend typed operations and narrow domain-owned action ports rather than weakening the graph contract.

## Tests and merge gates

Focused coverage includes graph validation, runtime trigger/condition/action execution, non-matching triggers, tenant-scoped execution-history mapping, starter-graph persistence and active-workflow read-only behavior.

Before merge, the final head must pass Repository Hygiene, Backend, PostgreSQL/Flyway, Frontend format/tests/coverage/lint/build, Security, Container CI, Qodana and Wiki Sync.
