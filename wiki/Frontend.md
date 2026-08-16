# Frontend

## Stack

Current frontend dependencies include:

- React 19.2
- TypeScript 6
- Vite 8
- Material UI 9
- React Router 7
- TanStack React Query
- Axios
- React Hook Form
- Zod
- Vitest
- Testing Library

## Responsibilities

The frontend provides:

- tenant and system-admin authentication UX
- protected route shells
- dashboards
- tenant/system administration screens
- users and invitations
- organization/authorization views
- projects and tasks
- audit logs
- subscription state presentation
- normalized API-error handling

## Route loading

Major routed pages and application shells are lazy-loaded at route boundaries. This reduces the initial JavaScript bundle and keeps route-level functionality split into smaller chunks.

## Security rule

Route hiding, button hiding and client-side role checks are UX controls only.

The backend must independently enforce:

```text
authentication
tenant isolation
authorization
subscription access
quota/domain rules
```

## Session behavior

Tenant and system-admin session stores are separate.

Tenant flows include:

- access token usage
- refresh-token rotation
- session restoration
- logout
- logout-all invalidation

## API errors and support references

The API client normalizes backend failures into `ApiClientError`.

When the backend returns `X-Request-ID`, the client preserves it as `requestId`, allowing an unexpected UI error to be correlated with backend logs.

See [[API-and-Errors]] and [[Operations-and-Observability]].

## Configuration

Do not hard-code backend URLs. Use:

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

or the corresponding deployed API URL.
