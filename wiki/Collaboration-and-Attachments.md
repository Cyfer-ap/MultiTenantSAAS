# Collaboration and Attachments

Task collaboration is a tenant-, project- and task-scoped product area. Authorization and tenant isolation remain backend authoritative for every collaboration operation.

## Implemented collaboration features

- comments
- mentions
- activity history
- one-level replies
- pinned comments
- task-level and comment-linked attachments

Relevant migrations:

```text
V21__create_task_collaboration.sql
V22__create_task_attachments.sql
V23__harden_task_attachment_cleanup.sql
V24__add_comment_threads_and_pins.sql
```

## Reply model

Replies are intentionally one level deep. This keeps API, query and UI behavior predictable while still supporting conversational task threads.

Do not convert the model to arbitrary-depth recursion without explicitly revisiting schema/query/API/UI behavior.

## Attachment storage

Attachments use an S3-compatible object-storage abstraction backed by AWS SDK v2 and designed for Cloudflare R2.

Typical upload flow:

```text
client requests upload authorization
        ↓
backend validates tenant/project/task/comment scope
        ↓
backend returns presigned upload URL
        ↓
client uploads directly to object storage
        ↓
client completes attachment flow
        ↓
backend verifies and records durable metadata
```

Download and delete operations repeat scope/authorization validation. Object keys are never treated as authorization credentials.

## Storage configuration

Storage is environment controlled. R2 endpoint, bucket and credentials must remain outside committed source.

## Lifecycle hardening

Attachment cleanup behavior is designed so database metadata and object-storage state do not silently drift during failed or deleted attachment flows.

## Related pages

- [[Projects-and-Tasks]]
- [[Notifications]]
- [[Authorization]]
- [[Tenancy-and-Data-Model]]
