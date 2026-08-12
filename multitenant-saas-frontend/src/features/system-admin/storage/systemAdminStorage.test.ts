import { afterEach, describe, expect, it, vi } from 'vitest'

import type { SystemAdminSession } from '../types/systemAdmin'
import { systemAdminStorage } from './systemAdminStorage'

const SYSTEM_ADMIN_STORAGE_KEY = 'multitenant-saas.system-admin-session'

const session: SystemAdminSession = {
    systemAdminId: 'system-admin-id',
    fullName: 'System Administrator',
    email: 'admin@example.test',
    role: 'SYSTEM_ADMIN',
    accessToken: 'access-token',
    tokenType: 'Bearer',
    accessTokenExpiresAt: Date.now() + 60_000,
}

afterEach(() => {
    localStorage.clear()
})

describe('systemAdminStorage cross-tab synchronization', () => {
    it('notifies subscribers when another tab writes a session', () => {
        const listener = vi.fn()
        const unsubscribe = systemAdminStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: SYSTEM_ADMIN_STORAGE_KEY,
                    newValue: JSON.stringify(session),
                }),
            )

            expect(listener).toHaveBeenCalledOnce()
            expect(listener).toHaveBeenCalledWith(session)
        } finally {
            unsubscribe()
        }
    })

    it('notifies subscribers when another tab clears the session', () => {
        const listener = vi.fn()
        const unsubscribe = systemAdminStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: SYSTEM_ADMIN_STORAGE_KEY,
                    newValue: null,
                }),
            )

            expect(listener).toHaveBeenCalledOnce()
            expect(listener).toHaveBeenCalledWith(null)
        } finally {
            unsubscribe()
        }
    })

    it('ignores unrelated storage keys', () => {
        const listener = vi.fn()
        const unsubscribe = systemAdminStorage.subscribe(listener)

        try {
            window.dispatchEvent(
                new StorageEvent('storage', {
                    key: 'unrelated-key',
                    newValue: JSON.stringify(session),
                }),
            )

            expect(listener).not.toHaveBeenCalled()
        } finally {
            unsubscribe()
        }
    })
})
