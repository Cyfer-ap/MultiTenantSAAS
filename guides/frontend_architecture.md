# Multi-Tenant SaaS Frontend — Architecture Guide

This guide describes the current frontend architecture and conventions that should be preserved while adding hierarchical authorization.

```text
Repository baseline reviewed:
ae1fa4cbb5133ae0b3bcd2596379e1ab64f36be1
```

## 1. Technology choices

```text
React 19
TypeScript 6
Vite 8
Material UI 9
TanStack Query 5
Axios
React Router 7
React Hook Form
Zod
Vitest and Testing Library
```

## 2. Main source layout

```text
src/
├── api/
├── app/
├── components/
├── config/
├── features/
├── layouts/
├── pages/
├── routes/
├── test/
├── theme/
├── types/
├── App.tsx
└── main.tsx
```

Use feature-oriented folders for domain logic and shared folders only for genuinely cross-cutting concerns.

## 3. Application providers

`AppProviders` establishes:

```text
Material UI ThemeProvider
CssBaseline
TanStack QueryClientProvider
BrowserRouter
SystemAdminProvider
AuthProvider
```

Tenant and system-admin authentication remain separate because they represent different account types and session behavior.

## 4. Environment configuration

The application reads `VITE_API_BASE_URL` through `src/config/env.ts`. The value is trimmed and trailing slashes are removed.

Do not access `import.meta.env` throughout feature code. Add validated values to the central configuration module.

## 5. API architecture

Backend responses use typed success, error, and page contracts.

Feature API modules should:

```text
Declare exact request/response types
Use the correct tenant or system-admin HTTP client
Unwrap data consistently
Avoid returning raw Axios responses to components
Normalize backend errors
```

## 6. Tenant HTTP client

The tenant client:

```text
Adds the stored bearer token
Retries one 401 after token refresh
Deduplicates simultaneous refresh attempts
Rotates stored access and refresh tokens
Prevents refresh from overwriting a changed session
Clears authentication when refresh returns 401/403
Normalizes API errors
```

Do not implement refresh logic inside feature APIs or components.

## 7. System-admin HTTP client

System-admin requests use a separate client and storage module.

Current system-admin sessions have no refresh-token rotation. A system-admin 401 should clear only the system-admin session, not the tenant session.

## 8. Tenant authentication provider

The tenant `AuthProvider` controls:

```text
loading
authenticated
unauthenticated
```

Session restoration:

```text
1. Read local storage.
2. If absent, clear authentication.
3. Call GET /api/auth/me.
4. Merge validated current-user data.
5. Clear on 401/403.
6. Preserve the local session during temporary backend/network errors.
```

The provider subscribes to storage changes and clears tenant query data when authentication is removed.

## 9. System-admin authentication provider

The system-admin provider follows a similar restoration model but uses separate storage, separate APIs, and separate query keys. It has no refresh-token flow.

## 10. Storage rules

Tenant storage key:

```text
multitenant-saas.auth-session
```

Rules:

```text
Do not read localStorage directly in feature components.
Use the appropriate storage abstraction.
Reject and remove invalid serialized sessions.
Notify provider subscribers after storage changes.
Keep tenant and system-admin storage isolated.
```

## 11. Routing

All routes are defined in `src/routes/AppRoutes.tsx`.

Route groups:

```text
SystemPublicOnlyRoute
SystemProtectedRoute
PublicOnlyRoute
ProtectedRoute
RoleProtectedRoute
```

Current fixed-role access:

```text
/dashboard and /users
    TENANT_ADMIN, TENANT_MANAGER

/invitations and /audit-logs
    TENANT_ADMIN

/projects, /projects/:projectId,
/account, /account/change-password
    All authenticated tenant roles
```

When authorization v2 arrives, guards should consume effective capabilities returned by the backend rather than reconstructing hierarchy rules locally.

## 12. Navigation

Navigation currently derives from fixed tenant roles.

Future rule:

```text
Render an item when the effective-access payload
contains the required capability.
```

Do not duplicate organization-tree traversal or scope evaluation in the browser.

## 13. Server state

TanStack Query manages remote state.

Query keys should include all isolation boundaries:

```text
Account type
Tenant ID
Resource type
Parent resource ID
Filters
Pagination
Sorting
```

Mutation success should invalidate only relevant query families. Authentication removal must clear sensitive cached data.

## 14. Forms and validation

Use:

```text
React Hook Form
Zod schemas
Backend error normalization
Material UI form controls
```

Rules:

```text
Client validation improves feedback.
Backend validation remains authoritative.
Show field errors near fields.
Show mutation errors in an alert.
Disable duplicate submission while pending.
```

## 15. Feature organization

Typical feature layout:

```text
features/<feature>/
├── api/
├── components/
├── hooks/
├── session/       when needed
├── storage/       when needed
└── types/
```

Pages orchestrate features and layout. They should not contain reusable API, storage, or authorization algorithms.

Current feature domains include auth, system-admin, dashboard, users, invitations, projects, audit logs, onboarding, and password reset.

## 16. Layouts

```text
AppShell
SystemAdminShell
```

Responsibilities:

```text
Responsive navigation
Current account identity
Role-aware navigation
User menu
Outlet rendering
Mobile drawer behavior
```

Business mutations do not belong in layouts.

## 17. Error behavior

The UI distinguishes:

```text
Authentication failure
Authorization failure
Validation failure
Conflict/business-rule failure
Network/backend unavailability
Unexpected failure
```

Important rule:

```text
401/403 during validation or refresh:
clear the relevant session.

Temporary network/server failure during restoration:
preserve the local session and provide recoverable UI behavior.
```

## 18. Authorization-v2 preparation

Do not introduce fixed levels such as:

```text
MANAGER_LEVEL_1
MANAGER_LEVEL_2
ASSISTANT_MANAGER_LEVEL_3
```

Expected future frontend domains:

```text
Organization tree
Organization-unit details
User assignments
Reporting relationships
Permission catalogue
Role builder
Scoped role assignments
Delegation
Effective-access viewer
```

The backend must calculate effective access. The frontend should use capability identifiers and summarized assignment data for route/action visibility.

## 19. Capability-driven UI

Prefer capabilities such as:

```text
canReadOrganization
canManageOrganization
canAssignRole
canManageDirectReports
canManageSubtree
canManageProject
```

Avoid checking role names throughout pages. During migration, use a compatibility adapter that maps fixed roles to capabilities.

## 20. Change checklist

For every feature:

```text
Add/update types
Add typed API function
Add query/mutation hook
Add loading/empty/error/success states
Add route/action visibility
Add API tests
Add component/page tests
Test forbidden state
Verify backend denial independently
Run lint, test, and build
Update documentation
```
