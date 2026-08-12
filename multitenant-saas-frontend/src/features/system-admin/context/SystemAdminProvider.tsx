import { useQueryClient } from '@tanstack/react-query'
import type { PropsWithChildren } from 'react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'

import { normalizeApiError } from '../../../api/apiError'
import { systemAdminApi } from '../api/systemAdminApi'
import { applyCurrentSystemAdmin, createSystemAdminSession } from '../session/systemAdminSession'
import { systemAdminStorage } from '../storage/systemAdminStorage'
import type { SystemAdminLoginInput, SystemAdminSession } from '../types/systemAdmin'
import {
    SystemAdminContext,
    type SystemAdminAuthStatus,
    type SystemAdminContextValue,
} from './SystemAdminContext'

export function SystemAdminProvider({ children }: PropsWithChildren) {
    const queryClient = useQueryClient()
    const [status, setStatus] = useState<SystemAdminAuthStatus>('loading')
    const [session, setSession] = useState<SystemAdminSession | null>(null)
    const restorationStarted = useRef(false)

    const commitSession = useCallback((nextSession: SystemAdminSession) => {
        systemAdminStorage.write(nextSession)
    }, [])

    const clearSession = useCallback(() => {
        systemAdminStorage.clear()
    }, [])

    useEffect(
        () =>
            systemAdminStorage.subscribe((storedSession) => {
                if (storedSession) {
                    setSession(storedSession)
                    setStatus('authenticated')
                    return
                }

                void queryClient.removeQueries({
                    queryKey: ['system-admin'],
                })
                setSession(null)
                setStatus('unauthenticated')
            }),
        [queryClient],
    )

    useEffect(() => {
        if (restorationStarted.current) {
            return
        }

        restorationStarted.current = true

        async function restoreSession() {
            const storedSession = systemAdminStorage.read()

            if (!storedSession) {
                clearSession()
                return
            }

            try {
                const currentAdmin = await systemAdminApi.getCurrentAdmin()
                const validatedSession = systemAdminStorage.read()

                if (!validatedSession) {
                    clearSession()
                    return
                }

                commitSession(applyCurrentSystemAdmin(validatedSession, currentAdmin))
            } catch (error: unknown) {
                const normalizedError = normalizeApiError(error)

                if (normalizedError.status === 401 || normalizedError.status === 403) {
                    clearSession()
                    return
                }

                commitSession(storedSession)
            }
        }

        void restoreSession()
    }, [clearSession, commitSession])

    const login = useCallback(
        async (input: SystemAdminLoginInput) => {
            const response = await systemAdminApi.login(input)
            commitSession(createSystemAdminSession(response))
        },
        [commitSession],
    )

    const logout = useCallback(() => {
        clearSession()
    }, [clearSession])

    const contextValue = useMemo<SystemAdminContextValue>(
        () => ({
            status,
            session,
            login,
            logout,
        }),
        [status, session, login, logout],
    )

    return (
        <SystemAdminContext.Provider value={contextValue}>{children}</SystemAdminContext.Provider>
    )
}
