import { afterEach, describe, expect, it, vi } from 'vitest'

import type { AuthSession } from '../types/auth'
import { authStorage } from './authStorage'

const AUTH_STORAGE_KEY = 'multitenant-saas.auth-session'

const persistentSession: AuthSession = {
    accessToken: 'access-token',
    csrfToken: 'csrf-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    persistentSession: true,
    tenantId: 'tenant-id',
    userId: 'user-id',
    fullName: 'Tenant User',
    email: 'user@example.test',
    role: 'TENANT_USER',
}

afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
})

describe('authStorage session persistence', () => {
    it('stores persistent sessions in localStorage', () => {
        authStorage.write(persistentSession)

        expect(localStorage.getItem(AUTH_STORAGE_KEY)).not.toBeNull()
        expect(sessionStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    })

    it('stores browser-session-only credentials in sessionStorage', () => {
        authStorage.write({
            ...persistentSession,
            persistentSession: false,
        })

        expect(sessionStorage.getItem(AUTH_STORAGE_KEY)).not.toBeNull()
        expect(localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull()
    })

    it('notifies subscribers when another tab writes a persistent session', () => {
        const listener = vi.fn()
        const unsubscribe = authStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: AUTH_STORAGE_KEY,
                    newValue: JSON.stringify(persistentSession),
                }),
            )

            expect(listener).toHaveBeenCalledOnce()
            expect(listener).toHaveBeenCalledWith(persistentSession)
        } finally {
            unsubscribe()
        }
    })

    it('notifies subscribers when another tab clears the session', () => {
        const listener = vi.fn()
        const unsubscribe = authStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: AUTH_STORAGE_KEY,
                    newValue: null,
                }),
            )

            expect(listener).toHaveBeenCalledOnce()
            expect(listener).toHaveBeenCalledWith(null)
        } finally {
            unsubscribe()
        }
    })
})
