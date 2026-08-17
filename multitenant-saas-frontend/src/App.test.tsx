import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'

import App from './App'
import { dashboardApi } from './features/dashboard/api/dashboardApi'
import type { TenantDashboardSummary } from './features/dashboard/types/dashboard'
import { authApi } from './features/auth/api/authApi'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { authStorage } from './features/auth/storage/authStorage'
import type { AuthSession, CurrentUserResponse, TenantRole } from './features/auth/types/auth'
import { authorizationApi } from './features/authorization/api/authorizationApi'
import {
    authorizationPermissionCodes,
    type CurrentAuthorizationContext,
} from './features/authorization/types/authorization'
import { invitationsApi } from './features/invitations/api/invitationsApi'
import { appTheme } from './theme/appTheme'

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
            mutations: {
                retry: false,
            },
        },
    })
}

const storedSession: AuthSession = {
    accessToken: 'access-token',
    csrfToken: 'csrf-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    persistentSession: true,
    tenantId: 'tenant-id',
    userId: 'user-id',
    fullName: 'Tenant Admin',
    email: 'admin@example.com',
    role: 'TENANT_ADMIN',
}

const currentUser: CurrentUserResponse = {
    tenantId: 'tenant-id',
    tenantName: 'Example Tenant',
    tenantSlug: 'example-tenant',
    userId: 'user-id',
    fullName: 'Tenant Admin',
    email: 'admin@example.com',
    role: 'TENANT_ADMIN',
    status: 'ACTIVE',
}

const dashboardSummary: TenantDashboardSummary = {
    tenantId: 'tenant-id',
    tenantName: 'Example Tenant',
    tenantSlug: 'example-tenant',
    tenantStatus: 'ACTIVE',
    totalUsers: 1,
    activeUsers: 1,
    inactiveUsers: 0,
    suspendedUsers: 0,
    totalProjects: 0,
    planningProjects: 0,
    activeProjects: 0,
    onHoldProjects: 0,
    completedProjects: 0,
    archivedProjects: 0,
    totalProjectMemberships: 0,
    totalTasks: 0,
    todoTasks: 0,
    inProgressTasks: 0,
    blockedTasks: 0,
    completedTasks: 0,
    cancelledTasks: 0,
    overdueTasks: 0,
    taskCompletionPercentage: 0,
}

function createAuthorizationContext(role: TenantRole): CurrentAuthorizationContext {
    const permissionCodes =
        role === 'TENANT_ADMIN'
            ? Object.values(authorizationPermissionCodes)
            : role === 'TENANT_MANAGER'
              ? [
                    authorizationPermissionCodes.TENANT_READ,
                    authorizationPermissionCodes.USER_READ,
                    authorizationPermissionCodes.ORGANIZATION_UNIT_READ,
                    authorizationPermissionCodes.ORGANIZATION_ASSIGNMENT_READ,
                    authorizationPermissionCodes.PROJECT_READ,
                    authorizationPermissionCodes.PROJECT_CREATE,
                    authorizationPermissionCodes.PROJECT_UPDATE,
                    authorizationPermissionCodes.PROJECT_ARCHIVE,
                    authorizationPermissionCodes.PROJECT_MEMBER_MANAGE,
                    authorizationPermissionCodes.PROJECT_TASK_READ,
                    authorizationPermissionCodes.PROJECT_TASK_MANAGE,
                ]
              : [
                    authorizationPermissionCodes.TENANT_READ,
                    authorizationPermissionCodes.USER_READ,
                    authorizationPermissionCodes.ORGANIZATION_UNIT_READ,
                    authorizationPermissionCodes.ORGANIZATION_ASSIGNMENT_READ,
                    authorizationPermissionCodes.PROJECT_READ,
                ]

    const roleCode =
        role === 'TENANT_ADMIN' ? 'ADMIN' : role === 'TENANT_MANAGER' ? 'MANAGER' : 'MEMBER'

    return {
        tenantId: 'tenant-id',
        userId: 'user-id',
        fullName: 'Tenant User',
        email: 'user@example.com',
        evaluatedAt: '2026-08-04T12:00:00Z',
        tenantPermissionCodes: permissionCodes,
        allPermissionCodes: permissionCodes,
        grants: [
            {
                assignmentId: `assignment-${roleCode}`,
                roleId: `role-${roleCode}`,
                roleCode,
                roleName: roleCode,
                roleSource: 'SYSTEM',
                scopeType: 'TENANT',
                scopeTargetId: null,
                validFrom: '2026-08-01T00:00:00Z',
                validUntil: null,
                permissionCodes,
            },
        ],
    }
}

