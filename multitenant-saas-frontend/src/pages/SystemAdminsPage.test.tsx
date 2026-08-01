import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    fireEvent,
    render,
    screen,
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

import { systemAdminApi } from '../features/system-admin/api/systemAdminApi'
import { SystemAdminContext } from '../features/system-admin/context/SystemAdminContext'
import type { SystemAdminRecord } from '../features/system-admin/types/systemAdmin'
import { appTheme } from '../theme/appTheme'
import type { PageResponse } from '../types/api'
import { SystemAdminsPage } from './SystemAdminsPage'

const owner: SystemAdminRecord = {
    id: 'admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    status: 'ACTIVE',
    failedLoginAttempts: 0,
    lockedUntil: null,
    createdAt: '2026-08-01T10:00:00Z',
    updatedAt: '2026-08-01T10:00:00Z',
}

const lockedAdmin: SystemAdminRecord = {
    id: 'admin-2',
    fullName: 'Security Operator',
    email: 'security@example.com',
    status: 'ACTIVE',
    failedLoginAttempts: 5,
    lockedUntil: '2099-08-02T10:00:00Z',
    createdAt: '2026-08-02T10:00:00Z',
    updatedAt: '2026-08-02T10:00:00Z',
}

const adminPage: PageResponse<SystemAdminRecord> = {
    content: [owner, lockedAdmin],
    page: 0,
    size: 10,
    totalElements: 2,
    totalPages: 1,
    first: true,
    last: true,
}

function renderAdmins() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <SystemAdminContext.Provider value={{
                        status: 'authenticated',
                        session: {
                            systemAdminId: owner.id,
                            fullName: owner.fullName,
                            email: owner.email,
                            role: 'SYSTEM_ADMIN',
                            accessToken: 'system-token',
                            tokenType: 'Bearer',
                            accessTokenExpiresAt: Date.now() + 60_000,
                        },
                        login: vi.fn(),
                        logout: vi.fn(),
                    }}>
                        {children}
                    </SystemAdminContext.Provider>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<SystemAdminsPage />, { wrapper: Wrapper })
}

describe('SystemAdminsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        vi.spyOn(systemAdminApi, 'getSystemAdmins')
            .mockResolvedValue(adminPage)
    })

    it('renders administrators, current-account safety, and login restrictions', async () => {
        const user = userEvent.setup()
        renderAdmins()

        expect(await screen.findByText('Platform Owner')).toBeInTheDocument()
        expect(screen.getByText('Security Operator')).toBeInTheDocument()
        expect(screen.getByText('You')).toBeInTheDocument()
        expect(screen.getByText('Restricted')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /^unlock$/i })).toBeInTheDocument()

        const statusButtons = screen.getAllByRole('button', {
            name: /change status/i,
        })
        await user.click(statusButtons[0])
        const dialog = screen.getByRole('dialog', {
            name: /change administrator status/i,
        })
        expect(within(dialog).getByText(/must remain active/i)).toBeInTheDocument()
    })

    it('submits search, status, and sorting parameters', async () => {
        const user = userEvent.setup()
        const getAdmins = vi.mocked(systemAdminApi.getSystemAdmins)
        renderAdmins()
        await screen.findByText('Platform Owner')

        fireEvent.change(screen.getByLabelText(/search administrators/i), {
            target: { value: '  security  ' },
        })
        await user.click(screen.getByLabelText(/^status$/i))
        await user.click(screen.getByRole('option', { name: 'Suspended' }))
        await user.click(screen.getByRole('button', { name: /^search$/i }))
        await user.click(screen.getByRole('button', { name: /^administrator$/i }))

        expect(getAdmins).toHaveBeenLastCalledWith(
            expect.objectContaining({
                page: 0,
                search: 'security',
                sortBy: 'fullName',
                sortDir: 'asc',
                status: 'SUSPENDED',
            }),
        )
    })

    it('normalizes and creates a system administrator', async () => {
        const user = userEvent.setup()
        const create = vi.spyOn(systemAdminApi, 'createSystemAdmin')
            .mockResolvedValue(lockedAdmin)
        renderAdmins()
        await screen.findByText('Platform Owner')

        await user.click(screen.getByRole('button', { name: /add administrator/i }))
        const dialog = screen.getByRole('dialog', {
            name: /create system administrator/i,
        })
        fireEvent.change(within(dialog).getByLabelText(/^full name$/i), {
            target: { value: '  Security Operator  ' },
        })
        fireEvent.change(within(dialog).getByLabelText(/^email address$/i), {
            target: { value: '  SECURITY@EXAMPLE.COM  ' },
        })
        fireEvent.change(within(dialog).getByLabelText(/^temporary password$/i), {
            target: { value: 'Strong@123' },
        })
        fireEvent.change(within(dialog).getByLabelText(/confirm temporary password/i), {
            target: { value: 'Strong@123' },
        })
        await user.click(within(dialog).getByRole('button', {
            name: /create administrator/i,
        }))

        expect(create).toHaveBeenCalledWith({
            fullName: 'Security Operator',
            email: 'security@example.com',
            password: 'Strong@123',
        })
        expect(await screen.findByText(/was added as a system administrator/i)).toBeInTheDocument()
    })

    it('updates another administrator status and unlocks login', async () => {
        const user = userEvent.setup()
        const update = vi.spyOn(systemAdminApi, 'updateSystemAdminStatus')
            .mockResolvedValue({ ...lockedAdmin, status: 'SUSPENDED' })
        const unlock = vi.spyOn(systemAdminApi, 'unlockSystemAdminLogin')
            .mockResolvedValue({
                ...lockedAdmin,
                failedLoginAttempts: 0,
                lockedUntil: null,
            })
        renderAdmins()
        await screen.findByText('Security Operator')

        const statusButtons = screen.getAllByRole('button', {
            name: /change status/i,
        })
        await user.click(statusButtons[1])
        let dialog = screen.getByRole('dialog', {
            name: /change administrator status/i,
        })
        await user.click(within(dialog).getByLabelText(/^status$/i))
        await user.click(screen.getByRole('option', { name: 'Suspended' }))
        await user.click(within(dialog).getByRole('button', { name: /save status/i }))

        expect(update).toHaveBeenCalledWith(
            lockedAdmin.id,
            { status: 'SUSPENDED' },
        )

        await user.click(screen.getByRole('button', { name: /^unlock$/i }))
        dialog = screen.getByRole('dialog', {
            name: /unlock administrator login/i,
        })
        await user.click(within(dialog).getByRole('button', { name: /unlock login/i }))

        expect(unlock).toHaveBeenCalledWith(lockedAdmin.id)
    })
})
