import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { PropsWithChildren } from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'

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

    it('validates the tenant and email before requesting a token', async () => {
        const user = userEvent.setup()
        const forgotPassword = vi.spyOn(passwordResetApi, 'forgotPassword')

        renderForgotPasswordPage()
        fireEvent.change(screen.getByLabelText(/email address/i), {
            target: { value: 'not-an-email' },
        })
        await user.click(
            screen.getByRole('button', {
                name: /request reset link/i,
            }),
        )

        expect(await screen.findByText(/tenant id is required/i)).toBeInTheDocument()
        expect(forgotPassword).not.toHaveBeenCalled()
    })

    it('normalizes the request and exposes the development reset link', async () => {
        const user = userEvent.setup()
        const forgotPassword = vi.spyOn(passwordResetApi, 'forgotPassword').mockResolvedValue({
            message: 'Reset token generated.',
            devResetToken: 'one-time-reset-token',
        })

        renderForgotPasswordPage()
        fireEvent.change(screen.getByLabelText(/tenant id/i), {
            target: { value: '  tenant-1  ' },
        })
        fireEvent.change(screen.getByLabelText(/email address/i), {
            target: { value: '  GRACE@EXAMPLE.COM  ' },
        })
        await user.click(
            screen.getByRole('button', {
                name: /request reset link/i,
            }),
        )

        const resetLink = (await screen.findByLabelText(/reset link/i)) as HTMLInputElement

        expect(forgotPassword).toHaveBeenCalledWith('tenant-1', { email: 'grace@example.com' })
        expect(resetLink.value).toMatch(/\/reset-password\?token=one-time-reset-token$/)
    })
})
