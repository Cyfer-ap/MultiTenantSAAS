# Project Health / Risk Radar

Project Health / Risk Radar is an **advisory, explainable read model** over the current project task and dependency state. It does not mutate projects or tasks and does not assign opaque productivity scores to people.

## Ownership and boundaries

The backend owner is the explicit `projectrisk` domain. It performs risk calculations against narrow snapshots supplied by the domains that own the underlying data:

```text
projectrisk
    -> ProjectRiskTaskSource       -> task-owned adapter
    -> ProjectRiskDependencySource -> Task Relationships-owned adapter
```

Do not replace these contracts by injecting `ProjectTaskService`, `ProjectService`, `ProjectTaskRepository`, or Task Relationships persistence directly into the risk coordinator.

Frontend ownership lives under `features/project-risk`. A neutral `features/project-tools` wrapper exposes project-level planning tools without making Whiteboard or Risk Radar own the legacy project-details page.

## API

```text
GET /api/tenants/{tenantId}/projects/{projectId}/risk
```

The endpoint is read-only and uses the existing project task-read authorization semantics. Backend authorization remains authoritative regardless of frontend navigation.

## V1 signal model

The first version reports five explicit signal families:

- **Overdue task** — open task whose due date is before the current instant.
- **Blocked task** — open task explicitly marked `BLOCKED` or with unresolved open dependency blockers.
- **Stale task** — open task whose `updatedAt` is older than the configured stale threshold (14 days in v1).
- **Unassigned critical work** — open `HIGH` or `URGENT` priority task with no assignee.
- **Dependency bottleneck** — open task upstream of at least three open dependent tasks.

Every returned signal carries a severity, source task, plain-language explanation, affected/related task context, and relevant task timestamps. Overall project risk is the maximum severity among the returned documented signals; there is no hidden weighted score.

## Bounds

A single calculation is bounded to:

- 500 project tasks
- 1,000 dependency edges
- 200 returned signals

If source data exceeds a bound, the response records that limitation rather than pretending the analysis is complete.

## Interpretation guardrails

Risk Radar is intentionally not an employee evaluation system.

- completed/cancelled tasks do not create open-work risk
- workload pressure is not calculated until the platform has an explicit capacity/availability model
- assignment counts are not treated as employee capacity
- no people ranking, productivity score, keystroke/activity monitoring, or surveillance proxy is permitted
- risk signals remain advisory and do not automatically change task/project state
- stale work uses task `updatedAt`; v1 does not infer activity from comments, presence, or user behavior

## Project-facing surface

The project-detail tools entry exposes **Open risk radar**. The persisted URL is:

```text
/projects/{projectId}?view=risk
```

The surface shows:

- overall documented severity
- summary counts for each signal family
- ordered explainable signal cards
- task deep-links back to `/projects/{projectId}?task={taskId}`
- explicit interpretation/limitation notes
- manual refresh only; no auto-fix/apply behavior

## Validation

Backend tests use a fixed `Clock` so overdue/stale behavior is deterministic. Coverage includes the five signal families and verifies that completed dependency blockers do not produce blocked/bottleneck risk.

Frontend tests cover explainable signal rendering, the source-task deep link, and the no-signal state.

## Persistence

Risk Radar introduces **no migration**. V51 remains frozen. If a later version requires persisted acknowledgements, thresholds, snapshots, or history, that work must begin at V52+ and should be justified independently from the read-only v1 calculation.
