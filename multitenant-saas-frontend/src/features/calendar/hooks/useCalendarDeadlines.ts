import { useQuery } from '@tanstack/react-query'

import { calendarApi } from '../api/calendarApi'

export const calendarQueryKeys = {
    all: ['calendar'] as const,
    tenant: (tenantId: string) => [...calendarQueryKeys.all, tenantId] as const,
    deadlines: (tenantId: string, from: string, to: string, limit: number) =>
        [...calendarQueryKeys.tenant(tenantId), 'deadlines', from, to, limit] as const,
}

export function useCalendarDeadlines(
    tenantId: string,
    from: string,
    to: string,
    limit = 500,
) {
    return useQuery({
        queryKey: calendarQueryKeys.deadlines(tenantId, from, to, limit),
        queryFn: () => calendarApi.getDeadlines(tenantId, from, to, limit),
        enabled: tenantId.length > 0 && from.length > 0 && to.length > 0,
        staleTime: 15_000,
    })
}
