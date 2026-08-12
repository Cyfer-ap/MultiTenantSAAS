import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '../features/auth/context/AuthContext'
import type { AuthContextValue } from '../features/auth/context/AuthContext'
import { authorizationApi } from '../features/authorization/api/authorizationApi'
import { createTenantAuthorizationContext } from '../features/authorization/test/authorizationTestData'
import { authorizationPermissionCodes } from '../features/authorization/types/authorization'
import { usersApi } from '../features/users/api/usersApi'
import type { TenantUser } from '../features/users/types/users'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { UsersPage } from './UsersPage'

const tenantUser: TenantUser = {
    id: 'user-1',
    tenantId: 'tenant-1',
    fullName: 'Ada Admin',
    email: 'ada@example.com',
    role: 'TENANT_ADMIN',
    status: 'ACTIVE',
    createdAt: '2026-07-15T10:30:00Z',
    updatedAt: '2026-07-15T10:30:00Z',
}

const memberUser: TenantUser = {
    id: 'user-2',
    tenantId: 'tenant-1',
    fullName: 'Grace User',
    email: 'grace@example.com',
    role: 'TENANT_USER',
    status: 'ACTIVE',
    createdAt: '2026-07-16T10:30:00Z',
    updatedAt: '2026-07-16T10:30:00Z',
}

const usersPage: PageResponse<TenantUser> = {
    content: [tenantUser, memberUser],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    first: true,
    last: true,
}

const fullUserManagementAuthorization = createTenantAuthorizationContext({
    permissionCodes: [
        authorizationPermissionCodes.USER_READ,
        authorizationPermissionCodes.USER_CREATE,
        authorizationPermissionCodes.USER_UPDATE,
        authorizationPermissionCodes.USER_STATUS_UPDATE,
        authorizationPermissionCodes.AUTHORIZATION_MANAGE,
    ],
})

const readOnlyUserAuthorization = createTenantAuthorizationContext({
    permissionCodes: [authorizationPermissionCodes.USER_READ],
})

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
        email: 'ada@example.com',
        role: 'TENANT_ADMIN',
    },
    login: vi.fn(),
    logout: vi.fn(),
}

function createTestQueryClient(): QueryClient {
    return new QueryClient({
        defaultOptions: {
            queries: {
                retry: false,
            },
        },
    })
}

function renderUsersPage(contextValue: AuthContextValue = authContextValue) {
    const queryClient = createTestQueryClient()

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<UsersPage />, {
        wrapper: Wrapper,
    })
}

