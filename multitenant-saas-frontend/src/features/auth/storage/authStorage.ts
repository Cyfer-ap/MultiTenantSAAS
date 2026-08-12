import type { AuthSession, TenantRole } from '../types/auth'

const AUTH_STORAGE_KEY = 'multitenant-saas.auth-session'

type AuthStorageListener = (session: AuthSession | null) => void

const listeners = new Set<AuthStorageListener>()

let storageListenerAttached = false

const tenantRoles: TenantRole[] = ['TENANT_ADMIN', 'TENANT_MANAGER', 'TENANT_USER']

function isTenantRole(value: unknown): value is TenantRole {
    return typeof value === 'string' && tenantRoles.includes(value as TenantRole)
}

function isAuthSession(value: unknown): value is AuthSession {
    if (typeof value !== 'object' || value === null) {
        return false
    }

    const candidate = value as Partial<AuthSession>

    return (
        typeof candidate.accessToken === 'string' &&
        typeof candidate.refreshToken === 'string' &&
        typeof candidate.tokenType === 'string' &&
        typeof candidate.accessTokenExpiresAt === 'number' &&
        typeof candidate.tenantId === 'string' &&
        typeof candidate.userId === 'string' &&
        typeof candidate.fullName === 'string' &&
        typeof candidate.email === 'string' &&
        isTenantRole(candidate.role)
    )
}

function read(): AuthSession | null {
    try {
        const storedValue = localStorage.getItem(AUTH_STORAGE_KEY)

        if (!storedValue) {
            return null
        }

        const parsedValue: unknown = JSON.parse(storedValue)

        if (!isAuthSession(parsedValue)) {
            localStorage.removeItem(AUTH_STORAGE_KEY)
            notify(null)
            return null
        }

        return parsedValue
    } catch {
        localStorage.removeItem(AUTH_STORAGE_KEY)
        notify(null)
        return null
    }
}

function write(session: AuthSession): void {
    localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))

    notify(session)
}

function clear(): void {
    localStorage.removeItem(AUTH_STORAGE_KEY)
    notify(null)
}

function notify(session: AuthSession | null): void {
    listeners.forEach((listener) => {
        listener(session)
    })
}

function parseStoredSession(storedValue: string | null): AuthSession | null {
    if (!storedValue) {
        return null
    }

    try {
        const parsedValue: unknown = JSON.parse(storedValue)

        return isAuthSession(parsedValue) ? parsedValue : null
    } catch {
        return null
    }
}

function handleStorageEvent(event: StorageEvent): void {
    if (event.key !== AUTH_STORAGE_KEY) {
        return
    }

    notify(parseStoredSession(event.newValue))
}

function attachStorageListener(): void {
    if (storageListenerAttached || typeof window === 'undefined') {
        return
    }

    window.addEventListener('storage', handleStorageEvent)
    storageListenerAttached = true
}

function detachStorageListener(): void {
    if (!storageListenerAttached || typeof window === 'undefined') {
        return
    }

    window.removeEventListener('storage', handleStorageEvent)
    storageListenerAttached = false
}

function subscribe(listener: AuthStorageListener): () => void {
    listeners.add(listener)
    attachStorageListener()

    return () => {
        listeners.delete(listener)

        if (listeners.size === 0) {
            detachStorageListener()
        }
    }
}

export const authStorage = {
    read,
    write,
    clear,
    subscribe,
}
