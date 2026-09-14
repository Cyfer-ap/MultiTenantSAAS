import { useQuery } from '@tanstack/react-query'

import { myWorkApi } from '../api/myWorkApi'

export const myWorkQueryKeys = {
    all: ['my-work'] as const,
    tenant: (tenantId: string) => [...myWorkQueryKeys.all, tenantId] as const,
    overview: (tenantId: string, limit: number) =>
        [...myWorkQueryKeys.tenant(tenantId), 'overview', limit] as const,
}

export function useMyWork(tenantId: string, limit = 50) {
    return useQuery({
        queryKey: myWorkQueryKeys.overview(tenantId, limit),
        queryFn: () => myWorkApi.getOverview(tenantId, limit),
        enabled: tenantId.length > 0,
        staleTime: 15_000,
    })
}
