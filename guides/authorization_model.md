# Authorization model

Authentication, tenant isolation, authorization, subscription access, and quota enforcement are separate concerns in this project.

## Authentication

Tenant users authenticate through tenant-scoped authentication endpoints and receive JWT-based credentials.

The authentication area also includes refresh/logout/current-user/password operations and account-lockout handling.

System administrators use a separate system-admin identity/control plane.

## Tenant isolation

Every tenant-scoped business operation must be bound to the tenant identified by the request/security context and must not read or mutate another tenant's records.

Tenant isolation is a mandatory boundary even when a caller would otherwise possess a powerful role.

## Authorization layers

The current authorization implementation supports a permission-oriented model rather than relying only on coarse fixed application roles.

The domain includes:

- a permission catalog
- tenant authorization roles
- role-permission assignments
- subject/user role assignments
- authorization policies/rules

Controllers/services may also use Spring method-security checks such as `@PreAuthorize` where appropriate.

## Tenant administrator helpers

Legacy/coarse tenant-admin checks can still exist for operations that intentionally require tenant administration. They should not replace the more granular permission model for newly permissionized business capabilities.

## Subscription is not authorization

A user may be fully authorized for an operation and still be unable to execute a growth mutation because the workspace is read-only or a quota is exhausted.

Likewise, subscription access must never grant a permission the user does not have.

The expected order is conceptually:

```text
authenticated?
  -> correct tenant?
    -> authorized?
      -> workspace mutation allowed?
        -> resource quota available?
```

## Read-only recovery operations

The central subscription interceptor blocks ordinary business mutations when the workspace is read-only.

Selected cleanup/recovery operations use the dedicated read-only-allowed annotation so customers can reduce usage or recover the workspace without first being able to grow it.

Examples include operations such as deactivation/revocation/archive flows where explicitly annotated in the current code.

## Error-contract distinction

Two common subscription-related failures should remain distinguishable:

### Workspace lifecycle restriction

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

### Resource quota restriction

Examples include:

```text
restriction = USER_LIMIT_REACHED
resource = users
```

or:

```text
restriction = PROJECT_LIMIT_REACHED
resource = projects
```

Tests should assert the contract produced by the layer that actually intercepts the request, rather than an older service-level contract that is no longer reached.
