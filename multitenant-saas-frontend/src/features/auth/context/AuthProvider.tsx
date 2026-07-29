import { useQueryClient } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import {
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from 'react'

import { normalizeApiError } from '../../../api/apiError'
import { authApi } from '../api/authApi'
import {
    applyCurrentUser,
    createAuthSession,
} from '../session/authSession'
import { authStorage } from '../storage/authStorage'
import type {
    AuthSession,
    CurrentUserResponse,
    LoginInput,
} from '../types/auth'
import {
    AuthContext,
    type AuthContextValue,
    type AuthStatus,
} from './AuthContext'

export function AuthProvider({
                                 children,
                             }: PropsWithChildren) {
    const queryClient = useQueryClient()

    const [status, setStatus] =
        useState<AuthStatus>('loading')

    const [session, setSession] =
        useState<AuthSession | null>(null)

    const restorationStarted = useRef(false)

    const commitSession = useCallback(
        (nextSession: AuthSession) => {
            authStorage.write(nextSession)
        },
        [],
    )

    const clearSession = useCallback(() => {
        authStorage.clear()
    }, [])

    useEffect(
        () =>
            authStorage.subscribe(
                (storedSession) => {
                    if (storedSession) {
                        setSession(storedSession)
                        setStatus('authenticated')
                        return
                    }

                    // Prevent cached data belonging to one
                    // tenant from surviving sign-out.
                    queryClient.clear()
                    setSession(null)
                    setStatus('unauthenticated')
                },
            ),
        [queryClient],
    )

    useEffect(() => {
        if (restorationStarted.current) {
            return
        }

        restorationStarted.current = true

        async function restoreSession() {
            const storedSession = authStorage.read()

            if (!storedSession) {
                clearSession()
                return
            }

            try {
                const currentUser: CurrentUserResponse =
                    await authApi.getCurrentUser()

                const validatedSession =
                    authStorage.read()

                if (!validatedSession) {
                    clearSession()
                    return
                }

                commitSession(
                    applyCurrentUser(
                        validatedSession,
                        currentUser,
                    ),
                )
            }
            catch (error: unknown) {
                const normalizedError =
                    normalizeApiError(error)

                if (
                    normalizedError.status === 401 ||
                    normalizedError.status === 403
                ) {
                    clearSession()
                    return
                }

                // Retain the locally stored session during a
                // temporary network or backend failure.
                commitSession(storedSession)
            }
        }

        void restoreSession()
    }, [
        clearSession,
        commitSession,
    ])

    const login = useCallback(
        async ({
                   tenantId,
                   email,
                   password,
               }: LoginInput): Promise<void> => {
            const response = await authApi.login(
                tenantId,
                {
                    email,
                    password,
                },
            )

            commitSession(
                createAuthSession(response),
            )
        },
        [commitSession],
    )

    const logout = useCallback(
        async (): Promise<void> => {
            const currentSession = authStorage.read()

            try {
                if (currentSession) {
                    await authApi.logout(
                        currentSession.refreshToken,
                    )
                }
            }
            finally {
                clearSession()
            }
        },
        [clearSession],
    )

    const contextValue =
        useMemo<AuthContextValue>(
            () => ({
                status,
                session,
                login,
                logout,
            }),
            [
                status,
                session,
                login,
                logout,
            ],
        )

    return (
        <AuthContext.Provider value={contextValue}>
            {children}
        </AuthContext.Provider>
    )
}
