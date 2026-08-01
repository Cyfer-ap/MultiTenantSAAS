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
import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'
import {
    MemoryRouter,
    Route,
    Routes,
} from 'react-router'

import { authApi } from '../features/auth/api/authApi'
import { appTheme } from '../theme/appTheme'
import { AccountSettingsPage } from './AccountSettingsPage'

const currentUser = {
    tenantId: 'tenant-1',
    tenantName: 'Research Lab',
    tenantSlug: 'research-lab',
    userId: 'user-1',
    fullName: 'Grace Hopper',
    email: 'grace@example.com',
    role: 'TENANT_ADMIN' as const,
    status: 'ACTIVE' as const,
}

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
        },
    })

    return render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/account']}>
                    <Routes>
                        <Route
                            path="account"
                            element={<AccountSettingsPage />}
                        />
                        <Route
                            path="account/change-password"
                            element={<output>Password destination</output>}
                        />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

describe('AccountSettingsPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('shows the current account and workspace details', async () => {
        vi.spyOn(authApi, 'getCurrentUser')
            .mockResolvedValue(currentUser)
        renderPage()

        expect(await screen.findByRole('heading', {
            name: /account settings/i,
        })).toBeInTheDocument()
        expect(await screen.findAllByText('Grace Hopper')).toHaveLength(2)
        expect(screen.getAllByText('grace@example.com')).toHaveLength(2)
        expect(screen.getAllByText('Tenant administrator')).toHaveLength(2)
        expect(screen.getByText('Research Lab')).toBeInTheDocument()
        expect(screen.getByText('research-lab')).toBeInTheDocument()
        expect(screen.getByText('tenant-1')).toBeInTheDocument()
        expect(screen.getByText('user-1')).toBeInTheDocument()
        expect(screen.getByRole('heading', {
            name: /device sessions/i,
        })).toBeInTheDocument()
        expect(screen.getByRole('button', {
            name: /sign out all devices/i,
        })).toBeInTheDocument()
    })

    it('opens the tenant password-change route', async () => {
        const user = userEvent.setup()
        vi.spyOn(authApi, 'getCurrentUser')
            .mockResolvedValue(currentUser)
        renderPage()

        await user.click(await screen.findByRole('button', {
            name: /change password/i,
        }))

        expect(await screen.findByText(
            'Password destination',
        )).toBeInTheDocument()
    })

    it('shows a retry action when current-account loading fails', async () => {
        const user = userEvent.setup()
        const getCurrentUser = vi.spyOn(authApi, 'getCurrentUser')
            .mockRejectedValueOnce(new Error('Account service unavailable'))
            .mockResolvedValueOnce(currentUser)
        renderPage()

        expect(await screen.findByText(
            'Account service unavailable',
        )).toBeInTheDocument()

        await user.click(screen.getByRole('button', { name: /retry/i }))

        expect(await screen.findByText('Research Lab')).toBeInTheDocument()
        expect(getCurrentUser).toHaveBeenCalledTimes(2)
    })
})
