# MultiTenantSAAS — Current Checkpoint

Updated: 2026-09-15
Repository: `Cyfer-ap/MultiTenantSAAS`
Branch: `main` after PR #143 merge

This file is the **single repository-side source of truth for current project status**. Do not create additional progress/checkpoint mirrors.

## Current phase

**Product Experience & Work Management Enrichment**

Delivered enrichment milestones now include:

- Global Search — #129
- Command Palette — #130
- Favorites + Recently Viewed — #131/#132
- My Work — #133
- Saved Views — #134
- capability-aware Dashboard Refresh — #136
- authorization-safe Calendar / Deadline View — #137/#138
- task relationships backend + Task Planning UI — #139/#140
- recurring-task + project-scoped task-template backend foundation — #141
- documentation/architecture checkpoint — #142
- tenant-scoped project templates + Work Automation & Templates workspace — #143

The **Recurring Work + Project/Task Templates milestone is complete with #143**.

## Work automation capability checkpoint

### Recurring work

- task recurrence only in v1
- `DAILY`, `WEEKLY`, `MONTHLY`
- interval 1–52 with explicit IANA timezone
- optional due offset/end/max occurrences
- timezone/calendar-aware advancement across DST/month boundaries
- edits affect future work only
- pause/resume supported; resume skips paused-period schedules
- discovery capped at 50 definitions/pass and catch-up at 5 occurrences/definition/pass
- pessimistic per-definition materialization lock
- unique `(definition_id, scheduled_for)` occurrence key for retry/multi-instance idempotency
- generation failures pause the definition with bounded diagnostics

### Project-scoped task templates

- normalized unique name per project
- maximum 100 templates/project
- task title/description/priority, optional assignee, optional due offset
- instantiate through task-owned `TaskCreationPort`
- existing tasks remain snapshots when a template changes or is deleted

### Tenant-scoped project templates

- normalized unique name per tenant
- project name seed, optional description and non-archived initial status
- zero to 50 ordered starter-task snapshots/template
- starter snapshots contain title, optional description, priority and optional due offset
- instantiate through project-owned `ProjectCreationPort` and task-owned `TaskCreationPort`
- invoking actor becomes initial `PROJECT_LEAD` through ordinary project lifecycle rules
- ordinary and template-driven project creation converge on the same project creation adapter
- project quota, active actor/tenant validation, audit and lifecycle/webhook behavior remain project-owned
- project + starter-task instantiation is transactional snapshot/copy semantics

### Frontend

Standalone `/work-automation` workspace owns:

- authorization-safe project discovery for tenant-wide and project-scoped users
- recurring rule create/edit/pause/resume/history
- project-scoped task-template create/edit/delete/instantiate
- tenant-scoped project-template create/edit/delete/instantiate
- project-name override on template instantiation
- bounded 50-row starter-task editor

Frontend ownership remains explicit under `features/recurring-work`, `features/task-templates`, `features/project-templates`, and `features/work-automation`.

Detailed contract: `guides/recurring_work_and_templates.md`.

## Database checkpoint

Portable PostgreSQL Flyway migrations extend through **V49**.

```text
V40 tenant identity-provider configuration
V41 OIDC authorization transactions + tenant federated identities
V42 tenant SSO policy
V43 OIDC browser session handoffs
V44 authorization delegation provenance + authorization.delegate
V45 personal workspace favorites/recent items
V46 saved views
V47 task parent/dependency/project-label relationships
V48 recurring task definitions/occurrences + project task templates
V49 tenant project templates + bounded starter-task snapshots
```

Never rewrite an applied migration. New persistence starts at **V50+**.

## Non-negotiable architecture rule

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

Recent reference implementations:

- Global Search — coordinator + contributor contracts + domain-owned adapters
- Personal Workspace — personal state coordinator + project/task resolver adapters
- My Work — attention coordinator + narrow task source
- Saved Views — persistence/definition domain + validator SPI
- Calendar — calendar coordinator + deadline-source SPI
- Task Relationships — query/graph/label services + narrow task gateway/change sink
- Recurring Work / Task Templates — explicit owning domains + `TaskCreationPort`
- Project Templates — explicit `projecttemplates` domain + project-owned `ProjectCreationPort` + task-owned `TaskCreationPort`

Do not move recurrence into Calendar, template logic into `ProjectTasksSection`, or project-template orchestration into the full legacy `ProjectService`.

## Established application foundations

Major capabilities include authentication/tenant isolation, invitations/password recovery, scoped authorization/delegation/Explain Access, organization hierarchy, projects/tasks/collaboration, task relationships, work automation/templates, search/command palette/personal workspace/My Work/Saved Views/Dashboard/Calendar, R2-compatible attachments, durable notifications/email, API keys/quotas/usage, Stripe/Razorpay billing abstractions, outbound webhooks, enterprise OIDC SSO, auditability, and PostgreSQL/Flyway CI/security/container validation.

## Provider status

### Stripe

Working and validated in deployed Test Mode. Hosted checkout, signed lifecycle synchronization, provider-side cancellation, reconciliation and managed Product/Price provisioning are implemented.

### Razorpay

Application integration and managed Plan provisioning remain implemented. Recurring Test Mode authorization is provider-sandbox blocked; keep the integration available while treating live readiness separately.

## Engineering-health checkpoint

The codebase remains feasible for continued development without a rewrite, provided new work continues enforcing explicit domains and narrow cross-domain contracts.

Priority debt remains:

1. older inconsistent backend package/domain boundaries
2. large legacy application services with accumulated orchestration dependencies
3. older tenant-isolation paths relying partly on repository/query discipline
4. duplicated backend/frontend API contracts
5. growing route/navigation aggregation points
6. comprehensive scale/load characterization still missing
7. deferred operational maturity: backup/restore drills, recovery/load validation and production R2 verification

Canonical assessment: `guides/ENGINEERING_STANDARDS.md`.

## Next product sequence

1. **bulk actions + CSV import/export**
2. custom fields/forms
3. workflows/approvals + knowledge/documents
4. user-facing analytics/reporting and selected differentiated experiments
5. onboarding/workspace-switching/personalization polish

Every new capability must first choose an explicit domain owner and narrow integration boundaries before code is added.

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
- `guides/task_relationships.md` — task hierarchy/dependency/label contract
- `guides/recurring_work_and_templates.md` — recurring work/task/project template contract
- `wiki/*.md` — canonical reader-facing Wiki source
- `wiki/Roadmap.md` — product direction

Do not recreate duplicate checkpoint/progress/manifests.
