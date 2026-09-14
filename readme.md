# Multi-Tenant SaaS Platform

A production-oriented full-stack multi-tenant SaaS platform focused on tenant isolation, permission-oriented authorization, collaboration, subscription enforcement, external billing, durable integrations, enterprise OIDC SSO, PostgreSQL correctness, and an expanding work-management/product layer.

For current project status and next work, use **`CHECKPOINT.md`** and **`HANDOFF.md`** rather than this README. This file intentionally avoids volatile milestone snapshots.

## Platform capabilities

### Tenant plane

- secure tenant onboarding, workspace discovery, JWT/browser sessions, password recovery and invitations
- users and organization hierarchy
- scoped permission authorization with bounded delegation and Explain Access
- projects, tasks, priorities, due dates, assignees, comments, replies, mentions, pins and activity
- bounded subtasks, directed task dependencies and project-scoped task labels
- dedicated Task Planning workspace over authorization-safe task discovery
- recurring-task definitions/materialization with explicit timezone and idempotent occurrence tracking
- project-scoped reusable task templates
- tenant-scoped reusable project templates with bounded starter-task snapshots
- dedicated Work Automation & Templates workspace for recurring rules and task/project template lifecycle
- permission-aware Global Search and capability-aware Command Palette
- server-backed Favorites + Recently Viewed with contextual favorite controls
- My Work personal attention queue and server-backed Saved Views
- capability-aware dashboard combining personal attention/context with tenant-wide health metrics
- authorization-safe Calendar / Deadline View over accessible task due dates
- S3/R2-compatible attachments
- durable in-app/email notifications and preferences
- subscription lifecycle enforcement, quotas and usage metering
- provider-backed checkout, cancellation, reconciliation and immutable subscription history
- tenant API keys restricted to external APIs with metering/plan limits
- tenant-configurable HMAC-signed outbound webhooks with retries/history/replay
- enterprise OIDC SSO with safe identity linking, optional/required policy, break-glass recovery and tenant-admin configuration

### System plane

- separate system-admin identity/control plane
- tenant, subscription-plan and subscription administration
- managed Stripe/Razorpay provider catalog provisioning
- billing event/history visibility and read-only provider reconciliation
- usage summaries and plan-level limits
- tenant/platform audit logs

System administrators are not tenant users with an elevated tenant role.

## Technology stack

**Backend:** Java 21, Spring Boot 4.x, Spring Security/JWT/OIDC, Spring Data JPA/Hibernate, Flyway, PostgreSQL 17, Testcontainers, AWS SDK v2 and Actuator/Micrometer.

**Frontend:** React 19, TypeScript 6, Vite 8, Material UI, React Router, TanStack React Query, Axios, React Hook Form/Zod, Vitest and Testing Library.

## Architecture

MultiTenantSAAS is intentionally a **modular monolith** with a separate web client.

```text
React / TypeScript
        ↓ HTTPS/JSON
Spring Boot modular monolith
        ↓
PostgreSQL + Flyway
        ↓
Stripe / Razorpay / Brevo / S3-compatible storage / tenant webhooks
```

The enforcement boundary is:

```text
authentication / federation
    ↓
tenant isolation
    ↓
scoped authorization
    ↓
subscription lifecycle / entitlement access
    ↓
resource and API quotas
    ↓
domain invariants
    ↓
transaction + database constraints
```

The backend is authoritative at every security and entitlement boundary.

Full architecture: `guides/current_architecture.md`.

Engineering rules and the current technical-debt register: `guides/ENGINEERING_STANDARDS.md`.

## Architecture rule for future development

> **New functionality must live in an explicit domain module and interact with other domains through narrow services, contracts, or events — not by injecting five more services into existing god-services.**

This rule is part of the persistent repository contract in `AGENTS.md`.

## Major completed platform foundations

