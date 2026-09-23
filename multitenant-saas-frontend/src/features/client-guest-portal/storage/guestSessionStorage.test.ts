import { beforeEach, describe, expect, it } from 'vitest'

import { guestSessionStorage } from './guestSessionStorage'

beforeEach(() => {
    window.sessionStorage.clear()
})

describe('guestSessionStorage', () => {
    it('stores the guest session separately from tenant authentication state', () => {
        window.sessionStorage.setItem('unrelated-auth-session', 'tenant-session')

        guestSessionStorage.write('guest-session')

        expect(guestSessionStorage.read()).toBe('guest-session')
        expect(window.sessionStorage.getItem('unrelated-auth-session')).toBe('tenant-session')
    })

    it('clears only the guest portal session', () => {
        window.sessionStorage.setItem('unrelated-auth-session', 'tenant-session')
        guestSessionStorage.write('guest-session')

        guestSessionStorage.clear()

        expect(guestSessionStorage.read()).toBeNull()
        expect(window.sessionStorage.getItem('unrelated-auth-session')).toBe('tenant-session')
    })
})
