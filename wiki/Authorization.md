# Authorization

## Model

Authorization is permission-oriented rather than being limited to a single fixed role enum.

Core concepts include:

- permission catalogue
- authorization roles
- role-to-permission mapping
- user role assignments
- assignment validity windows
- target scopes
- relationship-aware evaluation
- policy/rule evaluation

## Supported scope concepts

Authorization assignments can target bounded scope types such as:

```text
TENANT
ORGANIZATIONAL_UNIT
ORGANIZATIONAL_SUBTREE
DIRECT_REPORTS
PROJECT
SELF
```

The evaluator must validate both the permission and the requested target relationship.

## Compatibility roles

Tenant compatibility roles remain available where required:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Project membership roles include:

```text
PROJECT_LEAD
MEMBER
```

These role names are not substitutes for tenant isolation.

## Evaluation order

A useful mental model is:

```text
authenticated identity
  -> tenant boundary
  -> permission / role assignment
  -> assignment validity
  -> target scope / relationship
  -> subscription lifecycle access
  -> quota / domain invariant
```

## Authorization vs subscription

Authorization answers:

> Is this principal permitted to perform this operation on this target?

Subscription enforcement answers:

> Is this tenant currently entitled to perform this class of mutation?

Both checks may be required. They intentionally produce different error semantics.

## Frontend rule

The frontend may hide routes/actions for usability, but backend security and authorization remain authoritative.
