import type { SystemAdminSession } from '../types/systemAdmin'

const SYSTEM_ADMIN_STORAGE_KEY = 'multitenant-saas.system-admin-session'

type SystemAdminStorageListener = (session: SystemAdminSession | null) => void

const listeners = new Set<SystemAdminStorageListener>()

let storageListenerAttached = false

function isSystemAdminSession(value: unknown): value is SystemAdminSession {
    if (typeof value !== 'object' || value === null) {
        return false
    }

    const candidate = value as Partial<SystemAdminSession>

    return (
        typeof candidate.systemAdminId === 'string' &&
        typeof candidate.fullName === 'string' &&
        typeof candidate.email === 'string' &&
        candidate.role === 'SYSTEM_ADMIN' &&
        typeof candidate.accessToken === 'string' &&
        typeof candidate.tokenType === 'string' &&
        typeof candidate.accessTokenExpiresAt === 'number'
    )
}

function notify(session: SystemAdminSession | null): void {
    listeners.forEach((listener) => {
        listener(session)
    })
}

function read(): SystemAdminSession | null {
    try {
        const storedValue = localStorage.getItem(SYSTEM_ADMIN_STORAGE_KEY)

        if (!storedValue) {
            return null
        }

        const parsedValue: unknown = JSON.parse(storedValue)

        if (!isSystemAdminSession(parsedValue)) {
            localStorage.removeItem(SYSTEM_ADMIN_STORAGE_KEY)
            notify(null)
            return null
        }

        return parsedValue
    } catch {
        localStorage.removeItem(SYSTEM_ADMIN_STORAGE_KEY)
        notify(null)
        return null
    }
}

function write(session: SystemAdminSession): void {
    localStorage.setItem(SYSTEM_ADMIN_STORAGE_KEY, JSON.stringify(session))
    notify(session)
}

function clear(): void {
    localStorage.removeItem(SYSTEM_ADMIN_STORAGE_KEY)
    notify(null)
}

function parseStoredSession(storedValue: string | null): SystemAdminSession | null {
    if (!storedValue) {
        return null
    }

    try {
        const parsedValue: unknown = JSON.parse(storedValue)

        return isSystemAdminSession(parsedValue) ? parsedValue : null
    } catch {
        return null
    }
}

function handleStorageEvent(event: StorageEvent): void {
    if (event.key !== SYSTEM_ADMIN_STORAGE_KEY) {
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

function subscribe(listener: SystemAdminStorageListener): () => void {
    listeners.add(listener)
    attachStorageListener()

    return () => {
        listeners.delete(listener)

        if (listeners.size === 0) {
            detachStorageListener()
        }
    }
}

export const systemAdminStorage = {
    read,
    write,
    clear,
    subscribe,
}
