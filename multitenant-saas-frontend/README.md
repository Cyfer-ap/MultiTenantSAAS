# Multi-Tenant SaaS Frontend

React frontend for the Multi-Tenant SaaS platform.

```text
Repository baseline reviewed:
ae1fa4cbb5133ae0b3bcd2596379e1ab64f36be1
```

## Stack

```text
React 19
React DOM 19
TypeScript 6
Vite 8
Material UI 9
TanStack Query 5
Axios
React Router 7
React Hook Form
Zod
Vitest 4
Testing Library
```

## Configuration

Create `.env.local`:

```dotenv
VITE_API_BASE_URL=http://localhost:8081
```

The application intentionally fails during startup when `VITE_API_BASE_URL` is missing.

## Install and run

```powershell
npm install
npm run dev
```

Default development URL:

```text
http://localhost:5173
```

The backend normally runs at:

```text
http://localhost:8081
```

## Commands

```powershell
npm run dev
npm run lint
npm run test
npm run test:watch
npm run build
npm run preview
```

## Application areas

### Public tenant flows

```text
/login
/register
/forgot-password
/reset-password
/accept-invitation
```

### Tenant application

```text
/dashboard
/users
/invitations
/projects
/projects/:projectId
/audit-logs
/account
/account/change-password
```

### System-admin application

```text
/system/login
/system/dashboard
/system/tenants
/system/admins
/system/audit-logs
/system/change-password
```

## Tenant role routing

```text
TENANT_ADMIN
    Dashboard, users, invitations, projects,
    audit logs, account settings

TENANT_MANAGER
    Dashboard, users, projects, account settings

TENANT_USER
    Projects, project details, account settings
```

A regular tenant user defaults to `/projects`.

Backend authorization remains authoritative even when a route or navigation item is hidden.

## Session behavior

Tenant sessions:

```text
Persist in localStorage
Restore through /api/auth/me
Use automatic access-token refresh
Deduplicate concurrent refresh requests
Clear after authentication failures
Clear tenant query data on logout
Remain locally available during temporary network failure
```

Sign out all devices:

```text
Invalidates existing tenant access and refresh sessions.
Other browsers return to sign-in on their next server request.
```

System-admin sessions are independent and currently use access tokens without refresh-token rotation.

## Architecture guides

```text
../guides/frontend_architecture.md
../guides/frontend_testing.md
../guides/security_model.md
```

## Verification

```powershell
npm run lint
npm run test
npm run build
```

Vitest configuration:

```text
Environment: jsdom
Setup: src/test/setup.ts
Test timeout: 15 seconds
Hook timeout: 15 seconds
Maximum workers: 2
```

## Adding a feature

```text
1. Define feature types.
2. Add a typed API module.
3. Add query/mutation hooks.
4. Build feature components.
5. Add or update a page.
6. Add route and role/capability handling.
7. Add API tests.
8. Add component/page tests.
9. Run lint, tests, and build.
10. Update the relevant guide.
```
