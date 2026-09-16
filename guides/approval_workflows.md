# Approval Workflows

## Purpose

Approval Workflows adds durable human review checkpoints to the existing workflow runtime without turning workflow, project or task services into approval-state owners.

The first slice is internal and project-scoped. It supports reusable approval definitions, bounded sequential stages, explicit reviewer sets, immutable request-time snapshots, approve/reject decisions, reviewer inbox/history and workflow pause/resume.

## Ownership and boundaries

The `approvals` domain owns:

- approval definitions and lifecycle,
- ordered approval stages,
- configured reviewer references,
- approval requests,
- immutable request-time stage/reviewer snapshots,
- decision provenance,
- stage progression and terminal approval/rejection state.

Cross-domain behavior is deliberately narrow:

```text
workflow runtime
    -> ApprovalCheckpointPort
    -> approvals domain

approvals domain
    -> ApprovalReviewerEligibilityPort
    -> project-owned reviewer eligibility adapter

approvals domain
    -> ApprovalResolvedEvent
    -> workflow-owned resolution listener
    -> existing workflow runtime
    -> TaskAutomationMutationPort for any downstream task mutation
```

The approvals domain does **not** inject project/task/workflow repositories as a normal integration mechanism. Approval configuration never grants resource authority.

## Persistence — V53

V53 is append-only and creates:

- `approval_definitions`
- `approval_stages`
- `approval_stage_reviewers`
- `approval_requests`
- `approval_request_stages`
- `approval_request_stage_reviewers`

V53 also extends the existing workflow schema with:

- `APPROVED` / `REJECTED` workflow edge branches,
- `WAITING_APPROVAL` workflow execution status.

Definitions are tenant/project scoped and versioned. Request rows record the definition version, workflow/version, workflow execution/node, task, original workflow actor, current stage and both continuation node keys.

Request-stage and request-reviewer rows are snapshots. Later edits to an approval definition cannot rewrite the provenance of an already-open or completed request.

## Definition model

A definition contains 1–10 ordered stages. Each stage has:

- a stable bounded key,
- a display name,
- an explicit `allowRequesterApproval` policy,
- 1–20 configured reviewer user IDs.

Lifecycle is:

```text
DRAFT -> ACTIVE -> PAUSED -> ACTIVE
```

Active definitions are not edited in place. Pause before editing. Updating a definition advances its definition version.

Configured reviewers must resolve to eligible active project members when definitions are validated. This is configuration validation only; it does not grant or permanently preserve authority.

## Reviewer authorization

A user being stored in `approval_stage_reviewers` or an immutable request snapshot is **not** sufficient to decide.

At decision time the approvals domain verifies:

1. the request is still `PENDING`,
2. the current stage is still pending,
3. the actor is in that request-stage's snapshotted reviewer set,
4. the project-owned `ApprovalReviewerEligibilityPort` says the actor is currently eligible,
5. requester self-approval is permitted by the snapshotted stage policy when actor and requester are the same user.

This means removing or disabling project membership removes decision eligibility even when the user was configured earlier.

## Decision and concurrency semantics

Decisions are `APPROVE` or `REJECT` commands.

Approval is sequential:

- approving a non-terminal stage resolves that stage and activates the next snapshotted stage,
- approving the final stage resolves the request as `APPROVED`,
- rejecting any pending stage resolves the request as `REJECTED`.

A request is locked for decision and also carries an optimistic row version. One logical pending stage cannot be successfully decided twice by racing/replayed requests. A decision against an already-resolved request is rejected rather than silently replayed.

Decision provenance retained on the stage includes:

- deciding user,
- terminal stage outcome,
- optional bounded comment,
- decision timestamp.

## Workflow integration

The visual workflow graph exposes `ACTION_REQUEST_APPROVAL` as an action node.

Its configuration is exactly one approval-definition UUID. The graph validator requires exactly two outgoing edges:

```text
APPROVED -> approved continuation
REJECTED -> rejected continuation
```

When traversal reaches the node:

1. workflow runtime opens/reuses an approval checkpoint through `ApprovalCheckpointPort`,
2. the request snapshots the current active approval definition,
3. the workflow execution becomes `WAITING_APPROVAL`,
4. traversal returns without performing downstream mutations.

When the request becomes terminal, the approvals domain publishes `ApprovalResolvedEvent`. A workflow-owned listener resumes the existing execution. The runtime verifies that the stored workflow version still matches before continuation.

The runtime takes a fresh task snapshot before resuming. Any downstream task mutation still crosses `TaskAutomationMutationPort`, which rechecks current task/project/actor authority and lifecycle rules. Approval itself therefore never authorizes the mutation.

## Internal UI

Approval UI is feature-local under `features/approvals` and is composed into the existing Work Automation & Templates workspace.

V1 provides:

- definition/stage builder,
- current-project reviewer selection using the existing project-members API,
- activate/pause controls,
- current reviewer's inbox,
- approve/reject actions with optional comments,
- project approval request history.

The workflow builder uses the currently selected project only to discover compatible active approval definitions. The workflow runtime remains owned by the workflow domain.

## Bounds and non-goals

V1 intentionally excludes:

- public/anonymous approvals,
- guest/client approval identity,
- arbitrary expressions or executable rules,
- role-expression reviewer DSLs,
- automatic escalation,
- expiry/timeouts,
- reassignment/delegation inside the approval domain,
- quorum/majority voting,
- parallel approval stages,
- approval-driven permission grants.

External/client approval belongs behind the later Client / Guest Portal authentication and abuse boundary. Expiry, cancellation, reassignment and escalation require explicit lifecycle semantics before introduction.

## Security invariants

- tenant/project scope is authoritative before exposure or action,
- approval definitions never grant project/task/workflow access,
- workflow-admin permission does not imply reviewer eligibility,
- reviewer eligibility is revalidated at decision time,
- requester self-approval is an explicit stage policy,
- stored reviewer snapshots preserve history but do not preserve authority,
- post-approval resource mutations re-enter the owning domain's authorization and lifecycle checks,
- decision history remains auditable after definition changes,
- list/inbox/history reads are bounded and project scoped.

## Validation contract

Regression coverage should lock:

- V53 migration/table/column expectations,
- definition/stage/reviewer bounds,
- approval node UUID and `APPROVED`/`REJECTED` graph contract,
- configured-but-no-longer-eligible reviewer denial,
- requester self-approval denial when disabled,
- replayed/resolved decision denial,
- workflow pause without premature task mutation,
- resume of the same workflow execution,
- downstream task mutation through `TaskAutomationMutationPort`,
- frontend project-scoped routes and reviewer decision interaction.
