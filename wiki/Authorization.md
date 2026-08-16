# Authorization

## Authorization model

The application supports a permission-oriented authorization model.

Core concepts include:

- permission catalogue
- roles
- role-permission mapping
- user/subject role assignments
- organization-aware or scoped decisions
- policy/rule evaluation

## Compatibility roles

Legacy tenant roles may remain during migration:

```text
TENANT_ADMIN
TENANT_MANAGER
TENANT_USER
```

Project-specific roles include:

```text
PROJECT_LEAD
MEMBER
```

## Evaluation order

```text
identity
-> tenant boundary
-> permission/policy
-> target scope/relationship
-> subscription lifecycle
-> quota
```

## Authorization vs subscription

Authorization answers whether a user is allowed to perform an action.

Subscription enforcement answers whether the workspace/account state permits that class of mutation.

A successful authorization decision must not bypass subscription restrictions, and a valid subscription must not grant missing authorization.

## Error semantics

Permission denials and subscription/quota restrictions should remain distinguishable in API contracts.
