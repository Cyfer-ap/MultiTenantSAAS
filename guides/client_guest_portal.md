# Client / Guest Portal

## Purpose

Client / Guest Portal introduces bounded external collaboration without converting clients or guests into tenant members.

The portal now has two completed slices: the external-access security/read boundary and bounded create-only guest task comments. External approval remains the next slice.

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

Every grant must include `PROJECT_READ`. `TASK_READ` is optional. `TASK_COMMENT_CREATE` is explicit and requires `TASK_READ`.

The capability set is intentionally tiny and typed; it is not a general permission DSL and does not map to tenant RBAC assignments.

## Internal management API

Project members with `project.member.manage` can:

- create a bounded grant,
- list grants for the project,
- revoke a grant.

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

## Deliberately excluded from the current portal

Not yet exposed:

- external approval decisions,
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

External approval must be added only through an approval-owned narrow contract that intersects an active grant with the exact request/stage scope.

## Validation contract

Regression coverage should lock:

- V54/V55 migration/table/column expectations,
- raw token non-persistence,
- one-time invitation exchange,
- grant expiry/revocation invalidating retained sessions,
- mandatory `PROJECT_READ`,
- capability denial before project/task/comment adapters are called,
- `TASK_COMMENT_CREATE` requiring `TASK_READ`,
- guest comment grant/project/task rebinding and immutable guest/grant provenance,
- grant-bound tenant/project scope,
- public guest rate limiting,
- normal tenant API authentication remaining unchanged.
