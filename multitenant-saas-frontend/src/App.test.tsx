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
import { authApi } from './features/auth/api/authApi'
import { AuthProvider } from './features/auth/context/AuthProvider'
import { authStorage } from './features/auth/storage/authStorage'
import type {
    AuthSession,
    CurrentUserResponse,
    TenantRole,
} from './features/auth/types/auth'
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
                    name: /dashboard/i,
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
        'prevents a tenant manager from opening admin-only audit logs',
        async () => {
            renderAuthenticatedRoute(
                'TENANT_MANAGER',
                '/audit-logs',
            )

            expect(
                await screen.findByRole('heading', {
                    name: /dashboard/i,
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
                    'Audit Logs',
                ),
            ).not.toBeInTheDocument()
        },
    )
})