function renderAuthenticatedRoute(
    role: TenantRole,
    initialPath: string,
    authorizationContext = createAuthorizationContext(role),
): void {
    const queryClient = createTestQueryClient()

    authStorage.write({
        ...storedSession,
        role,
    })

    vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue({
        ...currentUser,
        role,
    })

    vi.mocked(authorizationApi.getCurrentAuthorizationContext).mockResolvedValue(
        authorizationContext,
    )

    render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={[initialPath]}>
                    <AuthProvider>
                        <App />
                    </AuthProvider>
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

describe('App authentication routes', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        localStorage.clear()
        sessionStorage.clear()

        vi.spyOn(dashboardApi, 'getSummary').mockResolvedValue(dashboardSummary)

        vi.spyOn(authorizationApi, 'getCurrentAuthorizationContext').mockResolvedValue(
            createAuthorizationContext('TENANT_ADMIN'),
        )
    })

    it('redirects an unauthenticated user to login', async () => {
        const queryClient = createTestQueryClient()

        render(
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/dashboard']}>
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>,
        )

        expect(
            await screen.findByRole('heading', {
                name: /sign in/i,
            }),
        ).toBeInTheDocument()

        expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
        expect(screen.queryByLabelText(/tenant id/i)).not.toBeInTheDocument()
    })

    it('returns to login when the active session is cleared', async () => {
        const queryClient = createTestQueryClient()

        authStorage.write(storedSession)

        vi.spyOn(authApi, 'getCurrentUser').mockResolvedValue(currentUser)

        render(
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/dashboard']}>
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>,
        )

        expect(await screen.findByRole('button', { name: /tenant admin/i })).toBeInTheDocument()

        act(() => {
            authStorage.clear()
        })

        expect(
            await screen.findByRole('heading', {
                name: /sign in/i,
            }),
        ).toBeInTheDocument()
    })

    it('exposes the public forgot-password route', async () => {
        const queryClient = createTestQueryClient()

        render(
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/forgot-password']}>
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>,
        )

        expect(
            await screen.findByRole('heading', {
                name: /forgot password/i,
            }),
        ).toBeInTheDocument()
    })

    it('exposes the public tenant-onboarding route', async () => {
        const queryClient = createTestQueryClient()

        render(
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter initialEntries={['/register']}>
                        <AuthProvider>
                            <App />
                        </AuthProvider>
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>,
        )

        expect(
            await screen.findByRole('heading', {
                name: /create workspace/i,
            }),
        ).toBeInTheDocument()
    })

    it('prevents a tenant manager from opening admin-only audit logs', async () => {
        renderAuthenticatedRoute('TENANT_MANAGER', '/audit-logs')

        expect(
            await screen.findByRole('heading', {
                name: /example tenant/i,
            }),
        ).toBeInTheDocument()

        const navigation = screen.getByRole('navigation', {
            name: /primary navigation/i,
        })

        expect(within(navigation).getByText('Dashboard')).toBeInTheDocument()

        expect(within(navigation).getByText('Users')).toBeInTheDocument()

        expect(within(navigation).getByText('Projects')).toBeInTheDocument()

        expect(within(navigation).getByText('Invitations')).toBeInTheDocument()

        expect(within(navigation).queryByText('Audit Logs')).not.toBeInTheDocument()
    })

    it('redirects a tenant member to projects and shows permitted navigation', async () => {
        renderAuthenticatedRoute('TENANT_USER', '/dashboard')

        expect(
            await screen.findByRole('heading', {
                name: /projects/i,
            }),
        ).toBeInTheDocument()

        const navigation = screen.getByRole('navigation', {
            name: /primary navigation/i,
        })

        expect(within(navigation).getByText('Projects')).toBeInTheDocument()

        expect(within(navigation).queryByText('Dashboard')).not.toBeInTheDocument()

        expect(within(navigation).getByText('Users')).toBeInTheDocument()

        expect(within(navigation).getByText('Invitations')).toBeInTheDocument()

        expect(within(navigation).queryByText('Audit Logs')).not.toBeInTheDocument()
    })

    it('allows a tenant administrator to open invitations', async () => {
        vi.spyOn(invitationsApi, 'getInvitations').mockResolvedValue({
            content: [],
            page: 0,
            size: 10,
            totalElements: 0,
            totalPages: 0,
            first: true,
            last: true,
        })

        renderAuthenticatedRoute('TENANT_ADMIN', '/invitations')

        expect(
            await screen.findByRole('heading', {
                name: /invitations/i,
            }),
        ).toBeInTheDocument()

        const navigation = screen.getByRole('navigation', {
            name: /primary navigation/i,
        })

        expect(within(navigation).getByText('Invitations')).toBeInTheDocument()
    })

    it('uses V2 permissions rather than the legacy role for navigation', async () => {
        const context = createAuthorizationContext('TENANT_USER')

        const authorizationContext: CurrentAuthorizationContext = {
            ...context,
            tenantPermissionCodes: [
                ...context.tenantPermissionCodes,
                authorizationPermissionCodes.AUDIT_READ,
            ],
            allPermissionCodes: [
                ...context.allPermissionCodes,
                authorizationPermissionCodes.AUDIT_READ,
            ],
            grants: [
                ...context.grants,
                {
                    assignmentId: 'assignment-auditor',
                    roleId: 'role-auditor',
                    roleCode: 'TENANT_AUDITOR',
                    roleName: 'Tenant Auditor',
                    roleSource: 'TENANT',
                    scopeType: 'TENANT',
                    scopeTargetId: null,
                    validFrom: '2026-08-01T00:00:00Z',
                    validUntil: null,
                    permissionCodes: [authorizationPermissionCodes.AUDIT_READ],
                },
            ],
        }

        renderAuthenticatedRoute('TENANT_USER', '/account', authorizationContext)

        expect(
            await screen.findByRole('heading', {
                name: /account settings/i,
            }),
        ).toBeInTheDocument()

        const navigation = screen.getByRole('navigation', {
            name: /primary navigation/i,
        })

        expect(await within(navigation).findByText('Audit Logs')).toBeInTheDocument()
    })

    it('allows a regular tenant user to open account settings', async () => {
        renderAuthenticatedRoute('TENANT_USER', '/account')

        expect(
            await screen.findByRole('heading', {
                name: /account settings/i,
            }),
        ).toBeInTheDocument()

        expect(await screen.findByText('Example Tenant')).toBeInTheDocument()
    })
})
