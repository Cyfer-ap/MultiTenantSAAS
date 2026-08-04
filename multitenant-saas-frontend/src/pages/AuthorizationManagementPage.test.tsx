import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
    waitFor,
    within,
} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { authorizationApi } from '../features/authorization/api/authorizationApi'
import type {
    AuthorizationPermission,
    AuthorizationRole,
    AuthorizationUserRoleAssignment,
} from '../features/authorization/types/authorization'
import { appTheme } from '../theme/appTheme'
import { AuthorizationManagementPage } from './AuthorizationManagementPage'

const permission: AuthorizationPermission = {
    id: 'permission-1',
    tenantId: null,
    code: 'project.read',
    name: 'Project read',
    description: 'Read projects.',
    category: 'PROJECT',
    source: 'SYSTEM',
    status: 'ACTIVE',
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

const systemRole: AuthorizationRole = {
    id: 'role-system',
    tenantId: 'tenant-1',
    code: 'ADMIN',
    name: 'Administrator',
    description: 'System-managed administrator.',
    source: 'SYSTEM',
    status: 'ACTIVE',
    permissions: [permission],
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

const tenantRole: AuthorizationRole = {
    id: 'role-tenant',
    tenantId: 'tenant-1',
    code: 'TENANT_OPERATOR',
    name: 'Tenant Operator',
    description: 'Tenant-managed operator.',
    source: 'TENANT',
    status: 'ACTIVE',
    permissions: [permission],
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

const assignment: AuthorizationUserRoleAssignment = {
    id: 'assignment-1',
    tenantId: 'tenant-1',
    userId: 'user-2',
    userFullName: 'Grace User',
    userEmail: 'grace@example.com',
    roleId: tenantRole.id,
    roleCode: tenantRole.code,
    roleName: tenantRole.name,
    roleSource: 'TENANT',
    scopeType: 'TENANT',
    scopeTargetId: null,
    status: 'ACTIVE',
    validFrom: '2026-08-05T00:00:00Z',
    validUntil: null,
    createdByUserId: 'user-1',
    createdByUserEmail: 'admin@example.com',
    createdAt: '2026-08-05T00:00:00Z',
    updatedAt: '2026-08-05T00:00:00Z',
}

const authContextValue: AuthContextValue = {
    status: 'authenticated',
    session: {
        accessToken: 'access-token',
        refreshToken: 'refresh-token',
        tokenType: 'Bearer',
        accessTokenExpiresAt: Date.now() + 60_000,
        tenantId: 'tenant-1',
        userId: 'user-1',
        fullName: 'Ada Admin',
        email: 'admin@example.com',
        role: 'TENANT_ADMIN',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })
}

function renderPage() {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <AuthContext.Provider value={authContextValue}>
                        {children}
                    </AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<AuthorizationManagementPage />, {
        wrapper: Wrapper,
    })
}

describe('AuthorizationManagementPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(
            authorizationApi,
            'getPermissions',
        ).mockResolvedValue([permission])
        vi.spyOn(
            authorizationApi,
            'getRoles',
        ).mockResolvedValue([systemRole, tenantRole])
        vi.spyOn(
            authorizationApi,
            'getUserAssignments',
        ).mockResolvedValue([])
        vi.spyOn(
            authorizationApi,
            'initializeDefaultRoles',
        ).mockResolvedValue([systemRole, tenantRole])
    })

    it('keeps system roles read-only and tenant roles manageable', async () => {
        renderPage()

        expect(
            await screen.findByText('Tenant Operator'),
        ).toBeInTheDocument()

        expect(
            screen.queryByRole('button', {
                name: /edit permissions for administrator/i,
            }),
        ).not.toBeInTheDocument()

        expect(
            screen.getByRole('button', {
                name: /edit permissions for tenant operator/i,
            }),
        ).toBeInTheDocument()
    })

    it('creates a normalized tenant role', async () => {
        const user = userEvent.setup()
        const createRole = vi
            .spyOn(authorizationApi, 'createRole')
            .mockResolvedValue({
                ...tenantRole,
                id: 'role-created',
                code: 'PROJECT_COORDINATOR',
                name: 'Project Coordinator',
            })

        renderPage()
        await screen.findByText('Tenant Operator')

        await user.click(
            screen.getByRole('button', {
                name: /create role/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /create authorization role/i,
        })
        await user.type(
            within(dialog).getByLabelText(/role code/i),
            'project coordinator',
        )
        await user.type(
            within(dialog).getByLabelText(/role name/i),
            'Project Coordinator',
        )
        await user.click(
            within(dialog).getByRole('checkbox', {
                name: /project read/i,
            }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /^create role$/i,
            }),
        )

        await waitFor(() => {
            expect(createRole).toHaveBeenCalledWith(
                'tenant-1',
                {
                    code: 'PROJECT_COORDINATOR',
                    name: 'Project Coordinator',
                    description: null,
                    permissionIds: ['permission-1'],
                },
            )
        })
    })

    it('creates a tenant-scoped assignment for an exact user ID', async () => {
        const user = userEvent.setup()
        const createAssignment = vi
            .spyOn(authorizationApi, 'createAssignment')
            .mockResolvedValue(assignment)

        renderPage()
        await screen.findByText('Tenant Operator')

        await user.click(
            screen.getByRole('tab', {
                name: /assignments/i,
            }),
        )
        await user.type(
            screen.getByLabelText(/user id/i),
            'user-2',
        )
        await user.click(
            screen.getByRole('button', {
                name: /load assignments/i,
            }),
        )

        expect(
            await screen.findByText(/no assignments found/i),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /assign role/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /assign authorization role/i,
        })
        await user.click(
            within(dialog).getByLabelText(/^role$/i),
        )
        await user.click(
            screen.getByRole('option', {
                name: /tenant operator/i,
            }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /^assign role$/i,
            }),
        )

        await waitFor(() => {
            expect(createAssignment).toHaveBeenCalledWith(
                'tenant-1',
                {
                    userId: 'user-2',
                    roleId: 'role-tenant',
                    scopeType: 'TENANT',
                    scopeTargetId: null,
                    validFrom: null,
                    validUntil: null,
                },
            )
        })
    })
})