describe('UsersPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()

        vi.spyOn(authorizationApi, 'getCurrentAuthorizationContext').mockResolvedValue(
            fullUserManagementAuthorization,
        )
    })

    it('shows a loading state while users are pending', () => {
        vi.spyOn(usersApi, 'getUsers').mockReturnValue(new Promise(() => undefined))
        vi.mocked(authorizationApi.getCurrentAuthorizationContext).mockResolvedValue(
            createTenantAuthorizationContext({
                permissionCodes: [
                    authorizationPermissionCodes.USER_READ,
                    authorizationPermissionCodes.USER_CREATE,
                ],
            }),
        )

        renderUsersPage()

        expect(
            screen.getByRole('status', {
                name: /loading users/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders tenant users returned by the API', async () => {
        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)

        renderUsersPage()

        expect(await screen.findByText('Ada Admin')).toBeInTheDocument()

        expect(screen.getByText('ada@example.com')).toBeInTheDocument()

        expect(screen.getByText('Administrator')).toBeInTheDocument()

        expect(screen.getAllByText('Active')).toHaveLength(2)
    })

    it('submits a server-side name or email search', async () => {
        const user = userEvent.setup()
        const getUsers = vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)

        renderUsersPage()

        await screen.findByText('Ada Admin')

        await user.type(screen.getByLabelText(/search users/i), '  ada@example.com  ')
        await user.click(
            screen.getByRole('button', {
                name: /^search$/i,
            }),
        )

        expect(getUsers).toHaveBeenLastCalledWith(
            'tenant-1',
            expect.objectContaining({
                page: 0,
                search: 'ada@example.com',
            }),
        )
    })

    it('requests server-side sorting from table headers', async () => {
        const user = userEvent.setup()
        const getUsers = vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)

        renderUsersPage()

        await screen.findByText('Ada Admin')

        await user.click(
            screen.getByRole('button', {
                name: /^user$/i,
            }),
        )

        expect(getUsers).toHaveBeenLastCalledWith(
            'tenant-1',
            expect.objectContaining({
                page: 0,
                sortBy: 'fullName',
                sortDir: 'asc',
            }),
        )
    })

    it('retries after the users request fails', async () => {
        const user = userEvent.setup()

        vi.spyOn(usersApi, 'getUsers')
            .mockRejectedValueOnce(new Error('Users service unavailable.'))
            .mockResolvedValueOnce(usersPage)

        renderUsersPage()

        expect(await screen.findByText('Users service unavailable.')).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /retry/i,
            }),
        )

        expect(await screen.findByText('Ada Admin')).toBeInTheDocument()
    })

    it('keeps user management controls hidden without V2 permissions', async () => {
        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)
        vi.mocked(authorizationApi.getCurrentAuthorizationContext).mockResolvedValue(
            readOnlyUserAuthorization,
        )

        renderUsersPage()

        await screen.findByText('Ada Admin')

        expect(
            screen.queryByRole('button', {
                name: /add user/i,
            }),
        ).not.toBeInTheDocument()

        expect(
            screen.queryByRole('button', {
                name: /manage grace user/i,
            }),
        ).not.toBeInTheDocument()
    })

    it('uses V2 create permission rather than the legacy role', async () => {
        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)

        renderUsersPage({
            ...authContextValue,
            session: {
                ...authContextValue.session!,
                role: 'TENANT_USER',
            },
        })

        await screen.findByText('Ada Admin')

        expect(
            await screen.findByRole('button', {
                name: /add user/i,
            }),
        ).toBeInTheDocument()
    })

    it('creates a tenant user with normalized input', async () => {
        const user = userEvent.setup()
        const createdUser: TenantUser = {
            ...memberUser,
            id: 'user-3',
            fullName: 'Lin User',
            email: 'lin@example.com',
        }

        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)
        const createUser = vi.spyOn(usersApi, 'createUser').mockResolvedValue(createdUser)

        renderUsersPage()

        await screen.findByText('Ada Admin')
        await user.click(
            await screen.findByRole('button', {
                name: /add user/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /add tenant user/i,
        })

        await user.type(within(dialog).getByLabelText(/full name/i), '  Lin User  ')
        await user.type(within(dialog).getByLabelText(/^email/i), '  LIN@EXAMPLE.COM  ')
        await user.type(within(dialog).getByLabelText(/initial password/i), 'Strong@123')
        await user.click(
            within(dialog).getByRole('button', {
                name: /create user/i,
            }),
        )

        await waitFor(() => {
            expect(createUser).toHaveBeenCalledWith('tenant-1', {
                fullName: 'Lin User',
                email: 'lin@example.com',
                password: 'Strong@123',
                role: 'TENANT_USER',
            })
        })

        expect(await screen.findByText('Lin User was created successfully.')).toBeInTheDocument()
    })

    it('updates a tenant user profile', async () => {
        const user = userEvent.setup()
        const updatedUser = {
            ...memberUser,
            fullName: 'Grace Hopper',
        }

        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)
        const updateUser = vi.spyOn(usersApi, 'updateUser').mockResolvedValue(updatedUser)

        renderUsersPage()

        await screen.findByText('Grace User')
        await user.click(
            await screen.findByRole('button', {
                name: /manage grace user/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /edit profile/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /edit user profile/i,
        })
        const fullNameInput = within(dialog).getByLabelText(/full name/i)

        await user.clear(fullNameInput)
        await user.type(fullNameInput, 'Grace Hopper')
        await user.click(
            within(dialog).getByRole('button', {
                name: /save profile/i,
            }),
        )

        await waitFor(() => {
            expect(updateUser).toHaveBeenCalledWith('tenant-1', 'user-2', {
                fullName: 'Grace Hopper',
                email: 'grace@example.com',
            })
        })
    })

    it('changes a tenant user role and status', async () => {
        const user = userEvent.setup()

        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)
        const updateRole = vi.spyOn(usersApi, 'updateUserRole').mockResolvedValue({
            ...memberUser,
            role: 'TENANT_MANAGER',
        })
        const updateStatus = vi.spyOn(usersApi, 'updateUserStatus').mockResolvedValue({
            ...memberUser,
            status: 'SUSPENDED',
        })

        renderUsersPage()

        await screen.findByText('Grace User')
        await user.click(
            await screen.findByRole('button', {
                name: /manage grace user/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /change role/i,
            }),
        )

        let dialog = screen.getByRole('dialog', {
            name: /change user role/i,
        })
        await user.click(
            within(dialog).getByRole('combobox', {
                name: /^role$/i,
            }),
        )
        await user.click(
            screen.getByRole('option', {
                name: /manager/i,
            }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /change role/i,
            }),
        )

        await waitFor(() => {
            expect(updateRole).toHaveBeenCalledWith('tenant-1', 'user-2', {
                role: 'TENANT_MANAGER',
            })
        })

        await user.click(
            await screen.findByRole('button', {
                name: /manage grace user/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /change status/i,
            }),
        )

        dialog = screen.getByRole('dialog', {
            name: /change account status/i,
        })
        await user.click(
            within(dialog).getByRole('combobox', {
                name: /^status$/i,
            }),
        )
        await user.click(
            screen.getByRole('option', {
                name: /suspended/i,
            }),
        )
        await user.click(
            within(dialog).getByRole('button', {
                name: /change status/i,
            }),
        )

        await waitFor(() => {
            expect(updateStatus).toHaveBeenCalledWith('tenant-1', 'user-2', { status: 'SUSPENDED' })
        })
    })

    it('unlocks tenant user login attempts', async () => {
        const user = userEvent.setup()

        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(usersPage)
        const unlockUserLogin = vi.spyOn(usersApi, 'unlockUserLogin').mockResolvedValue(memberUser)

        renderUsersPage()

        await screen.findByText('Grace User')
        await user.click(
            await screen.findByRole('button', {
                name: /manage grace user/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /unlock login/i,
            }),
        )

        const dialog = screen.getByRole('dialog', {
            name: /unlock user login/i,
        })
        await user.click(
            within(dialog).getByRole('button', {
                name: /unlock login/i,
            }),
        )

        await waitFor(() => {
            expect(unlockUserLogin).toHaveBeenCalledWith('tenant-1', 'user-2')
        })
    })
})
