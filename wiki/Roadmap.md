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
- Project Health / Risk Radar — #148
- **Forms -> Workflow Engine — #152**
- **Approval Workflows — #154**

## Forms -> Workflow Engine — completed through #152

Forms now provides a bounded authenticated intake layer for project work without bypassing existing task/workflow ownership.

Delivered v1:

- tenant/project-scoped versioned form definitions
- bounded `TEXT`, `TEXTAREA`, `NUMBER`, `DATE`, `BOOLEAN` and `SELECT` field schemas
- backend-authoritative definition/submission validation
- authenticated internal submissions and recent submission history
- task title/description/due-date/priority mapping
- normal task creation through task-owned `TaskCreationPort`
- optional typed `TRIGGER_FORM_SUBMITTED` workflow entry through `WorkflowFormSubmissionPort`
- after-commit form-specific workflow dispatch
- preserved ordinary `TRIGGER_TASK_CREATED` behavior for generated tasks
- exact decimal preservation from browser lexical input to backend `BigDecimal`
- V52 persistence for form definitions, fields and submission provenance

Architecture:

```text
forms
    -> ProjectAccessPort
    -> TaskCreationPort
    -> WorkflowFormSubmissionPort
```

Guardrails remain part of the v1 contract:

- no arbitrary executable code or expressions
- no direct writes into task/project/workflow persistence
- form configuration never grants target-resource permission
- created work re-enters normal authorization/quota/audit/lifecycle behavior
- field/options/payload sizes are bounded
- public/anonymous intake is excluded until a separate security/rate-limit/abuse boundary is designed

Detailed contract: `guides/forms_workflow_engine.md`.

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

Guardrails include no employee productivity ranking, secret activity scoring, assignment-count-as-capacity heuristic or automatic task/project mutation.

Detailed contract: `guides/project_risk_radar.md`.

## Current major product milestone

### Differentiated Work Platform Sequence

The older plan to move directly into bulk actions/CSV remains deliberately paused. Continue in this order unless a production/security issue or explicit product decision reprioritizes it.

1. ✅ **Visual Workflow Builder** — completed through #144.
2. ✅ **Project Simulation / What-If Engine** — completed through #145.
3. ✅ **Collaborative Whiteboard** — completed through #146/#147; live cursors/presence remain optional later enhancement work.
4. ✅ **Project Health / Risk Radar** — completed through #148.
5. ✅ **Forms -> Workflow Engine** — completed through #152.
6. ✅ **Approval Workflows** — completed through #154 with bounded human checkpoints, reviewer re-authorization and same-execution workflow resume.
7. 🚧 **Client / Guest Portal — ACTIVE NOW** — bounded external visibility, comments, review requests and approvals without broad tenant membership.
8. **Team Workload Engine** — capacity planning, overload detection and reassignment support without employee-surveillance scoring.
9. **Workspace Knowledge Graph** — permission-aware graph connecting projects, tasks, people, decisions, documents and dependencies.
10. **AI / Agent Teammates** — bounded agent work only after workflow, knowledge and authorization context are mature; human checkpoints remain mandatory for consequential actions.

## Feature 6 — Approval Workflows — completed through #154

Delivered v1 includes bounded project-scoped approval definitions/stages, reviewer configuration and request-time snapshots; durable decision provenance; current reviewer re-authorization; explicit self-approval policy; concurrency/replay-safe decisions; reviewer inbox/history; and workflow pause/resume through typed `APPROVED` / `REJECTED` branches.

Architecture:

```text
workflow runtime -> ApprovalCheckpointPort -> approvals
approvals -> ApprovalReviewerEligibilityPort -> project eligibility
approvals -> ApprovalResolvedEvent -> workflow runtime -> TaskAutomationMutationPort
```

V53 is now the immutable migration boundary. Public/guest approvals, escalation, expiry, reassignment, quorum and parallel stages remain outside this slice.

Detailed contract: `guides/approval_workflows.md`.

## Feature 7 — Client / Guest Portal — ACTIVE

The portal should provide bounded external collaboration without making clients/guests broad tenant members.

### First-slice product scope

- revocable, expiring tenant/project/resource-scoped external access grants
- separate guest invitation/session identity with hashed/rotatable credentials
- a small explicitly shared project/task/review projection
- bounded guest comments and review responses through owning-domain ports
- external approval through an approval-owned contract only when both grant scope and request scope allow it
- immutable guest/grant provenance
- deterministic revocation/session invalidation
- anti-enumeration, rate limiting and abuse controls on public endpoints

### Architecture direction

```text
external grant/credential
      ↓
external-access domain
      ↓ grant-scoped projection/mutation contracts
owning project/task/comment/approval domains
```

A guest is not a tenant member. Stored grants are explicit capabilities, not RBAC assignments, and every resource reference must be revalidated against the active grant before exposure or mutation.

### Guardrails

- no whole-tenant or whole-project visibility by default
- no guest identity on normal tenant-authenticated APIs
- no public arbitrary search, user directory, billing/admin or workflow-management surface
- revocation/expiry must invalidate future access even with stale browser state
- portal access alone is not approval authority
- public endpoints require anti-enumeration, rate-limit and abuse protections
- new persistence starts at V54+; V53 and earlier remain immutable

## Collaborative Whiteboard — optional later live-collaboration slice

A later enhancement may add replaceable real-time transport, reconnect/resynchronization, presence and cursors. The persisted V51 document model remains transport-independent and this work does not block the committed sequence.

## Product Experience & Work Management Enrichment — parked

Still valuable later:

- bulk actions and CSV import/export
- broader custom fields beyond the bounded Forms schema
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
