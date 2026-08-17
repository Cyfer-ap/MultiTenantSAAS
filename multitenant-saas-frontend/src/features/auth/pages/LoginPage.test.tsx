import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'

import { appTheme } from '../../../theme/appTheme'
import { authApi } from '../api/authApi'
import { AuthContext } from '../context/AuthContext'
import { LoginPage } from './LoginPage'

function renderLoginPage(state: Record<string, unknown> = {}) {
    return render(
        <ThemeProvider theme={appTheme}>
            <AuthContext.Provider
                value={{
                    status: 'unauthenticated',
                    session: null,
                    login: vi.fn(),
                    logout: vi.fn(),
                }}
            >
                <MemoryRouter
                    initialEntries={[
                        {
                            pathname: '/login',
                            state,
                        },
                    ]}
                >
                    <LoginPage />
                </MemoryRouter>
            </AuthContext.Provider>
        </ThemeProvider>,
    )
}

describe('LoginPage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        window.localStorage.clear()
    })

    it('prefills the email without exposing the tenant UUID field', () => {
        renderLoginPage({
            tenantId: 'tenant-1',
            email: 'grace@example.com',
        })

        expect(screen.getByLabelText(/email address/i)).toHaveValue('grace@example.com')
        expect(screen.queryByLabelText(/tenant id/i)).not.toBeInTheDocument()
    })

    it('confirms device-session revocation and preserves the email context', () => {
        renderLoginPage({
            email: 'grace@example.com',
            allDevicesSignedOut: true,
        })

        expect(screen.getByText(/all device refresh sessions were revoked/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/email address/i)).toHaveValue('grace@example.com')
    })

    it('moves from email to verification code when the browser is not trusted', async () => {
        const user = userEvent.setup()

        vi.spyOn(authApi, 'startWorkspaceDiscovery').mockResolvedValue({
            verificationRequired: true,
            challengeId: 'challenge-1',
            workspaces: [],
            expiresInSeconds: 600,
            message: 'Check your email.',
        })

        renderLoginPage()

        await user.type(screen.getByLabelText(/email address/i), 'grace@example.com')
        await user.click(screen.getByRole('button', { name: /^continue$/i }))

        expect(await screen.findByLabelText(/verification code/i)).toBeInTheDocument()
        expect(screen.getByText(/grace@example.com/i)).toBeInTheDocument()
    })

    it('skips the code step for a trusted browser and shows the password for one workspace', async () => {
        const user = userEvent.setup()

        window.localStorage.setItem('multitenant-saas.trusted-email-browser', 'trusted-token')

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
            expiresInSeconds: 0,
            message: 'Trusted browser.',
        })

        renderLoginPage({ email: 'grace@example.com' })

        await user.click(screen.getByRole('button', { name: /^continue$/i }))

        expect(await screen.findByLabelText(/password/i)).toBeInTheDocument()
        expect(screen.getByText(/workspace: research lab/i)).toBeInTheDocument()
    })
})
