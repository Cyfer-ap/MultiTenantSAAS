import { ThemeProvider } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'

import { systemAdminStorage } from '../../system-admin/storage/systemAdminStorage'
import type { SystemAdminSession } from '../../system-admin/types/systemAdmin'
import { appTheme } from '../../../theme/appTheme'
import { authApi } from '../api/authApi'
import { authStorage } from '../storage/authStorage'
import type { AuthSession } from '../types/auth'
import { SessionSecurityCard } from './SessionSecurityCard'

const tenantSession: AuthSession = {
    accessToken: 'tenant-access-token',
    refreshToken: 'tenant-refresh-token',
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

interface LoginState {
    tenantId?: unknown
    email?: unknown
    allDevicesSignedOut?: unknown
}

function LoginDestination() {
    const location = useLocation()
    const state =
        typeof location.state === 'object' && location.state !== null
            ? (location.state as LoginState)
            : {}

    return (
        <section>
            <h1>Login destination</h1>
            <output aria-label="Login tenant ID">{String(state.tenantId ?? '')}</output>
            <output aria-label="Login email">{String(state.email ?? '')}</output>
            <output aria-label="Logout-all confirmation">
                {String(state.allDevicesSignedOut ?? false)}
            </output>
        </section>
    )
}

function renderCard() {
    const queryClient = new QueryClient({
        defaultOptions: {
            mutations: { retry: false },
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
                            element={
                                <SessionSecurityCard
                                    email="grace@example.com"
                                    tenantId="tenant-1"
                                />
                            }
                        />
                        <Route path="login" element={<LoginDestination />} />
                    </Routes>
                </MemoryRouter>
            </QueryClientProvider>
        </ThemeProvider>,
    )
}

async function openConfirmation() {
    const user = userEvent.setup()
    await user.click(
        screen.getByRole('button', {
            name: /sign out all devices/i,
        }),
    )

    return {
        dialog: await screen.findByRole('dialog', {
            name: /sign out all devices/i,
        }),
        user,
    }
}

describe('SessionSecurityCard', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        localStorage.clear()
        authStorage.write(tenantSession)
        systemAdminStorage.write(systemSession)
    })

    it('revokes all refresh sessions and clears only the tenant session', async () => {
        const logoutAllDevices = vi.spyOn(authApi, 'logoutAllDevices').mockResolvedValue({
            message: 'Logged out from all devices successfully',
        })
        renderCard()

        const { dialog, user } = await openConfirmation()
        expect(
            within(dialog).getByText(
                /other browsers will return to sign in on their next server request/i,
            ),
        ).toBeInTheDocument()

        await user.click(
            within(dialog).getByRole('button', {
                name: /sign out all devices/i,
            }),
        )

        expect(logoutAllDevices).toHaveBeenCalledOnce()
        expect(
            await screen.findByRole('heading', {
                name: /login destination/i,
            }),
        ).toBeInTheDocument()
        expect(screen.getByLabelText(/login tenant id/i)).toHaveTextContent('tenant-1')
        expect(screen.getByLabelText(/login email/i)).toHaveTextContent('grace@example.com')
        expect(screen.getByLabelText(/logout-all confirmation/i)).toHaveTextContent('true')
        expect(authStorage.read()).toBeNull()
        expect(systemAdminStorage.read()).toEqual(systemSession)
    })

    it('keeps sessions intact when the backend request fails', async () => {
        vi.spyOn(authApi, 'logoutAllDevices').mockRejectedValue(
            new Error('Session service unavailable'),
        )
        renderCard()

        const { dialog, user } = await openConfirmation()
        await user.click(
            within(dialog).getByRole('button', {
                name: /sign out all devices/i,
            }),
        )

        expect(await within(dialog).findByText('Session service unavailable')).toBeInTheDocument()
        expect(authStorage.read()).toEqual(tenantSession)
        expect(systemAdminStorage.read()).toEqual(systemSession)
    })

    it('cancels without calling the backend', async () => {
        const logoutAllDevices = vi.spyOn(authApi, 'logoutAllDevices')
        renderCard()

        const { dialog, user } = await openConfirmation()
        await user.click(
            within(dialog).getByRole('button', {
                name: /keep sessions/i,
            }),
        )

        await waitFor(() => {
            expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
        })
        expect(logoutAllDevices).not.toHaveBeenCalled()
        expect(authStorage.read()).toEqual(tenantSession)
    })
})
