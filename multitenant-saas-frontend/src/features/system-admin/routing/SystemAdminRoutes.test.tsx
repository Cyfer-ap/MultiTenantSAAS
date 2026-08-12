import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'

import { appTheme } from '../../../theme/appTheme'
import { SystemAdminContext } from '../context/SystemAdminContext'
import type { SystemAdminContextValue } from '../context/SystemAdminContext'
import { SystemProtectedRoute, SystemPublicOnlyRoute } from './SystemAdminRoutes'

const session = {
    systemAdminId: 'admin-1',
    fullName: 'Platform Owner',
    email: 'owner@example.com',
    role: 'SYSTEM_ADMIN' as const,
    accessToken: 'system-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
}

function LocationProbe() {
    const location = useLocation()
    return (
        <output aria-label="location">
            {location.pathname}
            {location.search}
        </output>
    )
}

function renderRoutes(value: SystemAdminContextValue, initialPath: string) {
    return render(
        <ThemeProvider theme={appTheme}>
            <SystemAdminContext.Provider value={value}>
                <MemoryRouter initialEntries={[initialPath]}>
                    <Routes>
                        <Route element={<SystemPublicOnlyRoute />}>
                            <Route path="system/login" element={<LocationProbe />} />
                        </Route>
                        <Route element={<SystemProtectedRoute />}>
                            <Route path="system/tenants" element={<LocationProbe />} />
                        </Route>
                        <Route path="system/dashboard" element={<LocationProbe />} />
                    </Routes>
                </MemoryRouter>
            </SystemAdminContext.Provider>
        </ThemeProvider>,
    )
}

describe('SystemAdminRoutes', () => {
    it('redirects unauthenticated system routes to system login', async () => {
        renderRoutes(
            {
                status: 'unauthenticated',
                session: null,
                login: vi.fn(),
                logout: vi.fn(),
            },
            '/system/tenants?status=ACTIVE',
        )

        expect(await screen.findByLabelText('location')).toHaveTextContent('/system/login')
    })

    it('keeps system sessions separate and redirects login to the global dashboard', async () => {
        renderRoutes(
            {
                status: 'authenticated',
                session,
                login: vi.fn(),
                logout: vi.fn(),
            },
            '/system/login',
        )

        expect(await screen.findByLabelText('location')).toHaveTextContent('/system/dashboard')
    })
})
