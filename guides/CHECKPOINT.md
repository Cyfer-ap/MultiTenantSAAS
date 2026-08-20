# MultiTenantSAAS Documentation Checkpoint

Date: 2026-08-20
Repository: `Cyfer-ap/MultiTenantSAAS`
Base reviewed state: post-PR #65 (`0e1edd6`)

## Documentation status

The current engineering phase is **product expansion + production/platform completion**. This checkpoint replaces the post-PR #59 notification snapshot with the actual post-PR #65 platform state.

Primary documentation entry points:

- `readme.md`
- `CHECKPOINT.md`
- `guides/README.md`
- `guides/HANDOFF.md`
- `guides/DEFERRED_PLATFORM_WORK.md`
- `guides/collaboration_and_notifications.md`
- `wiki/Home.md`
- `wiki/Notifications.md`
- `wiki/Roadmap.md`

## Current feature state

Recent completed milestones include:

- authentication/security hardening
- project Kanban/task collaboration workspace
- threaded replies and pinned comments
- R2/S3-compatible task attachments and cleanup hardening
- notification persistence
- durable notification delivery records/outbox processing
- notification email delivery
- in-app notification center
- task assignment/reassignment notifications
- task status/cancellation notifications
- task comment, reply and mention notifications
- precise task/comment/reply deep links
- per-event email notification preferences
- project membership add/role-change/remove notifications

## Current migration state

The repository keeps separate historical H2 and PostgreSQL baseline histories, with portable shared migrations in `db/common`.

Current common migrations extend through **V27**:

```text
V21 task collaboration
V22 task attachments
V23 attachment cleanup hardening
V24 comment replies and pins
V25 notifications
V26 notification deliveries
V27 notification preferences
```

Never rewrite an already-applied migration.

## Hardening checkpoint

The existing focused tests already cover the individual subsystems. This checkpoint adds one cross-module critical tenant journey to protect the seams among onboarding/authentication, invitations, project membership, task assignment, collaboration, notifications, task status and session revocation.

A dedicated browser E2E runner is intentionally not introduced in this small slice; it can be added later as standalone test infrastructure if browser-level coverage becomes worth the dependency and CI cost.

## Source-of-truth rule

Use this order when documentation and implementation disagree:

1. current application code and tests
2. current Flyway migrations
3. focused current guides
4. historical workflow/planning notes

## Historical material

Older generated bundles, `progress.md`, `Plan.txt`, Step 39/40 notes and the Authorization V2 plan remain useful as implementation history, but they are not authoritative statements of the current project phase.

## Current backlog summary

Immediate:

- complete this documentation/critical-journey checkpoint
- begin external billing integration

Platform follow-up:

- usage metering/accounting
- tenant webhooks
- API keys/service accounts
- recovery/alerting/runbooks
- enterprise SSO
- advanced authorization delegation/explain-access
- broader load/failure-recovery and production R2 verification
