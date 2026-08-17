import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'

import { authApi } from '../features/auth/api/authApi'
import { passwordResetApi } from '../features/password-reset/api/passwordResetApi'
import { appTheme } from '../theme/appTheme'
import { ForgotPasswordPage } from './ForgotPasswordPage'

function renderForgotPasswordPage() {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: {
                retry: false,
            },
        },
    })

    function Wrapper({ children }: PropsWithChildren) {
        return (
            <ThemeProvider theme={appTheme}>
                <QueryClientProvider client={queryClient}>
                    <MemoryRouter>{children}</MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<ForgotPasswordPage />, {
        wrapper: Wrapper,
    })
}

describe('ForgotPasswordPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('requires only a valid email to start recovery', async () => {
        const user = userEvent.setup()
        const startDiscovery = vi.spyOn(authApi, 'startWorkspaceDiscovery')

        renderForgotPasswordPage()

        fireEvent.change(screen.getByLabelText(/email address/i), {
            target: { value: 'not-an-email' },
        })
        await user.click(screen.getByRole('button', { name: /^continue$/i }))

        expect(await screen.findByText(/enter a valid email address/i)).toBeInTheDocument()
        expect(screen.queryByLabelText(/tenant id/i)).not.toBeInTheDocument()
        expect(startDiscovery).not.toHaveBeenCalled()
    })

    it('moves through OTP verification before showing workspaces', async () => {
        const user = userEvent.setup()

        vi.spyOn(authApi, 'startWorkspaceDiscovery').mockResolvedValue({
            verificationRequired: true,
            challengeId: 'challenge-1',
            workspaces: [],
            workspaceGrantId: null,
            expiresInSeconds: 600,
            message: 'Check your email.',
        })

        vi.spyOn(authApi, 'verifyWorkspaceDiscovery').mockResolvedValue({
            workspaces: [
                {
                    tenantId: 'tenant-1',
                    name: 'Research Lab',
                    slug: 'research-lab',
                },
            ],
            workspaceGrantId: 'grant-1',
            trustedBrowserToken: null,
            message: 'Email verified successfully.',
        })

        renderForgotPasswordPage()

        await user.type(screen.getByLabelText(/email address/i), 'GRACE@EXAMPLE.COM')
        await user.click(screen.getByRole('button', { name: /^continue$/i }))

        await user.type(await screen.findByLabelText(/verification code/i), '123456')
        await user.click(screen.getByRole('button', { name: /verify email/i }))

        expect(
            await screen.findByRole('button', {
                name: /send reset link for research lab/i,
            }),
        ).toBeInTheDocument()

        expect(authApi.verifyWorkspaceDiscovery).toHaveBeenCalledWith({
            challengeId: 'challenge-1',
            code: '123456',
            trustBrowser: true,
        })
    })

    it('trusted-browser recovery sends a verified workspace reset request', async () => {
        const user = userEvent.setup()

        vi.spyOn(authApi, 'startWorkspaceDiscovery').mockResolvedValue({
            verificationRequired: false,
            challengeId: null,
            workspaces: [
                {
                    tenantId: 'tenant-1',
                    name: 'Research Lab',
                    slug: 'research-lab',
                },
            ],
            workspaceGrantId: 'grant-1',
            expiresInSeconds: 0,
            message: 'Trusted browser.',
        })

        const forgotPassword = vi.spyOn(passwordResetApi, 'forgotPassword').mockResolvedValue({
            message: 'Password reset instructions will be sent.',
            devResetToken: null,
        })

        renderForgotPasswordPage()

        await user.type(screen.getByLabelText(/email address/i), ' Grace@Example.com ')
        await user.click(screen.getByRole('button', { name: /^continue$/i }))
        await user.click(
            await screen.findByRole('button', {
                name: /send reset link for research lab/i,
            }),
        )

        expect(forgotPassword).toHaveBeenCalledWith({
            tenantId: 'tenant-1',
            email: 'grace@example.com',
            workspaceGrantId: 'grant-1',
        })

        expect(await screen.findByText(/check your email/i)).toBeInTheDocument()
    })
})
