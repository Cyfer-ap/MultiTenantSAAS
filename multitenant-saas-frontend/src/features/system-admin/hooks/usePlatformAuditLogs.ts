import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { systemAdminApi } from '../api/systemAdminApi'
import type { PlatformAuditLogsQueryParams } from '../types/systemAdmin'

export const platformAuditLogQueryKeys = {
    all: ['system-admin', 'platform-audit-logs'] as const,
    list: (params: PlatformAuditLogsQueryParams) =>
        [...platformAuditLogQueryKeys.all, params] as const,
}

export function usePlatformAuditLogs(params: PlatformAuditLogsQueryParams) {
    return useQuery({
        queryKey: platformAuditLogQueryKeys.list(params),
        queryFn: () => systemAdminApi.getPlatformAuditLogs(params),
        placeholderData: keepPreviousData,
    })
}
