import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    act,
    render,
    screen,
    within,
} from '@testing-library/react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'
import { MemoryRouter } from 'react-router'

import App from './App'
import { dashboardApi } from './features/dashboard/api/dashboardApi'
import type { TenantDashboardSummary } from './features/dashboard/types/dashboard'
import { authApi } from './features/auth/api/authApi'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { authStorage } from './features/auth/storage/authStorage'
import type {
    AuthSession,
    CurrentUserResponse,
    TenantRole,
} from './features/auth/types/auth'
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
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
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

function renderAuthenticatedRoute(
    role: TenantRole,
    initialPath: string,
): void {
    const queryClient = createTestQueryClient()

    authStorage.write({
        ...storedSession,
        role,
    })

    vi.spyOn(
        authApi,
        'getCurrentUser',
    ).mockResolvedValue({
        ...currentUser,
        role,
    })

    render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter
                    initialEntries={[initialPath]}
                >
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

        vi.spyOn(
            dashboardApi,
            'getSummary',
        ).mockResolvedValue(dashboardSummary)
    })

    it(
        'redirects an unauthenticated user to login',
        async () => {
            const queryClient =
                createTestQueryClient()

            render(
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider
                        client={queryClient}
                    >
                        <MemoryRouter
                            initialEntries={['/dashboard']}
                        >
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

            expect(
                screen.getByLabelText(/tenant id/i),
            ).toBeInTheDocument()
        },
    )

    it(
        'returns to login when the active session is cleared',
        async () => {
            const queryClient =
                createTestQueryClient()

            authStorage.write(storedSession)

            vi.spyOn(
                authApi,
                'getCurrentUser',
            ).mockResolvedValue(currentUser)

            render(
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider
                        client={queryClient}
                    >
                        <MemoryRouter
                            initialEntries={['/dashboard']}
                        >
                            <AuthProvider>
                                <App />
                            </AuthProvider>
                        </MemoryRouter>
                    </QueryClientProvider>
                </ThemeProvider>,
            )

            expect(
                await screen.findByRole('heading', {
                    name: /example tenant/i,
                }),
            ).toBeInTheDocument()

            act(() => {
                authStorage.clear()
            })

            expect(
                await screen.findByRole('heading', {
                    name: /sign in/i,
                }),
            ).toBeInTheDocument()
        },
    )

    it(
        'exposes the public forgot-password route',
        async () => {
            const queryClient =
                createTestQueryClient()

            render(
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider
                        client={queryClient}
                    >
                        <MemoryRouter
                            initialEntries={['/forgot-password']}
                        >
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
        },
    )

    it(
        'exposes the public tenant-onboarding route',
        async () => {
            const queryClient =
                createTestQueryClient()

            render(
                <ThemeProvider theme={appTheme}>
                    <QueryClientProvider
                        client={queryClient}
                    >
                        <MemoryRouter
                            initialEntries={['/register']}
                        >
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
        },
    )

    it(
        'prevents a tenant manager from opening admin-only audit logs',
        async () => {
            renderAuthenticatedRoute(
                'TENANT_MANAGER',
                '/audit-logs',
            )

            expect(
                await screen.findByRole('heading', {
                    name: /example tenant/i,
                }),
            ).toBeInTheDocument()

            const navigation = screen.getByRole(
                'navigation',
                {
                    name: /primary navigation/i,
                },
            )

            expect(
                within(navigation).getByText('Dashboard'),
            ).toBeInTheDocument()

            expect(
                within(navigation).getByText('Users'),
            ).toBeInTheDocument()

            expect(
                within(navigation).getByText('Projects'),
            ).toBeInTheDocument()

            expect(
                within(navigation).queryByText(
                    'Invitations',
                ),
            ).not.toBeInTheDocument()

            expect(
                within(navigation).queryByText(
                    'Audit Logs',
                ),
            ).not.toBeInTheDocument()
        },
    )

    it(
        'redirects a tenant user to projects and hides restricted navigation',
        async () => {
            renderAuthenticatedRoute(
                'TENANT_USER',
                '/dashboard',
            )

            expect(
                await screen.findByRole('heading', {
                    name: /projects/i,
                }),
            ).toBeInTheDocument()

            const navigation = screen.getByRole(
                'navigation',
                {
                    name: /primary navigation/i,
                },
            )

            expect(
                within(navigation).getByText('Projects'),
            ).toBeInTheDocument()

            expect(
                within(navigation).queryByText(
                    'Dashboard',
                ),
            ).not.toBeInTheDocument()

            expect(
                within(navigation).queryByText('Users'),
            ).not.toBeInTheDocument()

            expect(
                within(navigation).queryByText(
                    'Invitations',
                ),
            ).not.toBeInTheDocument()

            expect(
                within(navigation).queryByText(
                    'Audit Logs',
                ),
            ).not.toBeInTheDocument()
        },
    )

    it(
        'allows a tenant administrator to open invitations',
        async () => {
            vi.spyOn(
                invitationsApi,
                'getInvitations',
            ).mockResolvedValue({
                content: [],
                page: 0,
                size: 10,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
            })

            renderAuthenticatedRoute(
                'TENANT_ADMIN',
                '/invitations',
            )

            expect(
                await screen.findByRole('heading', {
                    name: /invitations/i,
                }),
            ).toBeInTheDocument()

            const navigation = screen.getByRole(
                'navigation',
                {
                    name: /primary navigation/i,
                },
            )

            expect(
                within(navigation).getByText('Invitations'),
            ).toBeInTheDocument()
        },
    )

    it(
        'allows a regular tenant user to open account settings',
        async () => {
            renderAuthenticatedRoute(
                'TENANT_USER',
                '/account',
            )

            expect(
                await screen.findByRole('heading', {
                    name: /account settings/i,
                }),
            ).toBeInTheDocument()

            expect(
                await screen.findByText('Example Tenant'),
            ).toBeInTheDocument()
        },
    )
})
