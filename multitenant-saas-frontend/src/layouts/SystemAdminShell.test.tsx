import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'

import { SystemAdminContext } from '../features/system-admin/context/SystemAdminContext'
import { appTheme } from '../theme/appTheme'
import { SystemAdminShell } from './SystemAdminShell'

const session = {
    systemAdminId: 'admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    role: 'SYSTEM_ADMIN' as const,
    accessToken: 'system-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
}

describe('SystemAdminShell', () => {
    it('opens the protected password page from the account menu', async () => {
        const user = userEvent.setup()

        render(
            <ThemeProvider theme={appTheme}>
                <SystemAdminContext.Provider
                    value={{
                        status: 'authenticated',
                        session,
                        login: vi.fn(),
                        logout: vi.fn(),
                    }}
                >
                    <MemoryRouter initialEntries={['/system/dashboard']}>
                        <Routes>
                            <Route path="system" element={<SystemAdminShell />}>
                                <Route
                                    path="dashboard"
                                    element={<output>Dashboard content</output>}
                                />
                                <Route
                                    path="change-password"
                                    element={<output>Password page destination</output>}
                                />
                            </Route>
                        </Routes>
                    </MemoryRouter>
                </SystemAdminContext.Provider>
            </ThemeProvider>,
        )

        await user.click(
            screen.getByRole('button', {
                name: /system administrator menu/i,
            }),
        )
        await user.click(
            screen.getByRole('menuitem', {
                name: /change password/i,
            }),
        )

        expect(await screen.findByText('Password page destination')).toBeInTheDocument()
    })
})
