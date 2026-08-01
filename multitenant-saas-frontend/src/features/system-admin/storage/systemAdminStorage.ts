import type { SystemAdminSession } from '../types/systemAdmin'

const SYSTEM_ADMIN_STORAGE_KEY =
    'multitenant-saas.system-admin-session'

type SystemAdminStorageListener = (
    session: SystemAdminSession | null,
) => void

const listeners = new Set<SystemAdminStorageListener>()

function isSystemAdminSession(
    value: unknown,
): value is SystemAdminSession {
    if (
        typeof value !== 'object' ||
        value === null
    ) {
        return false
    }

    const candidate =
        value as Partial<SystemAdminSession>

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

function notify(
    session: SystemAdminSession | null,
): void {
    listeners.forEach((listener) => {
        listener(session)
    })
}

function read(): SystemAdminSession | null {
    try {
        const storedValue = localStorage.getItem(
            SYSTEM_ADMIN_STORAGE_KEY,
        )

        if (!storedValue) {
            return null
        }

        const parsedValue: unknown =
            JSON.parse(storedValue)

        if (!isSystemAdminSession(parsedValue)) {
            localStorage.removeItem(
                SYSTEM_ADMIN_STORAGE_KEY,
            )
            notify(null)
            return null
        }

        return parsedValue
    }
    catch {
        localStorage.removeItem(
            SYSTEM_ADMIN_STORAGE_KEY,
        )
        notify(null)
        return null
    }
}

function write(session: SystemAdminSession): void {
    localStorage.setItem(
        SYSTEM_ADMIN_STORAGE_KEY,
        JSON.stringify(session),
    )
    notify(session)
}

function clear(): void {
    localStorage.removeItem(SYSTEM_ADMIN_STORAGE_KEY)
    notify(null)
}

function subscribe(
    listener: SystemAdminStorageListener,
): () => void {
    listeners.add(listener)

    return () => {
        listeners.delete(listener)
    }
}

export const systemAdminStorage = {
    read,
    write,
    clear,
    subscribe,
}