- billing/catalog lifecycle with Stripe and Razorpay provider abstractions
- tenant-configurable outbound webhooks
- enterprise OIDC SSO / identity federation
- scoped authorization, bounded delegation and Explain Access
- collaboration, notifications, attachments, API keys, usage limits and auditability
- permission-aware Global Search and capability-aware Command Palette
- personal-workspace Favorites/Recently Viewed, My Work and Saved Views
- capability-aware dashboard composition over existing authorized feature contracts
- bounded Calendar / Deadline View using task-owned authorization-aware projection contracts
- task-relationship foundation with bounded parent hierarchy, cycle-safe directed dependencies and project-scoped labels
- Task Planning workspace implemented as a separate frontend feature domain
- recurring work and project-scoped task templates through the task-owned `TaskCreationPort`
- tenant-scoped project templates through a project-owned `ProjectCreationPort` plus `TaskCreationPort` starter-task snapshots
- standalone Work Automation & Templates workspace under explicit frontend feature domains

Stripe is the validated deployed Test Mode payment path. Razorpay application/catalog integration remains implemented while recurring Test Mode authorization is provider-sandbox blocked.

## Database

Production schema evolution is owned by Flyway. Shared PostgreSQL migrations currently extend through **V49**. Never rewrite an applied migration; new persistence begins at V50+.

```text
multitenant-saas/src/main/resources/db/migration    historical H2 migrations
multitenant-saas/src/main/resources/db/postgresql  PostgreSQL baseline/current migrations
multitenant-saas/src/main/resources/db/common      portable shared migrations
```

Recent product migrations include V45 for personal-workspace favorites/recent items, V46 for saved views, V47 for task parent/dependency/label relationships, V48 for recurring task definitions/occurrences plus project task templates, and V49 for tenant project templates with bounded starter-task snapshots.

## Verification

GitHub Actions is the authoritative automated validation path. Applicable gates cover:

- repository hygiene
- backend Maven verification
- PostgreSQL/Flyway integration validation
- frontend formatting/tests/coverage/lint/build
- security scanning
- container validation
- Qodana/static analysis
- Wiki source validation when Wiki files change

## Deployment

- Frontend: `https://multitenantsaas-frontend.onrender.com`
- Backend: `https://multitenantsaas-akxn.onrender.com`
- Production profile: `SPRING_PROFILES_ACTIVE=postgres,production`

Use `.env.production.example` as the deployment-variable inventory. Never commit real keys or secrets.

## Documentation

Documentation ownership is deliberately narrow to prevent drift:

- `CHECKPOINT.md` — current repository/application status
- `HANDOFF.md` — current resume instructions and next action
- `AGENTS.md` — persistent development/quality contract
- `guides/README.md` — documentation index and ownership policy
- `guides/current_architecture.md` — canonical technical architecture
- `guides/ENGINEERING_STANDARDS.md` — technical health, debt and engineering rules
- `guides/task_relationships.md` — task hierarchy/dependency/label semantics and ownership
- `guides/recurring_work_and_templates.md` — recurring work, task-template and project-template generation contract
- focused guides — domain-specific behavior
- `guides/Wild_Thoughts.md` — exploratory idea vault, not a committed roadmap
- `wiki/*.md` — canonical source for the published reader-facing Wiki
- `wiki/Roadmap.md` — product direction and deferred milestones

The Wiki is automatically validated and published from merged `main` by `.github/workflows/wiki-sync.yml` using `scripts/publish-wiki.ps1`.

## Current product direction

The active phase is **Product Experience & Work Management Enrichment**. Search, Command Palette, Favorites/Recently Viewed, My Work, Saved Views, Dashboard, Calendar/Deadline View, Task Planning, recurring work, task templates, tenant-scoped project templates, and the Work Automation & Templates workspace are established. The next product slice is bulk actions + CSV import/export, followed by tenant adaptability and analytics.

Production Operations & Disaster Recovery remains an important deferred milestone rather than the immediate development focus.
