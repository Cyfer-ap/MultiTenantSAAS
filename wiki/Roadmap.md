# Roadmap

## Completed application milestones

### Billing/catalog lifecycle

Closed through PR #106.

### Tenant-configurable outbound webhooks

Closed through PR #112.

### Enterprise OIDC SSO / identity federation

Closed through PR #119.

### Authorization delegation and Explain Access

Closed through PR #125, with milestone documentation closed in PR #126.

Completed authorization capabilities include:

- structured access decisions from the enforcement evaluator
- tenant-admin Explain Access with stable decision reasoning
- V44 durable delegation provenance and `authorization.delegate`
- create/list/revoke delegation lifecycle and audit events
- explicit direct parent assignment for each delegated grant
- permission/scope/validity non-escalation enforcement
- one-level delegation only and protected authorization permissions
- runtime source revalidation after parent revocation/expiry/narrowing
- delegation-safe reference data for delegate-only actors
- direct-vs-delegated Explain Access provenance
- manager and delegate-only Authorization workspace UX

### Engineering/documentation governance

Consolidated through PR #128. Current status, handoff, architecture and engineering standards have single canonical owners, with PR/CI guardrails against documentation drift and new domain-coupling debt.

### Global Search

Completed through PR #129.

Provides tenant-aware bounded search of accessible projects, tasks and people with permission/project-membership-aware candidate selection and authoritative project/task access revalidation.

### Command Palette

Completed through PR #130.

Provides `Ctrl/Cmd + K` and `/` quick-open, authorized search reuse, keyboard operation, permission-filtered workspace commands and owning-domain quick actions.

### Favorites + Recently Viewed

Completed through PR #131, with contextual favorite controls in PR #132.

### My Work

Completed through PR #133 with an explicit `mywork` domain and narrow `MyWorkTaskSource`.

### Saved Views

Completed through PR #134 with V46 `saved_views` persistence and a context-validator SPI.

### Capability-aware Dashboard Refresh

Completed through PR #136 as frontend composition over existing authorized contracts.

### Calendar / Deadline View

Completed through PR #137, with UI refinement in PR #138.

Provides:

- `/calendar` workspace
- local-time Monday-start month view and selected-day agenda
- task due dates only
- narrow backend `calendar` domain + `CalendarDeadlineSource`
- authoritative task-read revalidation
- maximum 93-day range and 500 returned items with truncation signaling
- no dedicated schema migration

### Task Relationships + Task Planning

Backend foundation completed through PR #139 and user-facing Task Planning through PR #140.

Provides:

- V47 optional task parent relationship
- bounded same-project subtask hierarchy
- self-parent and ancestry-cycle prevention
- directed `blocking task -> dependent task` dependency edges
- duplicate/self/directed-cycle prevention for dependencies
- bounded graph validation and bounded relationship reads
- project-scoped reusable task labels with normalized uniqueness
- task-label assignment limits
- explicit backend `taskrelationships` domain split into query, graph and label responsibilities
- narrow `TaskRelationshipTaskGateway` and `TaskRelationshipChangeSink`
- `/task-planning` frontend workspace under `features/task-relationships`
- authorization-safe task selection through Global Search
- hierarchy, blocker/dependent and label management UX
- shared-navigation registration so Command Palette and Dashboard quick actions inherit the workspace

This milestone deliberately does **not** automate task status from relationships and does not create generic graph infrastructure.

## Current major product milestone

### 1. Product Experience & Work Management Enrichment

The platform foundation is broad enough that the immediate priority is user-facing product depth and daily usability.

#### Phase A — discoverability and personal productivity

- ✅ global authorized search
- ✅ command palette and quick actions
- ✅ favorites/recent items
- ✅ contextual favorite controls
- ✅ My Work attention queue
- ✅ saved views
- ✅ capability-aware dashboard refresh
- 🟡 onboarding/empty-state/quick-create polish — continue incrementally

#### Phase B — deeper work management

- ✅ calendar/deadline view
- ✅ subtasks
- ✅ directed task dependencies
- ✅ project-scoped labels/tags
- existing Kanban task board should be iterated rather than rebuilt
- recurring work — **next**
- project/task templates — **next**
- bulk actions and CSV import/export

The next slice should define recurrence and template semantics before persistence work: timezone/cadence ownership, occurrence idempotency, materialization horizon, pause/edit behavior, template scope, copy/snapshot rules, versioning expectations, authorization and bounded instantiation.

Do not put recurrence generation inside Calendar or task-relationship graph services. Those domains have different lifecycles.

#### Phase C — tenant adaptability

- custom fields
- forms
- workflow/approval automation
- knowledge/documents beyond attachments
- user-facing analytics/reporting

#### Phase D — selected differentiators

Use `guides/Wild_Thoughts.md` as the idea vault. Candidate experiments include Permission Lens, Context Capsules, Change Blast-Radius Preview, Alternate-Reality Planning, Responsibility Gap Detector, Assumption Register, Contradiction Radar, Context Compression Checkpoints, Project Necromancer, Bureaucracy Detector, Reality-vs-Plan Drift and Human Checkpoints for automation/AI.

No experiment becomes a roadmap commitment merely because it is listed.

## Immediate sequence

1. **recurring work + project/task templates**
2. bulk actions + CSV import/export
3. custom fields/forms + workflows/approvals + knowledge/documents
4. user-facing analytics/reporting
5. selected differentiated experiments after the core product layer is strong

## Core product gaps to keep visible

Before calling the product layer mature, revisit:

- smooth post-login multi-workspace switching
- recurring work and templates
- custom fields/forms
- workflow/approval engine
- first-class knowledge/documents
- user-facing analytics/reporting
- import/export and bulk productivity
- richer onboarding, personalization, timezone and locale UX

## Deferred platform work

### 2. Production Operations & Disaster Recovery

Still important, but deliberately deferred from the immediate sequence while the product is enriched.

Target capabilities remain:

- PostgreSQL backup/export and retention strategy
- repeatable isolated restore drills
- health/readiness/metrics review
- actionable alerts
- incident/recovery runbooks
- secret/key rotation procedures

### 3. Broader failure-recovery/load and production R2 verification

Follow the operations/DR baseline later.

### 4. Optional enterprise expansion

- SAML where required
- SCIM/directory provisioning where required
- richer session/device/MFA/passkey controls when prioritized

### 5. Optional notification expansion

- digests
- live browser delivery
- web/mobile push where justified

## Independent provider/live-readiness track

- preserve Stripe as the working/validated Test Mode path
- keep Razorpay integration/catalog provisioning available while recurring sandbox authorization remains blocked
- enable live credentials/catalog only after provider-specific readiness review
- validate provider account configuration, billing webhook endpoints, tax/compliance and production operational runbooks separately

## Engineering rules

Preserve tenant isolation, backend-authoritative authorization, delegation non-escalation, verified provider reconciliation, webhook-authoritative normal billing state, immutable history, Flyway invariants, database-backed concurrency, auditability, SSRF protections and server-only secrets/provider identifiers.

New user-facing features must remain permission-aware and tenant-safe. Search, favorites/recent resolution, My Work, saved views, calendar projections, Task Planning, analytics, automation and future AI must filter through the same authorization boundary rather than attempting to repair access after data retrieval.

New functionality must stay inside explicit domain modules and cross domain boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views, Calendar and Task Relationships are backend reference implementations; Dashboard remains the frontend composition reference. Recurring work/templates must use their own owning domain rather than expanding task graph, Calendar or legacy task services.
