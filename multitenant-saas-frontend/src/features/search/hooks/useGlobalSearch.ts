import { useQuery } from '@tanstack/react-query'

import { globalSearchApi } from '../api/globalSearchApi'

export const globalSearchQueryKeys = {
    all: ['global-search'] as const,
    tenant: (tenantId: string) => [...globalSearchQueryKeys.all, tenantId] as const,
    query: (tenantId: string, query: string, limit: number) =>
        [...globalSearchQueryKeys.tenant(tenantId), query, limit] as const,
}

export function useGlobalSearch(
    tenantId: string,
    query: string,
    options: { enabled?: boolean; limit?: number } = {},
) {
    const limit = options.limit ?? 12

    return useQuery({
        queryKey: globalSearchQueryKeys.query(tenantId, query, limit),
        queryFn: () => globalSearchApi.search(tenantId, query, limit),
        enabled: (options.enabled ?? true) && tenantId.length > 0 && query.trim().length >= 2,
        staleTime: 20_000,
    })
}
