# Client / Guest Portal

## Purpose

Client / Guest Portal introduces bounded external collaboration without converting clients or guests into tenant members.

The portal has three completed slices: the external-access security/read boundary, bounded create-only guest task comments, and request-scoped external approval review.

## Ownership and boundaries

The explicit `externalaccess` domain owns:

- external access grants,
- one-time invitation credentials,
- grant capabilities,
- guest sessions,
- grant/session validation,
- guest-facing orchestration.

It does not own project or task persistence.

Current composition:

```text
tenant project manager
    -> externalaccess grant management
    -> one-time invitation token (raw value returned once)
    -> persisted SHA-256 token hash only

guest
    -> /api/public/guest-portal/exchange
    -> hashed guest session
    -> externalaccess capability validation
    -> ExternalProjectProjectionPort / ExternalTaskProjectionPort
    -> project/task-owned adapters

guest comment mutation
    -> externalaccess capability/session validation
    -> ExternalTaskCommentPort
    -> task-collaboration-owned adapter

guest approval review
    -> externalaccess session/capability validation
    -> ExternalApprovalReviewPort
    -> approvals-owned request/stage decision behavior

approval reviewer assignment
    -> approvals-owned ApprovalExternalGrantPort
    -> externalaccess-owned active grant/capability validation
```

Guests are not `AppUser` records, project members, tenant RBAC subjects or API-key identities. Guest session credentials are only accepted by the isolated `/api/public/guest-portal/**` surface.

## V54 persistence

V54 is append-only and creates:

- `external_access_grants`
- `external_access_grant_capabilities`
- `external_guest_sessions`

Raw invitation and session tokens are never persisted. Only SHA-256 hashes are stored. Invitation tokens are one-time exchange credentials.

A session is bounded by the grant expiry. Every guest request re-resolves both the session and the grant, so grant revocation/expiry invalidates a retained browser session immediately on its next request.

## Capabilities

The current capability set is:

- `PROJECT_READ`
- `TASK_READ`
- `TASK_COMMENT_CREATE`
- `APPROVAL_REVIEW`

Every grant must include `PROJECT_READ`. `TASK_READ` is optional. `TASK_COMMENT_CREATE` is explicit and requires `TASK_READ`. `APPROVAL_REVIEW` is independent of task visibility: an external approver does not need `TASK_READ` and therefore need not receive the shared task list.

The capability set is intentionally tiny and typed; it is not a general permission DSL and does not map to tenant RBAC assignments.

## Internal management API

Project members with `project.member.manage` can:

- create a bounded grant,
- list grants for the project,
- revoke a grant.

Project members with the existing task-management authority used by Approval Workflows can also assign or remove an active `APPROVAL_REVIEW` grant on the **current pending approval stage**. Assignment snapshots the guest identity for provenance; it does not make the guest a project member or general approval reviewer.

Grant creation returns the raw invitation token once. The stored grant retains guest identity metadata and creator/revoker provenance.

Default maximum grant lifetime is 90 days. Archived projects cannot create new grants.

## Public guest API

Public routes are restricted to:

```text
POST /api/public/guest-portal/exchange
GET  /api/public/guest-portal/session
GET  /api/public/guest-portal/tasks
GET  /api/public/guest-portal/tasks/{taskId}/comments
POST /api/public/guest-portal/tasks/{taskId}/comments
GET  /api/public/guest-portal/approvals
POST /api/public/guest-portal/approvals/{requestId}/decision
```

Authenticated guest reads use the dedicated `X-Guest-Session` header rather than JWT or the normal `Authorization` bearer channel.

The public prefix is admitted by Spring Security only so the external-access domain can authenticate the guest session itself. Normal tenant APIs remain authenticated and do not recognize guest sessions.

Public guest routes share a bounded IP rate-limit bucket. Invalid, expired, revoked and replayed invitation/session states use generic authentication failures rather than revealing whether a grant exists.

## Project/task read boundary

Guest-facing project/task data crosses narrow owning-domain ports:

```text
externalaccess
    -> ExternalProjectProjectionPort
    -> project-owned adapter

externalaccess
    -> ExternalTaskProjectionPort
    -> task-owned adapter
```

Guest-supplied tenant/project IDs do not exist in the public API. Tenant/project scope comes only from the authenticated grant/session context.

Task reads are bounded to at most 100 tasks in this slice.

## V55 guest task comments

V55 extends the existing `task_comments` table rather than creating a shadow external-comment store.

Each comment has an explicit author type:

- `TENANT_USER`
- `EXTERNAL_GUEST`

External guest comments store the exact external-access grant id plus guest name/email snapshots. A scoped foreign key binds the provenance grant to the same tenant/project.

The `externalaccess` domain never writes task-comment persistence directly. It crosses `ExternalTaskCommentPort`; the task-collaboration-owned adapter independently rebinds tenant/project/task and lifecycle state before reading or writing.

Guest comments are deliberately limited to top-level, create-only comments. Guest-side history is grant-scoped and loaded on demand per task. Project members see the same records in the ordinary task thread with an explicit Guest label.

Tenant mutation paths reject editing, deletion, pinning/unpinning and threaded replies for external guest comments. Attachments and mentions are also rejected/excluded in this slice.

## V56 request-scoped external approvals

V56 extends the external-access capability constraint with `APPROVAL_REVIEW`, creates `approval_request_stage_external_reviewers`, and extends `approval_request_stages` with explicit decision-actor provenance.

An external reviewer must satisfy **both** boundaries:

1. the guest session resolves to an active, unexpired, unrevoked grant with `APPROVAL_REVIEW`;
2. that exact grant is explicitly assigned to the request's current pending stage in the same tenant/project.

The guest cannot select tenant/project scope. The public decision route receives only the request id; the approvals domain rebinds it to the grant-bound tenant/project and current stage, then requires the exact assignment before mutation.

External decisions use the same approval request lock/current-stage/replay behavior as internal decisions and continue the same workflow execution through the existing typed `APPROVED` / `REJECTED` resolution event. The stage records `EXTERNAL_GUEST`, the exact grant id, and immutable guest name/email snapshots; no synthetic `AppUser` is created.

The internal Approval Workflows history UI can assign/remove eligible approval-capable grants. The standalone guest UI lists only assigned pending reviews and provides bounded approve/reject actions with an optional 1000-character comment.

## Deliberately excluded from the current portal

Not yet exposed:

- guest comment editing/deleting/threaded replies,
- guest comment mentions or attachments,
- attachments/downloads,
- arbitrary search,
- user/member directory,
- tenant navigation,
- admin/billing/workflow configuration,
- public forms,
- guest-created tasks,
- broad tenant/project membership.

## Validation contract

Regression coverage should lock:

- V54/V55/V56 migration/table/column expectations,
- raw token non-persistence,
- one-time invitation exchange,
- grant expiry/revocation invalidating retained sessions,
- mandatory `PROJECT_READ`,
- capability denial before project/task/comment adapters are called,
- `TASK_COMMENT_CREATE` requiring `TASK_READ`,
- `APPROVAL_REVIEW` remaining independent of `TASK_READ`,
- guest comment grant/project/task rebinding and immutable guest/grant provenance,
- external approval exact tenant/project/request/stage/grant assignment,
- revoked/expired or unassigned approval grants failing before decision,
- replay/completed approval decisions failing deterministically,
- immutable `EXTERNAL_GUEST` decision provenance,
- grant-bound tenant/project scope,
- public guest rate limiting,
- normal tenant API authentication remaining unchanged.
