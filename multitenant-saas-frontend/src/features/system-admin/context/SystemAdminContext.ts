import { createContext } from 'react'

import type { SystemAdminLoginInput, SystemAdminSession } from '../types/systemAdmin'

export type SystemAdminAuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface SystemAdminContextValue {
    status: SystemAdminAuthStatus
    session: SystemAdminSession | null
    login: (input: SystemAdminLoginInput) => Promise<void>
    logout: () => void
}

export const SystemAdminContext = createContext<SystemAdminContextValue | undefined>(undefined)
