import {
    keepPreviousData,
    useQuery,
} from '@tanstack/react-query'

import { usersApi } from '../api/usersApi'
import type { TenantUsersQueryParams } from '../types/users'

export const tenantUsersQueryKeys = {
    all: ['tenant-users'] as const,
    tenant: (tenantId: string) => [
        ...tenantUsersQueryKeys.all,
        tenantId,
    ] as const,
    list: (
        tenantId: string,
        params: TenantUsersQueryParams,
    ) => [
        ...tenantUsersQueryKeys.tenant(tenantId),
        params,
    ] as const,
}

export function useTenantUsers(
    tenantId: string,
    params: TenantUsersQueryParams,
) {
    return useQuery({
        queryKey: tenantUsersQueryKeys.list(
            tenantId,
            params,
        ),
        queryFn: () =>
            usersApi.getUsers(tenantId, params),
        enabled: tenantId.length > 0,
        placeholderData: keepPreviousData,
    })
}
