import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
    fireEvent,
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

import { systemAdminApi } from '../features/system-admin/api/systemAdminApi'
import { appTheme } from '../theme/appTheme'
import { SystemChangePasswordPage } from './SystemChangePasswordPage'

const currentAdmin = {
    systemAdminId: 'admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    role: 'SYSTEM_ADMIN' as const,
    status: 'ACTIVE' as const,
}

function renderPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: { retry: false },
            queries: { retry: false },
        },
    })

    return render(
        <ThemeProvider theme={appTheme}>
            <QueryClientProvider client={queryClient}>
                <MemoryRouter initialEntries={['/system/change-password']}>
                    <Routes>
                        <Route
                            path="system/change-password"
                            element={<SystemChangePasswordPage />}
                        />
                        <Route
                            path="system/dashboard"
                            element={<output>Global dashboard destination</output>}
                        />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

function fillPasswords({
    currentPassword = 'Current@123',
    newPassword = 'Stronger@456',
    confirmPassword = 'Stronger@456',
}: {
    currentPassword?: string
    newPassword?: string
    confirmPassword?: string
} = {}): void {
    fireEvent.change(screen.getByLabelText(/current password/i), {
        target: { value: currentPassword },
    })
    fireEvent.change(screen.getByLabelText(/^new password/i), {
        target: { value: newPassword },
    })
    fireEvent.change(screen.getByLabelText(/confirm new password/i), {
        target: { value: confirmPassword },
    })
}

describe('SystemChangePasswordPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('submits the exact current and new passwords and keeps the session active', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(systemAdminApi, 'changePassword')
            .mockResolvedValue(currentAdmin)
        renderPage()
        fillPasswords()

        await user.click(screen.getByRole('button', {
            name: /^change password$/i,
        }))

        expect(changePassword).toHaveBeenCalledWith({
            currentPassword: 'Current@123',
            newPassword: 'Stronger@456',
            confirmPassword: 'Stronger@456',
        })
        expect(await screen.findByText(
            /password changed successfully/i,
        )).toBeInTheDocument()
        expect(screen.getByText(
            /current system-console session remains active/i,
        )).toBeInTheDocument()
        expect(screen.getByLabelText(/current password/i)).toHaveValue('')
    })

    it('rejects a weak, mismatched, or reused password before calling the API', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(systemAdminApi, 'changePassword')
        renderPage()

        fillPasswords({ newPassword: 'weak', confirmPassword: 'weak' })
        await user.click(screen.getByRole('button', {
            name: /^change password$/i,
        }))
        expect(await screen.findByText(
            /new password must be 8–100 characters/i,
        )).toBeInTheDocument()

        fillPasswords({ confirmPassword: 'Different@456' })
        await user.click(screen.getByRole('button', {
            name: /^change password$/i,
        }))
        expect(await screen.findByText(
            /new password and confirmation do not match/i,
        )).toBeInTheDocument()

        fillPasswords({
            currentPassword: 'Current@123',
            newPassword: 'Current@123',
            confirmPassword: 'Current@123',
        })
        await user.click(screen.getByRole('button', {
            name: /^change password$/i,
        }))
        expect(await screen.findByText(
            /new password must be different/i,
        )).toBeInTheDocument()
        expect(changePassword).not.toHaveBeenCalled()
    })

    it('shows the backend error when the current password is incorrect', async () => {
        const user = userEvent.setup()
        vi.spyOn(systemAdminApi, 'changePassword').mockRejectedValue(
            new Error('Current password is incorrect'),
        )
        renderPage()
        fillPasswords()

        await user.click(screen.getByRole('button', {
            name: /^change password$/i,
        }))

        expect(await screen.findByText(
            'Current password is incorrect',
        )).toBeInTheDocument()
    })

    it('returns to the global dashboard without submitting', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(systemAdminApi, 'changePassword')
        renderPage()

        await user.click(screen.getByRole('button', { name: /cancel/i }))

        expect(await screen.findByText(
            'Global dashboard destination',
        )).toBeInTheDocument()
        expect(changePassword).not.toHaveBeenCalled()
    })
})
