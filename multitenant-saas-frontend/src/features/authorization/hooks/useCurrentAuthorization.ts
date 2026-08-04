import { useQuery } from '@tanstack/react-query'

import type { ApiClientError } from '../../../api/apiError'
import { useAuth } from '../../auth/hooks/useAuth'
import { authorizationApi } from '../api/authorizationApi'
import type { CurrentAuthorizationContext } from '../types/authorization'

export const currentAuthorizationQueryKeys = {
    all: ['current-authorization'] as const,

    current: (
        tenantId: string,
        userId: string,
    ) => [
        ...currentAuthorizationQueryKeys.all,
        tenantId,
        userId,
    ] as const,
}

export function useCurrentAuthorization() {
    const { session, status } = useAuth()

    const tenantId = session?.tenantId ?? ''
    const userId = session?.userId ?? ''

    return useQuery<
        CurrentAuthorizationContext,
        ApiClientError
    >({
        queryKey:
            currentAuthorizationQueryKeys.current(
                tenantId,
                userId,
            ),

        queryFn: () =>
            authorizationApi
                .getCurrentAuthorizationContext(
                    tenantId,
                ),

        enabled:
            status === 'authenticated' &&
            tenantId.length > 0 &&
            userId.length > 0,

        staleTime: 30_000,

        retry: (
            failureCount,
            error,
        ) => {
            if (
                error.status === 401 ||
                error.status === 403
            ) {
                return false
            }

            return failureCount < 2
        },
    })
}
