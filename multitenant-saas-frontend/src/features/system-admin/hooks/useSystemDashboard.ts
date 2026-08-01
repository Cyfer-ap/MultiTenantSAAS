import { useQuery } from '@tanstack/react-query'

import { systemAdminApi } from '../api/systemAdminApi'

export const systemDashboardQueryKey = [
    'system-admin',
    'dashboard',
] as const

export function useSystemDashboard() {
    return useQuery({
        queryKey: systemDashboardQueryKey,
        queryFn: systemAdminApi.getDashboardSummary,
    })
}
