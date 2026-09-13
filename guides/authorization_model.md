# Authorization model

Authentication, tenant isolation, authorization, subscription access and quotas are separate enforcement layers.

## Core model

Authorization is permission-oriented and supports:

- permission catalogue
- authorization roles and role-to-permission mapping
- user role assignments with validity windows
- tenant, project, organization, subtree, direct-report and self scopes
- relationship-aware evaluation
- bounded delegated assignments with durable provenance
- structured access decisions for Explain Access

Tenant isolation is evaluated independently. Powerful permissions never authorize cross-tenant access.

## Evaluation pipeline

```text
authenticated identity
→ tenant boundary
→ effective direct/delegated assignment
→ assignment validity
→ permission
→ target scope / relationship
→ delegation source validity when applicable
→ subscription lifecycle
→ quota / domain invariant
```

The backend evaluator is authoritative. Frontend route/action filtering is UX only.

## Explain Access

PR #121 refactored authorization evaluation so enforcement can return a structured decision while `hasPermission()` remains a boolean projection of that same decision path.

`POST /api/tenants/{tenantId}/authorization/explain-access` is available to `authorization.manage` and explains a requested subject/permission/context without enumerating unrelated grants.

A granted result can include the matched role assignment/scope. PR #125 additionally identifies the matched grant source as:

```text
DIRECT
DELEGATED
```

Delegated explanations can include the delegation ID, parent assignment ID and delegator identity required for administrative provenance.

## Delegation model

V44 adds `authorization.delegate` and durable `authorization_delegations` provenance.

Lifecycle endpoints:

```text
POST  /api/tenants/{tenantId}/authorization/delegations
GET   /api/tenants/{tenantId}/authorization/delegations
PATCH /api/tenants/{tenantId}/authorization/delegations/{delegationId}/revoke
GET   /api/tenants/{tenantId}/authorization/delegations/reference-data
```

Create/list/revoke requires `authorization.delegate` or `authorization.manage`. Explain Access remains an authorization-management diagnostic.

### Non-escalation invariant

```text
delegated authority ⊆ delegator's current direct authority
```

Every delegation points to one explicit direct parent assignment. Creation validates:

- the parent assignment is direct and effective
- delegated role permissions are a subset of parent permissions
- delegated scope is contained by the parent scope
- delegated validity is explicit and cannot outlive the parent
- protected permissions `authorization.manage` and `authorization.delegate` are not delegated
- delegated assignments cannot become delegation sources
- unsupported source/child scope combinations are rejected rather than widened

### Runtime source revalidation

The generated child assignment is not trusted in isolation. The evaluator resolves delegation provenance and revalidates the parent source during access evaluation.

If the parent is revoked, expired, inactive, loses the requested permission or no longer covers the requested scope, the delegated access fails. `DELEGATION_SOURCE_UNAVAILABLE` is the stable denial boundary for an unavailable source.

Revocation deactivates the generated delegated assignment.

## Delegation-safe reference data

PR #125 adds a safe reference-data endpoint so actors with `authorization.delegate` do not need `authorization.manage` merely to populate the delegation form. It returns only data needed to create a bounded delegation, including direct parent authority choices and safe child roles.

The backend still validates every submitted delegation; UI filtering is not a security boundary.

## Authorization workspace

Managers with `authorization.manage` can use Management, Delegations and Explain Access.

Actors with `authorization.delegate` but without `authorization.manage` can enter the Authorization workspace and use Delegations without gaining role/assignment administration or Explain Access.

## Authorization vs subscription

Authorization answers:

> Is this principal permitted to perform this operation on this target?

Subscription enforcement answers:

> Is this tenant currently entitled to perform this class of mutation?

Both checks may be required and intentionally produce different error semantics.
