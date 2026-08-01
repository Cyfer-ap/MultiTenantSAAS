import { ThemeProvider } from '@mui/material'
import {
    QueryClient,
    QueryClientProvider,
} from '@tanstack/react-query'
import {
    fireEvent,
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
import { MemoryRouter } from 'react-router'

import { passwordResetApi } from '../features/password-reset/api/passwordResetApi'
import { appTheme } from '../theme/appTheme'
import { ResetPasswordPage } from './ResetPasswordPage'

function renderResetPasswordPage(
    initialEntry = '/reset-password?token=one-time-reset-token',
) {
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
                    <MemoryRouter initialEntries={[initialEntry]}>
                        {children}
                    </MemoryRouter>
                </QueryClientProvider>
            </ThemeProvider>
        )
    }

    return render(<ResetPasswordPage />, {
        wrapper: Wrapper,
    })
}

describe('ResetPasswordPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('rejects an incomplete reset link', () => {
        renderResetPasswordPage('/reset-password')

        expect(
            screen.getByText(/reset link is incomplete/i),
        ).toBeInTheDocument()
        expect(
            screen.getByRole('button', {
                name: /^reset password$/i,
            }),
        ).toBeDisabled()
    })

    it('requires matching strong passwords', async () => {
        const user = userEvent.setup()
        const resetPassword = vi.spyOn(
            passwordResetApi,
            'resetPassword',
        )

        renderResetPasswordPage()
        fireEvent.change(
            screen.getByLabelText(/^new password/i),
            {
                target: { value: 'Strong@123' },
            },
        )
        fireEvent.change(
            screen.getByLabelText(/confirm password/i),
            {
                target: { value: 'Different@123' },
            },
        )
        await user.click(
            screen.getByRole('button', {
                name: /^reset password$/i,
            }),
        )

        expect(
            await screen.findByText(/passwords do not match/i),
        ).toBeInTheDocument()
        expect(resetPassword).not.toHaveBeenCalled()
    })

    it('resets the password and returns the user to sign in', async () => {
        const user = userEvent.setup()
        const resetPassword = vi
            .spyOn(passwordResetApi, 'resetPassword')
            .mockResolvedValue({
                message: 'Password reset successfully. Please login again.',
            })

        renderResetPasswordPage()
        fireEvent.change(
            screen.getByLabelText(/^new password/i),
            {
                target: { value: 'Stronger@456' },
            },
        )
        fireEvent.change(
            screen.getByLabelText(/confirm password/i),
            {
                target: { value: 'Stronger@456' },
            },
        )
        await user.click(
            screen.getByRole('button', {
                name: /^reset password$/i,
            }),
        )

        expect(
            await screen.findByRole('heading', {
                name: /password updated/i,
            }),
        ).toBeInTheDocument()
        expect(resetPassword).toHaveBeenCalledWith({
            resetToken: 'one-time-reset-token',
            newPassword: 'Stronger@456',
            confirmPassword: 'Stronger@456',
        })
    })
})
