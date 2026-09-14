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

Provides:

- tenant-aware search of accessible projects, tasks and people
- bounded database queries and result limits
- exact/prefix/substring relevance ranking
- permission/project-membership-aware candidate selection
- authoritative project/task access revalidation
- reusable backend contributor contracts and frontend search query contracts

### Command Palette

Completed through PR #130.

Provides:

- `Ctrl/Cmd + K` and `/` workspace quick-open
- global authorized search reuse rather than a second discovery path
- Arrow Up/Down + Enter keyboard operation
- permission-filtered workspace navigation commands
- direct Create Project and Invite User actions through existing domain dialogs
- authorization + subscription-entitlement gating for quick-create actions
- separate `features/command-palette` UI orchestration domain

Create Task remains intentionally outside the global palette until a project-aware task capability/picker contract can represent project-lead membership and scoped authorization without duplicating task-access logic in the shell.

### Favorites + Recently Viewed

Completed through PR #131, with contextual project/task favorite controls added in PR #132.

Provides:

- server-backed tenant/user-scoped personal workspace state
- favorite projects and tasks
- recently viewed project/task tracking
- permission-aware resolution on writes and reads
- immediate disappearance of revoked/deleted/inaccessible references
- idempotent unfavorite even after access is lost
- reusable personal-workspace contracts for future dashboard/mobile consumers

### My Work

Completed through PR #133.

Provides:

- personal assigned-open-task attention queue
- overdue, blocked, due-soon, in-progress and remaining-work classification
- bounded source reads with authoritative readability checks
- summary counts and deep links
- explicit `mywork` domain with a narrow `MyWorkTaskSource` contract

### Saved Views

Completed through PR #134.

Provides:

- My Work filters for search, attention, status and priority
- server-backed user-owned saved views
- create/apply/update/delete lifecycle
- allow-listed and normalized persisted definitions
- explicit `savedviews` domain and frontend `features/saved-views`
- context-validator SPI prepared for later project-task view adoption
- V46 `saved_views` persistence

### Capability-aware Dashboard Refresh

Completed through PR #136.

Provides:

- personal My Work summary and bounded attention preview
- Favorites and Recently Viewed context on the dashboard
- capability-aware quick actions using the shared workspace-navigation contract
- preserved tenant-wide users/projects/tasks health metrics
- local degradation when personal widgets fail
- frontend composition only; no new dashboard backend service or schema migration

### Calendar / Deadline View

Completed through PR #137.

Provides:

- `/calendar` workspace route and shared navigation entry
- local-time Monday-start six-week month grid
- previous/next month and Today navigation
- selected-day agenda with task status, priority, project context and deep links
- task due dates only; projects currently have no deadline field and no synthetic project dates are introduced
- explicit backend `calendar` domain with narrow `CalendarDeadlineSource`
- task-owned deadline adapter with tenant/date/project narrowing and authoritative task-read revalidation
- maximum 93-day request range and 500 returned deadlines with explicit truncation signaling
- reuse of the existing indexed task `due_at` field; no new Flyway migration

## Current major product milestone

### 1. Product Experience & Work Management Enrichment

The platform foundation is broad enough that the immediate priority is user-facing product depth and daily usability.

#### Phase A — discoverability and personal productivity

- ✅ global authorized search
- ✅ `Ctrl/Cmd + K` command palette and quick actions
- ✅ favorites and recently viewed items
- ✅ contextual favorite controls
- ✅ My Work / unified attention queue
- ✅ saved filters/views for My Work
- ✅ capability-aware dashboard refresh
- 🟡 better empty states/onboarding and quick-create UX — continue incrementally with future product slices

Phase A now has a coherent daily-use foundation. Onboarding/empty-state polish remains an ongoing UX concern rather than a blocker for deeper work management.

#### Phase B — deeper work management

- ✅ calendar/deadline view
- existing Kanban task board should be iterated rather than rebuilt
- subtasks — **next**
- task dependencies — **next**
- labels/tags — **next**
- recurring work
- milestones/templates
- bulk actions and CSV import/export

The next slice should establish explicit task-relationship invariants before UI expansion: same-tenant/project hierarchy rules, dependency direction and cycle prevention, bounded traversal, deterministic cleanup behavior and label ownership/uniqueness.

#### Phase C — tenant adaptability

- custom fields
- forms
- workflow/approval automation
- knowledge/documents beyond attachments
- user-facing analytics/reporting

#### Phase D — selected differentiators

Use `guides/Wild_Thoughts.md` as the idea vault. Candidate experiments include:

- Permission Lens
- Context Capsules
- Change Blast-Radius Preview
- Alternate-Reality Planning
- Responsibility Gap Detector
- Assumption Register
- Contradiction Radar
- Context Compression Checkpoints
- Project Necromancer
- Bureaucracy Detector
- Reality-vs-Plan Drift
- Human Checkpoints for automation/AI

No experiment becomes a roadmap commitment merely because it is listed.

## Immediate sequence

1. subtasks + task dependencies + labels/tags
2. recurring work + project/task templates
3. bulk actions + CSV import/export
4. custom fields/forms + workflows/approvals + knowledge/documents
5. user-facing analytics/reporting
6. selected differentiated experiments after the core product layer is strong

## Core product gaps to keep visible

Before calling the product layer mature, revisit:

- smooth post-login multi-workspace switching
- richer task/project relationships
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

New user-facing features must remain permission-aware and tenant-safe. Search, favorites/recent resolution, My Work, saved views, calendar projections, analytics, automation and future AI must filter through the same authorization boundary rather than attempting to repair access after data retrieval.

New functionality must stay inside explicit domain modules and cross domain boundaries only through narrow services/contracts/events. Search, Personal Workspace, My Work, Saved Views and Calendar are backend reference implementations; Dashboard is the frontend composition reference. Task hierarchy/dependency/label work must preserve those boundaries and must not become generic graph infrastructure prematurely.
