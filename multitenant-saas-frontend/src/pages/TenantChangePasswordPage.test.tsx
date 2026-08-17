import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'

import { authApi } from '../features/auth/api/authApi'
import { AuthContext } from '../features/auth/context/AuthContext'
import { LoginPage } from '../features/auth/pages/LoginPage'
import { authStorage } from '../features/auth/storage/authStorage'
import type { AuthSession } from '../features/auth/types/auth'
import { systemAdminStorage } from '../features/system-admin/storage/systemAdminStorage'
import type { SystemAdminSession } from '../features/system-admin/types/systemAdmin'
import { appTheme } from '../theme/appTheme'
import { TenantChangePasswordPage } from './TenantChangePasswordPage'

const session: AuthSession = {
    accessToken: 'access-token',
    csrfToken: 'csrf-token',
    persistentSession: true,
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    tenantId: 'tenant-1',
    userId: 'user-1',
    fullName: 'Grace Hopper',
    email: 'grace@example.com',
    role: 'TENANT_ADMIN',
}

const systemSession: SystemAdminSession = {
    systemAdminId: 'system-admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    role: 'SYSTEM_ADMIN',
    accessToken: 'system-access-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
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
                <AuthContext.Provider
                    value={{
                        status: 'authenticated',
                        session,
                        login: vi.fn(),
                        logout: vi.fn(),
                    }}
                >
                    <MemoryRouter initialEntries={['/account/change-password']}>
                        <Routes>
                            <Route
                                path="account/change-password"
                                element={<TenantChangePasswordPage />}
                            />
                            <Route path="account" element={<output>Account destination</output>} />
                            <Route path="login" element={<LoginPage />} />
                        </Routes>
                    </MemoryRouter>
                </AuthContext.Provider>
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

describe('TenantChangePasswordPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        localStorage.clear()
        authStorage.write(session)
        systemAdminStorage.write(systemSession)
    })

    it('changes the password, clears the tenant session, and prepares sign-in', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(authApi, 'changePassword').mockResolvedValue({
            message: 'Password changed successfully. Please login again.',
        })
        renderPage()
        fillPasswords()

        await user.click(
            screen.getByRole('button', {
                name: /^change password$/i,
            }),
        )

        expect(changePassword).toHaveBeenCalledWith({
            currentPassword: 'Current@123',
            newPassword: 'Stronger@456',
            confirmPassword: 'Stronger@456',
        })
        expect(
            await screen.findByRole('heading', {
                name: /sign in/i,
            }),
        ).toBeInTheDocument()
        expect(
            screen.getByText(/password changed successfully\. sign in with your new password/i),
        ).toBeInTheDocument()
        expect(screen.queryByLabelText(/tenant id/i)).not.toBeInTheDocument()
        expect(screen.getByLabelText(/email address/i)).toHaveValue('grace@example.com')
        expect(authStorage.read()).toBeNull()
        expect(systemAdminStorage.read()).toEqual(systemSession)
    })

    it('rejects weak, mismatched, or reused passwords before calling the API', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(authApi, 'changePassword')
        renderPage()

        fillPasswords({ newPassword: 'weak', confirmPassword: 'weak' })
        await user.click(
            screen.getByRole('button', {
                name: /^change password$/i,
            }),
        )
        expect(
            await screen.findByText(/new password must be 8–100 characters/i),
        ).toBeInTheDocument()

        fillPasswords({ confirmPassword: 'Different@456' })
        await user.click(
            screen.getByRole('button', {
                name: /^change password$/i,
            }),
        )
        expect(
            await screen.findByText(/new password and confirmation do not match/i),
        ).toBeInTheDocument()

        fillPasswords({
            currentPassword: 'Current@123',
            newPassword: 'Current@123',
            confirmPassword: 'Current@123',
        })
        await user.click(
            screen.getByRole('button', {
                name: /^change password$/i,
            }),
        )
        expect(await screen.findByText(/new password must be different/i)).toBeInTheDocument()
        expect(changePassword).not.toHaveBeenCalled()
    })

    it('shows the backend error when the current password is incorrect', async () => {
        const user = userEvent.setup()
        vi.spyOn(authApi, 'changePassword').mockRejectedValue(
            new Error('Current password is incorrect'),
        )
        renderPage()
        fillPasswords()

        await user.click(
            screen.getByRole('button', {
                name: /^change password$/i,
            }),
        )

        expect(await screen.findByText('Current password is incorrect')).toBeInTheDocument()
        expect(authStorage.read()).toEqual(session)
    })

    it('returns to account settings without submitting', async () => {
        const user = userEvent.setup()
        const changePassword = vi.spyOn(authApi, 'changePassword')
        renderPage()

        await user.click(screen.getByRole('button', { name: /cancel/i }))

        expect(await screen.findByText('Account destination')).toBeInTheDocument()
        expect(changePassword).not.toHaveBeenCalled()
    })
})
