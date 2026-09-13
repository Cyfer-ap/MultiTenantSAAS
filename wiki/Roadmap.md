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

## Next major product milestone

### 1. Product Experience & Work Management Enrichment

The platform foundation is broad enough that the immediate priority is now user-facing product depth and daily usability.

#### Phase A — discoverability and personal productivity

- global authorized search
- `Ctrl/Cmd + K` command palette
- favorites and recently viewed items
- My Work / unified attention queue
- saved filters/views
- capability-aware dashboard refresh
- better empty states/onboarding and quick-create UX

#### Phase B — deeper work management

- Kanban board
- calendar/deadline view
- subtasks
- task dependencies
- labels/tags
- recurring work
- milestones/templates
- bulk actions and CSV import/export

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

## Core product gaps to keep visible

Before calling the product layer mature, revisit:

- smooth post-login multi-workspace switching
- global search/commands
- My Work and saved views
- richer task/project relationships and views
- templates
- custom fields/forms
- workflow/approval engine
- first-class knowledge/documents
- user-facing analytics/reporting
- import/export and bulk productivity
- richer personalization/timezone/locale UX

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

New user-facing features must remain permission-aware and tenant-safe. Search, analytics, automation and future AI must filter through the same authorization boundary rather than attempting to repair access after data retrieval.
