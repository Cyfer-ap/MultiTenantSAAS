# Authorization

## Model

Authorization is permission-oriented rather than being limited to a fixed role enum.

Core concepts include permissions, roles, role assignments, assignment validity windows, target scopes, relationship-aware evaluation, bounded delegation provenance and structured access decisions.

## Supported scope concepts

```text
TENANT
ORGANIZATIONAL_UNIT
ORGANIZATIONAL_SUBTREE
DIRECT_REPORTS
PROJECT
SELF
```

The evaluator validates both the permission and requested target relationship. Tenant isolation remains a separate mandatory boundary.

## Explain Access

PR #121 introduced a structured decision model from the same evaluator used by enforcement.

Tenant authorization managers can query:

```text
POST /api/tenants/{tenantId}/authorization/explain-access
```

The response explains whether the requested subject/permission/context is granted and can expose the matched assignment/role/scope required for diagnosis without listing unrelated grants.

PR #125 adds matched-grant source provenance:

```text
DIRECT
DELEGATED
```

For a delegated grant, Explain Access can identify the delegation, parent assignment and delegator.

## Bounded delegation

V44 adds `authorization.delegate` and durable `authorization_delegations` provenance.

Lifecycle:

```text
POST  /api/tenants/{tenantId}/authorization/delegations
GET   /api/tenants/{tenantId}/authorization/delegations
PATCH /api/tenants/{tenantId}/authorization/delegations/{delegationId}/revoke
GET   /api/tenants/{tenantId}/authorization/delegations/reference-data
```

Core invariant:

```text
delegated authority ⊆ delegator's current direct authority
```

A delegation must derive from one direct effective parent assignment. Child permissions, scope and validity must remain contained by that parent. Delegated assignments cannot be re-delegated, and `authorization.manage` / `authorization.delegate` cannot be delegated.

The evaluator revalidates delegation source authority at access time. A revoked, expired, inactive or narrowed parent invalidates the child grant even if the child assignment still exists.

Revoking the delegation deactivates its generated assignment.

## Authorization workspace

`authorization.manage` users can access Management, Delegations and Explain Access.

`authorization.delegate` users without management permission can access Delegations only. Delegation reference data is intentionally narrower than the administration reference-data surface.

Frontend filtering is not a security boundary; the backend repeats all containment and authorization checks.

## Evaluation order

```text
authenticated identity
  -> tenant boundary
  -> effective assignment
  -> permission + validity
  -> target scope / relationship
  -> delegation source revalidation if delegated
  -> subscription lifecycle access
  -> quota / domain invariant
```

## Authorization vs subscription

Authorization answers whether a principal may perform an operation on a target. Subscription enforcement answers whether the tenant is currently entitled to perform that class of mutation. Both checks may be required and intentionally have different error semantics.
