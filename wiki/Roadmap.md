# Roadmap

## Current

The platform foundation, PostgreSQL path, transaction/concurrency hardening, production-oriented deployment configuration, observability foundation and Wiki publication flow are in place.

Recent completed product/platform milestones include:

- authentication and browser-session hardening
- project Kanban/task collaboration workspace
- task comments, mentions and activity
- one-level replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments and cleanup hardening
- notification persistence
- durable notification delivery records/outbox processing
- email notification delivery
- in-app notification center
- task assignment and task-status notifications
- comment/reply/mention notifications
- precise task/comment/reply notification deep links
- per-event optional email preferences
- project membership notifications

The current phase is **product expansion + production/platform completion**.

The current short checkpoint is:

1. synchronize repository/Wiki documentation through PR #65
2. add a cross-module critical tenant integration journey

After that, **external billing** becomes the active platform milestone.

## Near term

1. complete the post-PR #65 documentation and critical-journey checkpoint
2. add an external billing provider boundary without replacing the existing subscription/entitlement domain
3. add signed billing webhooks, replay protection, durable provider-event handling and reconciliation
4. add durable usage metering/accounting
5. add database backup/restore procedure, recovery drill, alerts and operational runbooks
6. add tenant integration capabilities such as outbound webhooks and API keys/service accounts
7. perform focused load/failure-recovery and production R2 operational verification

## Platform backlog

- external payment-provider integration and reconciliation
- durable usage metering/accounting
- tenant-configurable outbound webhooks
- API keys and service accounts
- backup/restore drills and operational alerting/runbooks
- enterprise SSO / federated authentication
- advanced authorization delegation and explain-access

## Notification status

Do not list the following as deferred notification foundation work anymore:

- PostgreSQL-backed notification delivery records/outbox processing
- email notification delivery integration
- in-app notification persistence/read state
- task assignment/reassignment notifications
- task status/cancellation notifications
- comment/reply/mention notifications
- precise task/comment/reply deep links
- per-event email preferences
- project membership notifications

Remaining notification work is optional expansion such as workspace-invitation event wiring, digests, live browser delivery and admin/delivery observability.

## Testing direction

The critical journey added in the current checkpoint uses the existing Spring integration stack to protect cross-module wiring. A dedicated browser E2E runner remains a future testing-infrastructure decision rather than a prerequisite for billing.

## Engineering rules

New work must continue to preserve:

- tenant isolation
- backend-authoritative authorization
- subscription/quota enforcement
- stable API contracts
- Flyway migration invariants
- database-backed concurrency correctness
- auditability for sensitive mutations
- explicit provider boundaries for external integrations
