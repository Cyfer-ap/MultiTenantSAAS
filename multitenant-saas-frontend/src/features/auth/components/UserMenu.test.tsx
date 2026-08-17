import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'

import { appTheme } from '../../../theme/appTheme'
import { AuthContext } from '../context/AuthContext'
import { UserMenu } from './UserMenu'

const session = {
    accessToken: 'access-token',
    csrfToken: 'csrf-token',
    persistentSession: true,
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    tenantId: 'tenant-1',
    userId: 'user-1',
    fullName: 'Grace Hopper',
    email: 'grace@example.com',
    role: 'TENANT_ADMIN' as const,
}

describe('UserMenu', () => {
    it('opens tenant account settings from the account menu', async () => {
        const user = userEvent.setup()

        render(
            <ThemeProvider theme={appTheme}>
                <AuthContext.Provider
                    value={{
                        status: 'authenticated',
                        session,
                        login: vi.fn(),
                        logout: vi.fn(),
                    }}
                >
                    <MemoryRouter initialEntries={['/dashboard']}>
                        <Routes>
                            <Route path="dashboard" element={<UserMenu />} />
                            <Route
                                path="account"
                                element={<output>Account settings destination</output>}
                            />
                        </Routes>
                    </MemoryRouter>
                </AuthContext.Provider>
            </ThemeProvider>,
        )

        await user.click(
            screen.getByRole('button', {
                name: /grace hopper/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /account settings/i,
            }),
        )

        expect(await screen.findByText('Account settings destination')).toBeInTheDocument()
    })
})
