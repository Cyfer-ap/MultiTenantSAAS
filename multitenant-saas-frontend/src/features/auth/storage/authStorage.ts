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
        typeof candidate.csrfToken === 'string' &&
        typeof candidate.tokenType === 'string' &&
        typeof candidate.accessTokenExpiresAt === 'number' &&
        typeof candidate.persistentSession === 'boolean' &&
        typeof candidate.tenantId === 'string' &&
        typeof candidate.userId === 'string' &&
        typeof candidate.fullName === 'string' &&
        typeof candidate.email === 'string' &&
        isTenantRole(candidate.role)
    )
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

function read(): AuthSession | null {
    try {
        const sessionValue = sessionStorage.getItem(AUTH_STORAGE_KEY)
        const session = parseStoredSession(sessionValue)

        if (session) {
            return session
        }

        if (sessionValue) {
            sessionStorage.removeItem(AUTH_STORAGE_KEY)
        }

        const persistentValue = localStorage.getItem(AUTH_STORAGE_KEY)
        const persistentSession = parseStoredSession(persistentValue)

        if (persistentSession) {
            return persistentSession
        }

        if (persistentValue) {
            localStorage.removeItem(AUTH_STORAGE_KEY)
        }

        return null
    } catch {
        clearStorageOnly()
        return null
    }
}

function write(session: AuthSession): void {
    try {
        sessionStorage.removeItem(AUTH_STORAGE_KEY)
        localStorage.removeItem(AUTH_STORAGE_KEY)

        const targetStorage = session.persistentSession ? localStorage : sessionStorage
        targetStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session))
    } finally {
        notify(session)
    }
}

function clearStorageOnly(): void {
    try {
        sessionStorage.removeItem(AUTH_STORAGE_KEY)
    } catch {
        // Ignore unavailable browser storage.
    }

    try {
        localStorage.removeItem(AUTH_STORAGE_KEY)
    } catch {
        // Ignore unavailable browser storage.
    }
}

function clear(): void {
    clearStorageOnly()
    notify(null)
}

function notify(session: AuthSession | null): void {
    listeners.forEach((listener) => {
        listener(session)
    })
}

function handleStorageEvent(event: StorageEvent): void {
    if (event.key !== AUTH_STORAGE_KEY) {
        return
    }

    try {
        sessionStorage.removeItem(AUTH_STORAGE_KEY)
    } catch {
        // Ignore unavailable browser storage.
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
