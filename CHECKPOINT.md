# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-16
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch target: `main` after PR #148 merge

This file is the **single repository-side source of truth for current project status**. Do not create duplicate progress/checkpoint mirrors.

## Current phase

**Differentiated Work Platform Sequence**

Delivered milestones now include:

- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- capability-aware Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- recurring work + task/project templates — #141/#143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145
- Collaborative Whiteboard foundation + persisted visual workspace — #146/#147
- **Project Health / Risk Radar — #148**

**Forms -> Workflow Engine is the active next feature.**

Live whiteboard presence/cursors remain an optional later enhancement and do not block the committed sequence. Bulk/CSV, custom fields, broader knowledge/documents and analytics remain parked unless explicitly reprioritized.

## Project Health / Risk Radar checkpoint — #148

Risk Radar is complete as an explainable, advisory project-health surface.

### Ownership and boundaries

The explicit `projectrisk` backend domain owns risk calculation and depends only on narrow source contracts:

```text
projectrisk
    -> ProjectRiskTaskSource        -> task-owned adapter
    -> ProjectRiskDependencySource  -> Task Relationships-owned adapter
```

Frontend ownership lives under `features/project-risk`; project-level tool navigation is separated through `features/project-tools` rather than expanding the legacy project-details page further.

### API and UI

```text
GET /api/tenants/{tenantId}/projects/{projectId}/risk
```

The endpoint is read-only and reuses authoritative project task-read authorization.

Project-facing surface:

```text
/projects/{projectId}?view=risk
```

The UI shows overall documented severity, signal-family counts, plain-language reasons, task deep-links, explicit interpretation limits and manual refresh. It never mutates project/task state.

### Explainable v1 signals

- overdue open tasks
- blocked tasks, including unresolved open dependency blockers
- stale open tasks using task `updatedAt`
- unassigned HIGH/URGENT work
- dependency bottlenecks based on downstream open dependents

Overall risk is the maximum documented signal severity; there is no hidden weighted score.

### Guardrails and bounds

- advisory/read-only only
- no employee ranking or productivity scoring
- assignment counts are not treated as capacity
- workload pressure remains omitted until an explicit availability/capacity contract exists
- completed/cancelled work is excluded from open-risk calculations
- max 500 tasks, 1,000 dependency edges and 200 returned signals per calculation
- no migration; V51 remains the latest migration

Detailed contract: `guides/project_risk_radar.md`.

## Existing differentiated foundations

### Collaborative Whiteboard — #146/#147

V51 owns project-scoped whiteboards, nodes and connectors with optimistic versioning, bounded documents, archived-project mutation protection and sticky/text -> task conversion through task-owned `TaskCreationPort`.

Private workspace: `/projects/:projectId/whiteboards`.

Detailed contract: `guides/collaborative_whiteboard.md`.

### Project Simulation — #145

The explicit `projectsimulation` domain provides bounded, read-only due-date/assignee/dependency scenarios with downstream exposure, dependency-conflict comparison and workload deltas. It has no hidden apply path.

Private workspace: `/projects/:projectId/simulation`.

Detailed contract: `guides/project_simulation.md`.

### Visual Workflow Builder — #144

V50 owns workflow definitions/nodes/edges/executions, typed bounded graphs, after-commit task-domain events, idempotent execution history and task mutations through `TaskAutomationMutationPort`.

Detailed contract: `guides/visual_workflow_builder.md`.

### Recurring work + templates — #141/#143

V48/V49 recurrence and template generation continues through task-owned `TaskCreationPort` and project-owned `ProjectCreationPort`, preserving normal quota, authorization, audit and lifecycle rules.

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable PostgreSQL Flyway migrations extend through **V51**:

```text
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
V48 recurring task definitions/occurrences + project task templates
V49 tenant project templates + bounded starter-task snapshots
V50 visual workflow definitions/nodes/edges/executions
V51 project whiteboards/nodes/connectors
```

Project Simulation and Risk Radar add no migration. Never rewrite an applied migration. New persistence starts at **V52+**.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Current reference implementations include Search contributor contracts, Personal Workspace resolvers, My Work source ports, Saved Views validation SPI, Calendar deadline sources, Task Relationships gateways, `TaskCreationPort`, `ProjectCreationPort`, workflow events/`TaskAutomationMutationPort`, Project Simulation source ports, Whiteboard project/task ports and Risk Radar task/dependency sources.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning remain implemented. Recurring Test Mode authorization remains provider-sandbox blocked; keep the integration available while treating live readiness separately.

## Engineering-health checkpoint

The codebase remains feasible for continued development without a rewrite if new work preserves explicit domains and narrow boundaries.

Priority debt remains:

1. older inconsistent backend package/domain boundaries
2. large legacy application services with accumulated orchestration dependencies
3. older tenant-isolation paths relying partly on repository/query discipline
4. duplicated backend/frontend API contracts
5. growing route/navigation aggregation points
6. comprehensive scale/load characterization still missing
7. deferred operational maturity: backup/restore drills, recovery/load validation and production R2 verification

Canonical assessment: `guides/ENGINEERING_STANDARDS.md`.

## Next committed product sequence

Continue in this order:

1. **Forms -> Workflow Engine — ACTIVE NEXT**
2. Approval Workflows
3. Client / Guest Portal
4. Team Workload Engine
5. Workspace Knowledge Graph
6. AI / Agent Teammates
7. resume parked backlog such as bulk actions/CSV, custom fields, knowledge/documents and broader analytics unless priorities are explicitly changed

Forms must create authorized work through narrow domain-owned contracts and compose with the existing workflow engine rather than bypassing it.

## Deferred platform work

Production Operations & Disaster Recovery remains important but intentionally deferred from the immediate product sequence:

- PostgreSQL backup/restore drills
- health/readiness/metrics review and alerting
- incident/recovery runbooks
- broader failure-recovery/load validation
- production R2 verification

Optional SAML/SCIM, MFA/passkeys/device management and richer notification channels remain demand-driven.

## Documentation ownership

- `CHECKPOINT.md` — current status
- `HANDOFF.md` — current resume/next action
- `AGENTS.md` — persistent engineering contract
- `guides/README.md` — documentation ownership/index
- `guides/current_architecture.md` — canonical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical-health assessment and quality rules
- `guides/recurring_work_and_templates.md` — work-generation contracts
- `guides/visual_workflow_builder.md` — workflow graph/runtime/canvas contract
- `guides/project_simulation.md` — advisory What-If contract
- `guides/collaborative_whiteboard.md` — whiteboard persistence/workspace contract
- `guides/project_risk_radar.md` — Risk Radar source/bounds/signal/UI contract
- `guides/Wild_Thoughts.md` — product idea vault and committed sequence section
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.
