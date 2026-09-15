# Collaborative Whiteboard

This guide defines the persisted backend contract for committed differentiated feature #3: **Collaborative Whiteboard**.

## Ownership

Whiteboards live in the explicit backend domain:

```text
com.chacha.multitenantsaas.whiteboards
```

They are project-scoped planning documents. The whiteboard domain owns board metadata, visual nodes, connectors and node-to-task links. It does **not** own projects or tasks.

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

A board has an optimistic `version`. Clients must send the version they edited when updating, deleting or converting a node to a task. A stale version returns an HTTP `409 Conflict` rather than silently overwriting newer work.

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

Foundation endpoints:

```text
GET    /
POST   /
GET    /{boardId}
PUT    /{boardId}
DELETE /{boardId}?expectedVersion={version}
POST   /{boardId}/nodes/{nodeKey}/convert-to-task
```

Reads currently reuse project task-read authorization semantics. Whiteboard mutations and task conversion reuse project task-manage semantics until a dedicated product permission is justified.

Archived projects remain readable but reject board creation/update/delete and node-to-task conversion.

## Document replacement

The foundation API persists a board as one bounded document. `PUT` replaces its node/connector snapshot atomically after validating the submitted version.

Replacement order is explicit:

1. delete connectors and flush
2. capture/preserve task links by stable node key
3. delete nodes and flush
4. recreate validated nodes
5. recreate validated connectors

This keeps composite foreign keys deterministic and gives the future canvas one clear autosave contract.

## Node -> task conversion

Conversion never writes `project_tasks` directly.

```text
whiteboard node
    -> whiteboard service
    -> TaskCreationPort
    -> task-owned adapter
    -> ordinary task lifecycle
```

The task-owned adapter remains responsible for project lifecycle, creator/assignee validation, project membership, task activity, audit logging, assignment notification, outbound webhooks and task-domain events.

Whiteboard conversion adds only whiteboard-specific intent and stores the returned task ID on the source node.

## Collaboration transport

**V51 does not define WebSocket/STOMP state.**

The persisted board/document model must remain independent from the live collaboration transport. The later collaboration PR may add presence, cursors and real-time document updates, but those capabilities should operate against this versioned document contract rather than becoming persistence primitives themselves.

## Planned follow-up

The next whiteboard slice should add the project-facing visual workspace:

- board selector/create/delete
- draggable/resizable sticky, text and shape nodes
- connectors
- zoom/pan
- autosave against optimistic versions
- multi-select
- local undo/redo
- project page entry point
- node -> task conversion UX

After the persisted UI is stable, add live presence/cursors and conflict/resync transport as a separate collaboration slice.
