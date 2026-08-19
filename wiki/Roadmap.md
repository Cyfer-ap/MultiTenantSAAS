# Roadmap

## Current

The platform foundation, PostgreSQL path, transaction/concurrency hardening, staging/deployment configuration, observability foundation and Wiki publication flow are in place.

Recent completed product/platform milestones include:

- authentication and browser-session hardening
- project Kanban/task collaboration workspace
- task comments, mentions and activity
- one-level replies and pinned comments
- Cloudflare R2 / S3-compatible task attachments
- notification persistence
- durable notification delivery records/outbox processing
- email notification delivery
- in-app task-assignment notifications

The current phase is **product expansion + production/platform completion**.

## Near term

1. keep repository guides and Wiki source synchronized
2. expand notifications to comments, replies, mentions and task-status/collaboration events
3. improve notification deep links and add user/channel preferences
4. continue targeted collaboration UX where it creates clear product value
5. add database backup/restore procedure and recovery drill
6. add external monitoring/alerting and operational runbooks
7. perform focused load/failure-recovery verification

## Platform backlog

- external payment-provider integration
- signed provider webhook reconciliation/replay protection
- durable usage metering/accounting
- tenant-configurable outbound webhooks
- API keys and service accounts
- enterprise SSO / federated authentication
- advanced authorization delegation and explain-access

## Already delivered platform foundations

Do not list these as generic deferred work anymore:

- PostgreSQL-backed notification delivery records/outbox processing
- email notification delivery integration
- in-app notification persistence/read state

## Engineering rules

New work must continue to preserve:

- tenant isolation
- backend-authoritative authorization
- subscription/quota enforcement
- stable API contracts
- Flyway migration invariants
- database-backed concurrency correctness
- auditability for sensitive mutations
