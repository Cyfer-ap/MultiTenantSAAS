import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    render,
    screen,
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

const usersPage: PageResponse<TenantUser> = {
    content: [tenantUser],
    page: 0,
    size: 10,
    totalElements: 1,
    totalPages: 1,
    first: true,
    last: true,
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

function renderUsersPage() {
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

    return render(<UsersPage />, {
        wrapper: Wrapper,
    })
}

describe('UsersPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('shows a loading state while users are pending', () => {
        vi.spyOn(usersApi, 'getUsers').mockReturnValue(
            new Promise(() => undefined),
        )

        renderUsersPage()

        expect(
            screen.getByRole('status', {
                name: /loading users/i,
            }),
        ).toBeInTheDocument()
    })

    it('renders tenant users returned by the API', async () => {
        vi.spyOn(usersApi, 'getUsers').mockResolvedValue(
            usersPage,
        )

        renderUsersPage()

        expect(
            await screen.findByText('Ada Admin'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('ada@example.com'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('Administrator'),
        ).toBeInTheDocument()

        expect(
            screen.getByText('Active'),
        ).toBeInTheDocument()
    })

    it('submits a server-side name or email search', async () => {
        const user = userEvent.setup()
        const getUsers = vi
            .spyOn(usersApi, 'getUsers')
            .mockResolvedValue(usersPage)

        renderUsersPage()

        await screen.findByText('Ada Admin')

        await user.type(
            screen.getByLabelText(/search users/i),
            '  ada@example.com  ',
        )
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
        const getUsers = vi
            .spyOn(usersApi, 'getUsers')
            .mockResolvedValue(usersPage)

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
            .mockRejectedValueOnce(
                new Error('Users service unavailable.'),
            )
            .mockResolvedValueOnce(usersPage)

        renderUsersPage()

        expect(
            await screen.findByText(
                'Users service unavailable.',
            ),
        ).toBeInTheDocument()

        await user.click(
            screen.getByRole('button', {
                name: /retry/i,
            }),
        )

        expect(
            await screen.findByText('Ada Admin'),
        ).toBeInTheDocument()
    })
})
