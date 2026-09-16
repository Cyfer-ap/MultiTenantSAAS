# Roadmap

## Completed application milestones

- Billing/catalog lifecycle — #106
- Tenant-configurable outbound webhooks — #112
- Enterprise OIDC SSO / identity federation — #119
- Authorization delegation and Explain Access — #125/#126
- Engineering/documentation governance — #128
- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- Capability-aware Dashboard Refresh — #136
- Calendar / Deadline View — #137/#138
- Task Relationships + Task Planning — #139/#140
- Recurring Work + Project/Task Templates — #141/#143
- Visual Workflow Builder — #144
- Project Simulation / What-If Engine — #145
- Collaborative Whiteboard — #146/#147
- **Project Health / Risk Radar — #148**

## Project Health / Risk Radar — completed through #148

Risk Radar answers:

> **Why does this project need attention right now?**

The delivered v1 is advisory/read-only and provides explicit, explainable signals for:

- overdue open work
- blocked work, including unresolved open dependency blockers
- stale open work
- unassigned HIGH/URGENT work
- dependency bottlenecks / downstream exposure

Architecture:

```text
projectrisk
    -> ProjectRiskTaskSource
    -> ProjectRiskDependencySource
```

The calculation is bounded to 500 tasks, 1,000 dependency edges and 200 returned signals. Overall project risk is the maximum documented signal severity; there is no opaque weighted score.

Project surface:

```text
/projects/{projectId}?view=risk
```

Guardrails remain part of the product contract:

- no employee productivity ranking
- no secret activity scoring
- no assignment-count-as-capacity heuristic
- no automatic task/project mutation
- backend authorization before exposure
- completed/cancelled work excluded from open-work risk
- no persistence migration in v1

Detailed contract: `guides/project_risk_radar.md`.

## Current major product milestone

### Differentiated Work Platform Sequence

The older plan to move directly into bulk actions/CSV remains deliberately paused. Continue in this order unless a production/security issue or explicit product decision reprioritizes it.

1. ✅ **Visual Workflow Builder** — completed through #144.
2. ✅ **Project Simulation / What-If Engine** — completed through #145.
3. ✅ **Collaborative Whiteboard** — completed through #146/#147; live cursors/presence remain optional later enhancement work.
4. ✅ **Project Health / Risk Radar** — completed through #148.
5. 🚧 **Forms -> Workflow Engine — ACTIVE NOW** — structured internal intake that creates authorized work and can launch workflows.
6. **Approval Workflows** — reusable human review/approve/reject stages that compose with the workflow engine.
7. **Client / Guest Portal** — bounded external visibility, comments, review requests and approvals without broad tenant membership.
8. **Team Workload Engine** — capacity planning, overload detection and reassignment support without employee-surveillance scoring.
9. **Workspace Knowledge Graph** — permission-aware graph connecting projects, tasks, people, decisions, documents and dependencies.
10. **AI / Agent Teammates** — bounded agent work only after workflow, knowledge and authorization context are mature; human checkpoints remain mandatory for consequential actions.

## Feature 5 — Forms -> Workflow Engine — ACTIVE

The initial goal is not a general-purpose form/BPM platform. It is a bounded intake layer that safely turns structured submissions into existing work/workflow behavior.

### First-slice product scope

- versioned tenant/project-scoped form definitions
- bounded field schema and option counts
- safe field types first: text, textarea, number, date, boolean, select; authorized user/project references only where needed
- internal form builder and submission experience first
- server-side validation and normalized submission payloads
- submission provenance/audit context
- optional handoff into existing workflow runtime
- authorized task/project creation through narrow domain-owned creation contracts

### Architecture direction

Create an explicit form/intake owning domain. Do not put field-definition or submission orchestration into `ProjectTaskService`, `ProjectService` or the workflow coordinator.

Cross-domain behavior should look like:

```text
forms/intake
    -> work creation port(s) -> owning project/task domain
    -> workflow entry port   -> existing workflow runtime
```

The form domain owns form definitions/submissions. It does not own task/project/workflow repositories.

### Guardrails

- no arbitrary executable code in fields, validation or actions
- form definitions never grant access to submitted targets
- created work re-enters ordinary tenant isolation, authorization, quota, audit and lifecycle rules
- field count, option count and payload sizes remain bounded
- public/external forms are a later security boundary, not an accidental extension of internal forms
- do not duplicate the workflow engine inside the form domain

## Collaborative Whiteboard — optional later live-collaboration slice

A later enhancement may add replaceable real-time transport, reconnect/resynchronization, presence and cursors. The persisted V51 document model remains transport-independent and this work does not block the committed sequence.

## Product Experience & Work Management Enrichment — parked

Still valuable later:

- bulk actions and CSV import/export
- custom fields beyond what Forms specifically requires
- knowledge/documents beyond attachments
- user-facing analytics/reporting
- richer onboarding/workspace-switching/personalization

## Deferred platform work

### Production Operations & Disaster Recovery

Still important, but deliberately deferred from the immediate product sequence:

- PostgreSQL backup/export and retention strategy
- repeatable isolated restore drills
- health/readiness/metrics review
- actionable alerts
- incident/recovery runbooks
- secret/key rotation procedures
- broader failure-recovery/load validation
- production R2 verification

### Optional enterprise expansion

- SAML where required
- SCIM/directory provisioning where required
- richer session/device/MFA/passkey controls when prioritized

### Optional notification expansion

- digests
- live browser delivery
- web/mobile push where justified

## Independent provider/live-readiness track

- preserve Stripe as the working/validated Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, delegation non-escalation, verified provider reconciliation, immutable history, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.

New functionality must stay inside explicit domain modules and cross boundaries only through narrow services/contracts/events.
