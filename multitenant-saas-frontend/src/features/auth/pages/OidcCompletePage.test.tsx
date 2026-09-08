import { ThemeProvider } from '@mui/material'
import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'

import { appTheme } from '../../../theme/appTheme'
import { authApi } from '../api/authApi'
import { authStorage } from '../storage/authStorage'
import type { LoginResponse } from '../types/auth'
import { OidcCompletePage } from './OidcCompletePage'

const loginResponse: LoginResponse = {
    tenantId: 'tenant-1',
    userId: 'user-1',
    fullName: 'Grace Hopper',
    email: 'grace@example.com',
    role: 'TENANT_USER',
    accessToken: 'access-token',
    refreshToken: null,
    csrfToken: 'csrf-token',
    tokenType: 'Bearer',
    expiresInSeconds: 3600,
    persistentSession: false,
    message: 'Login successful',
}

function renderCompletion(initialEntry: string) {
    return render(
        <ThemeProvider theme={appTheme}>
            <MemoryRouter initialEntries={[initialEntry]}>
                <Routes>
                    <Route path="/auth/oidc/complete" element={<OidcCompletePage />} />
                    <Route path="/dashboard" element={<div>Dashboard reached</div>} />
                    <Route path="/login" element={<div>Login reached</div>} />
                </Routes>
            </MemoryRouter>
        </ThemeProvider>,
    )
}

describe('OidcCompletePage', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
        window.localStorage.clear()
        window.sessionStorage.clear()
    })

    it('exchanges a valid handoff exactly once, commits the session, and redirects', async () => {
        const completeOidcLogin = vi
            .spyOn(authApi, 'completeOidcLogin')
            .mockResolvedValue(loginResponse)

        renderCompletion('/auth/oidc/complete?code=opaque-handoff')

        expect(await screen.findByText('Dashboard reached')).toBeInTheDocument()
        expect(completeOidcLogin).toHaveBeenCalledTimes(1)
        expect(completeOidcLogin).toHaveBeenCalledWith('opaque-handoff')
        expect(authStorage.read()).toMatchObject({
            tenantId: 'tenant-1',
            userId: 'user-1',
            accessToken: 'access-token',
        })
    })

    it('shows a clean error when a handoff is invalid or expired', async () => {
        vi.spyOn(authApi, 'completeOidcLogin').mockRejectedValue(
            new Error('OIDC session handoff is invalid or expired'),
        )

        renderCompletion('/auth/oidc/complete?code=expired-handoff')

        expect(
            await screen.findByText(/oidc session handoff is invalid or expired/i),
        ).toBeInTheDocument()
        expect(screen.getByRole('button', { name: /return to sign in/i })).toBeInTheDocument()
        expect(authStorage.read()).toBeNull()
    })

    it('rejects a completion request without a handoff code', async () => {
        const completeOidcLogin = vi.spyOn(authApi, 'completeOidcLogin')

        renderCompletion('/auth/oidc/complete')

        expect(await screen.findByText(/session is missing or invalid/i)).toBeInTheDocument()
        expect(completeOidcLogin).not.toHaveBeenCalled()
    })
})
