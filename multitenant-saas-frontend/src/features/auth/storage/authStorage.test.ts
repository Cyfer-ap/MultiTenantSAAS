import { afterEach, describe, expect, it, vi } from 'vitest'

import type { AuthSession } from '../types/auth'
import { authStorage } from './authStorage'

const AUTH_STORAGE_KEY =
    'multitenant-saas.auth-session'

const session: AuthSession = {
    accessToken: 'access-token',
    refreshToken: 'refresh-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
    tenantId: 'tenant-id',
    userId: 'user-id',
    fullName: 'Tenant User',
    email: 'user@example.test',
    role: 'TENANT_USER',
}

afterEach(() => {
    localStorage.clear()
})

describe('authStorage cross-tab synchronization', () => {
    it('notifies subscribers when another tab writes a session', () => {
        const listener = vi.fn()
        const unsubscribe = authStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: AUTH_STORAGE_KEY,
                    newValue: JSON.stringify(session),
                }),
            )

            expect(listener).toHaveBeenCalledOnce()
            expect(listener).toHaveBeenCalledWith(session)
        }
        finally {
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
        }
        finally {
            unsubscribe()
        }
    })

    it('ignores unrelated storage keys', () => {
        const listener = vi.fn()
        const unsubscribe = authStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: 'unrelated-key',
                    newValue: JSON.stringify(session),
                }),
            )

            expect(listener).not.toHaveBeenCalled()
        }
        finally {
            unsubscribe()
        }
    })
})
