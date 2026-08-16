# MultiTenantSAAS Wiki

This wiki documents the architecture and development state of `Cyfer-ap/MultiTenantSAAS` at commit `3808c0ddf95d075aed7114bf060518640c19d6c2`.

## Platform at a glance

MultiTenantSAAS is a full-stack SaaS application with:

- shared-database/shared-schema tenant isolation
- tenant authentication and system-admin authentication
- permission-oriented authorization
- organization/scoped-access foundations
- projects, memberships, and tasks
- invitation lifecycle
- tenant/platform dashboards
- audit logs
- subscription plans, lifecycle, access evaluation, and quotas
- central workspace read-only enforcement
- PostgreSQL 17 runtime path and Flyway baseline strategy
- production Spring profile and production environment template
- active transaction/concurrency hardening

## Architecture map

```text
Browser
  |
  v
React / Vite frontend
  |
  v
Spring Security
  |
  v
Controllers
  |
  v
Authorization + subscription enforcement
  |
  v
Transactional services
  |
  v
Tenant-scoped repositories
  |
  v
PostgreSQL / H2 test path
```

## Wiki navigation

- [[Architecture]]
- [[Security-and-Authentication]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
- [[Projects-and-Tasks]]
- [[Subscriptions-and-Quotas]]
- [[PostgreSQL-and-Flyway]]
- [[Transaction-and-Concurrency]]
- [[Frontend]]
- [[Testing-and-CI]]
- [[Production-Deployment]]
- [[Roadmap]]
- [[Developer-Handoff]]

## Current milestone

The active engineering milestone is **Step 40 — Transaction & Concurrency Hardening**.

The latest `main` additionally includes the production configuration foundation.
