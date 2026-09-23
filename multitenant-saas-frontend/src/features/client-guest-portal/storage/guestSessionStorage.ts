const SESSION_STORAGE_KEY = 'multitenant-saas.guest-portal.session'

function read(): string | null {
    if (typeof window === 'undefined') return null
    return window.sessionStorage.getItem(SESSION_STORAGE_KEY)
}

function write(sessionToken: string): void {
    if (typeof window === 'undefined') return
    window.sessionStorage.setItem(SESSION_STORAGE_KEY, sessionToken)
}

function clear(): void {
    if (typeof window === 'undefined') return
    window.sessionStorage.removeItem(SESSION_STORAGE_KEY)
}

export const guestSessionStorage = { read, write, clear }
