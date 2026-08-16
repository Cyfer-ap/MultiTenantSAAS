# Frontend

## Stack

- React 19
- TypeScript
- Vite
- Material UI
- React Router
- current state/data-access tooling used by the repository
- Vitest
- Testing Library

## Responsibilities

The frontend handles:

- authentication UX
- protected routes
- tenant and system shells
- dashboards
- users
- invitations
- projects
- tasks
- audit logs
- subscription state presentation

## Security rule

Route hiding is not authorization.

The backend must reject unauthorized access independently.

## Session behavior

Tenant and system-admin session stores remain isolated.

Tenant flows include access-token refresh and session restoration.

## Errors

The UI should distinguish:

- authentication failure
- authorization failure
- validation failure
- conflict/business rule
- subscription restriction
- network/backend availability
- unexpected failure

## Production API configuration

Use `VITE_API_BASE_URL` rather than hard-coding backend URLs.
