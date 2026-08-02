# Multi-Tenant SaaS Frontend — Testing Guide

This guide defines frontend verification for the current MVP and authorization-v2 work.

## 1. Commands

From `multitenant-saas-frontend/`:

```powershell
npm run lint
npm run test
npm run build
```

Watch mode:

```powershell
npm run test:watch
```

## 2. Vitest configuration

```text
Environment: jsdom
Setup file: src/test/setup.ts
CSS processing: enabled
Test timeout: 15 seconds
Hook timeout: 15 seconds
Maximum workers: 2
Test API base URL: http://localhost:8081
```

The worker limit reduces contention in the Material UI/jsdom suite. Do not increase timeouts to hide deterministic waiting or mock problems.

## 3. Test layers

### API tests

Verify:

```text
HTTP method and endpoint
Path/query parameters
Request body
Response unwrapping
Error normalization
```

### Session/client tests

Verify:

```text
Authorization header
Access-token refresh
Refresh-token rotation
Concurrent refresh deduplication
Single retry after 401
Session clearing after failed refresh
No refresh loop
Tenant/system storage isolation
```

### Component tests

Verify rendering, validation, pending/success/error states, confirmation dialogs, disabled actions, and accessible labels.

### Page tests

Verify initial query, loading, empty and error states, filtering/sorting/pagination, mutations, invalidation, navigation, and role/capability visibility.

### Route tests

Verify unauthenticated/public-only redirects, authenticated defaults, tenant/system isolation, role restrictions, hidden navigation, and account access.

## 4. Mocking rules

Preferred:

```text
Mock feature API modules in page tests.
Use real providers/router where route behavior matters.
Use real storage modules where session behavior matters.
```

Avoid unresolved promises, arbitrary sleeps, implementation-detail mocks, and duplicated component logic.

Reset mocks and local storage before each independent test.

## 5. Async rules

Use semantic waits:

```text
findByRole
findByText
waitFor
waitForElementToBeRemoved
```

When a test times out, inspect unresolved mocks, accidental network calls, fake timers, unreachable elements, open dialogs/menus, and query retry loops before changing global timeouts.

## 6. Current regression areas

```text
Tenant and system-admin login
Session restoration and token refresh
Public onboarding
Forgot/reset password
Invitation acceptance
Tenant and platform dashboards
Tenant users and invitations
Projects, members, and tasks
Tenant and platform audit logs
System-admin management
Account settings and password changes
Sign out all devices
Application shells
Role-aware navigation
```

## 7. Manual role smoke test

Use at least one account for each fixed tenant role.

### Tenant admin

```text
Dashboard, users, invitations, projects,
audit logs, and account visible
Administrative mutations work
```

### Tenant manager

```text
Dashboard, users, projects, and account visible
Invitations and audit logs hidden
Forbidden paths do not expose content
```

### Tenant user

```text
Default redirect to /projects
Projects and account visible
Dashboard, users, invitations, and audit logs hidden
Project/task relationship rules work
```

## 8. Multi-browser session test

1. Sign in as the same tenant user in two browsers.
2. In browser A, choose sign out all devices.
3. Browser A returns to sign-in.
4. In browser B, trigger a protected request.
5. The old access token receives 401.
6. The refresh token also receives 401.
7. Browser B clears its session and returns to sign-in.

Repeat after password change and password reset.

## 9. Cross-tenant smoke test

Use Tenant A and Tenant B. Capture Tenant A resource IDs, authenticate as Tenant B, and attempt access to Tenant A users, invitations, projects, memberships, and tasks.

Confirm no Tenant A data renders and the backend returns the intended 403 or 404 response. Frontend hiding is not sufficient; inspect the network response.

## 10. Network-failure test

1. Authenticate normally.
2. Stop the backend.
3. Refresh the frontend.
4. Confirm local authentication is not destroyed solely because the backend is unavailable.
5. Confirm pages show recoverable query errors.
6. Restart the backend and retry.

## 11. Authorization-v2 tests

Add coverage for:

```text
Arbitrary-depth tree rendering
Unit movement validation
Direct-report visibility
Subtree visibility
Same-role users with different scopes
Custom-role capability rendering
Expired/revoked assignments
Delegation start/end behavior
Forbidden action visibility
Effective-access explanation
Fixed-role compatibility
```

Frontend tests prove correct use of backend capability/effective-access data; backend integration tests prove the authorization algorithm.

## 12. Authorization-v2 personas

Create reusable fixtures:

```text
Tenant owner/admin
Division manager
Department manager
Assistant manager
Team lead
Member
User outside subtree
Temporary delegate
Expired delegate
Project lead outside organizational authority
```

## 13. Definition of done

```text
Typed API contract exists.
Loading, empty, error, and success states exist.
Form validation exists where required.
Unauthorized actions are hidden or disabled.
Backend denial is handled.
API tests exist.
Page/component tests exist.
Full test suite passes.
Production build passes.
Documentation is updated.
```
