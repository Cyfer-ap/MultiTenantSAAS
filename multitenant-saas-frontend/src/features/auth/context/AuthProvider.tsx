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
import { authStorage } from '../storage/authStorage'
import type {
    AuthSession,
    CurrentUserResponse,
    LoginInput,
    LoginResponse,
    TokenRefreshResponse,
} from '../types/auth'
import {
    AuthContext,
    type AuthContextValue,
    type AuthStatus,
} from './AuthContext'

function calculateExpirationTime(
    expiresInSeconds: number,
): number {
    return (
        Date.now() +
        Math.max(expiresInSeconds, 0) * 1_000
    )
}

function createSession(
    response: LoginResponse,
): AuthSession {
    return {
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        accessTokenExpiresAt:
            calculateExpirationTime(
                response.expiresInSeconds,
            ),
        tenantId: response.tenantId,
        userId: response.userId,
        fullName: response.fullName,
        email: response.email,
        role: response.role,
    }
}

function applyTokenRefresh(
    session: AuthSession,
    response: TokenRefreshResponse,
): AuthSession {
    return {
        ...session,
        accessToken: response.accessToken,
        refreshToken: response.refreshToken,
        tokenType: response.tokenType,
        accessTokenExpiresAt:
            calculateExpirationTime(
                response.expiresInSeconds,
            ),
    }
}

function applyCurrentUser(
    session: AuthSession,
    user: CurrentUserResponse,
): AuthSession {
    return {
        ...session,
        tenantId: user.tenantId,
        userId: user.userId,
        fullName: user.fullName,
        email: user.email,
        role: user.role,
    }
}

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
            setSession(nextSession)
            setStatus('authenticated')
        },
        [],
    )

    const clearSession = useCallback(() => {
        authStorage.clear()

        // Prevent cached data belonging to one tenant
        // from being visible after another tenant logs in.
        queryClient.clear()

        setSession(null)
        setStatus('unauthenticated')
    }, [queryClient])

    const refreshSession = useCallback(
        async (
            currentSession: AuthSession,
        ): Promise<AuthSession> => {
            const response =
                await authApi.refreshToken(
                    currentSession.refreshToken,
                )

            return applyTokenRefresh(
                currentSession,
                response,
            )
        },
        [],
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
                let candidateSession = storedSession
                let tokenWasRefreshed = false

                if (
                    candidateSession.accessTokenExpiresAt <=
                    Date.now()
                ) {
                    candidateSession =
                        await refreshSession(candidateSession)

                    authStorage.write(candidateSession)
                    tokenWasRefreshed = true
                }

                let currentUser: CurrentUserResponse

                try {
                    currentUser =
                        await authApi.getCurrentUser()
                }
                catch (error: unknown) {
                    const normalizedError =
                        normalizeApiError(error)

                    if (
                        normalizedError.status !== 401 ||
                        tokenWasRefreshed
                    ) {
                        throw normalizedError
                    }

                    candidateSession =
                        await refreshSession(candidateSession)

                    authStorage.write(candidateSession)

                    currentUser =
                        await authApi.getCurrentUser()
                }

                commitSession(
                    applyCurrentUser(
                        candidateSession,
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
        refreshSession,
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

            commitSession(createSession(response))
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