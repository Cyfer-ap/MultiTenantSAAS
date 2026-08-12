import { createContext } from 'react'

import type { AuthSession, LoginInput } from '../types/auth'

export type AuthStatus = 'loading' | 'authenticated' | 'unauthenticated'

export interface AuthContextValue {
    status: AuthStatus
    session: AuthSession | null
    login: (input: LoginInput) => Promise<void>
    logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | undefined>(undefined)
