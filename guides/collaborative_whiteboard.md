# Collaborative Whiteboard

This guide defines committed differentiated feature #3: **Collaborative Whiteboard**. PR #146 established the persisted backend contract; PR #147 adds the project-facing visual workspace on top of that contract.

## Ownership

Whiteboards live in the explicit backend domain:

```text
com.chacha.multitenantsaas.whiteboards
```

Frontend ownership is localized under:

```text
multitenant-saas-frontend/src/features/whiteboards/
```

Whiteboards are project-scoped planning documents. The whiteboard domain owns board metadata, visual nodes, connectors and node-to-task links. It does **not** own projects or tasks.

Cross-domain boundaries:

```text
whiteboards
    -> ProjectAccessPort -> project-owned adapter -> project existence/lifecycle
    -> TaskCreationPort  -> task-owned adapter    -> real task creation
```

The whiteboard domain must not inject `ProjectService`, `ProjectTaskService`, `ProjectRepository` or `ProjectTaskRepository`.

## V51 persistence

Portable Flyway migration **V51** creates:

- `whiteboards`
- `whiteboard_nodes`
- `whiteboard_edges`

`whiteboards` is scoped by `(tenant_id, project_id)` and has a unique normalized board name within a project.

A board has an optimistic `version`. Clients must send the version they edited when updating, deleting or converting a node to a task. A stale version returns HTTP `409 Conflict` rather than silently overwriting newer work.

### Nodes

Initial node types:

- `STICKY`
- `TEXT`
- `SHAPE`

Every node has a stable `node_key`, content, position, width, height and z-index. Geometry is bounded in both validation and database constraints.

A node may store `linked_task_id`. Only sticky/text nodes are convertible to tasks. Once linked, a node cannot be converted a second time. The link is preserved across full-document updates when the same stable node key remains.

### Connectors

Connectors reference stable source/target node keys. Self-connectors and duplicate directed connectors are rejected.

Unlike task dependencies, a whiteboard is a visual planning surface, so **cycles are valid**. The whiteboard validator must not impose task-DAG semantics on visual connectors.

## Bounds

A board document is intentionally bounded:

- at most **300 nodes**
- at most **600 connectors**
- node key: up to 64 safe identifier characters
- node content: up to 4,000 characters
- connector label: up to 500 characters

These limits protect request size, persistence replacement and future collaboration fan-out.

## API

Under:

```text
/api/tenants/{tenantId}/projects/{projectId}/whiteboards
```

Endpoints:

```text
GET    /
POST   /
GET    /{boardId}
PUT    /{boardId}
DELETE /{boardId}?expectedVersion={version}
POST   /{boardId}/nodes/{nodeKey}/convert-to-task
```

Reads reuse project task-read authorization semantics. Whiteboard mutations and task conversion reuse project task-manage semantics. Exact project-lead membership remains a valid resource-level management relationship through the existing authorization model.

Archived projects remain readable but reject board creation/update/delete and node-to-task conversion.

## Document replacement and optimistic concurrency

The API persists a board as one bounded document. `PUT` replaces its node/connector snapshot atomically after validating the submitted version.

Replacement order is explicit:

1. delete connectors and flush
2. capture/preserve task links by stable node key
3. delete nodes and flush
4. recreate validated nodes
5. recreate validated connectors

The frontend keeps the currently loaded server version as its save token. Committed edits—adding/deleting nodes, drag/resize completion, connector changes, inspector edits, undo and redo—persist with that expected version. Successful saves advance the local version. A rejected stale save exposes an explicit **Reload board** action rather than silently rebasing local state.

The UI deliberately does not send a network request on every pointer-move event.

## Visual workspace

Private project route:

```text
/projects/:projectId/whiteboards
```

Project details expose an **Open whiteboard** entry action through a small whiteboard-owned wrapper rather than adding more responsibilities directly to the legacy project-details page.

The workspace provides:

- project-scoped board selector
- create/delete board
- sticky, text and shape creation
- drag and resize
- directed connectors
- pan and zoom
- Ctrl/Cmd multi-select
- local undo/redo history
- board/node inspector
- read-only rendering for users without edit authority and for archived projects
- version visibility and explicit conflict reload
- node -> task conversion UX

The editor avoids server-to-local `useEffect` synchronization. A selected board mounts the editor from its authoritative server snapshot; deliberate board switches/reloads remount it at a new editor epoch. This keeps the local interaction state independent from query cache refreshes while preserving the optimistic-version boundary.

## Node -> task conversion

Conversion never writes `project_tasks` directly.

```text
whiteboard node
    -> whiteboard API
    -> TaskCreationPort
    -> task-owned adapter
    -> ordinary task lifecycle
```

The task-owned adapter remains responsible for project lifecycle, creator/assignee validation, project membership, task activity, audit logging, assignment notification, outbound webhooks and task-domain events.

The conversion dialog supports:

- task title
- description
- priority
- active project-member assignee
- due date

After success, the returned task ID is attached to the node and project-task query caches are invalidated through the task domain's exported query-key contract.

## Collaboration transport

**V51 and PR #147 do not make WebSocket/STOMP state a persistence primitive.**

The persisted board/document model remains independent from the live collaboration transport. Presence, cursors and real-time document synchronization may be added later as a separate collaboration enhancement operating against this versioned document contract.

Those live-multiplayer enhancements are not required to begin the next committed differentiated product feature after #147 is merged green.
