# Authorization model

Authentication, tenant isolation, authorization, subscription access, and quotas are separate.

## Authentication

Tenant users receive JWT credentials and use refresh/session/password flows.

System administrators use a separate security plane.

## Tenant isolation

Every tenant operation must be bound to the active tenant context. Powerful permissions never authorize cross-tenant data access.

## Permission-oriented authorization

The model supports permissions, roles, assignments, and policies rather than relying only on coarse fixed roles.

## Subscription is not authorization

A caller may be authorized but still blocked because the workspace is read-only or a quota is exhausted.

Conceptually:

```text
authenticated?
-> correct tenant?
-> authorized?
-> workspace mutation allowed?
-> quota available?
```

## Error-contract distinction

Workspace lifecycle restriction:

```text
HTTP 409
restriction = WORKSPACE_READ_ONLY
resource = workspace
```

Resource quota restrictions remain resource specific.
