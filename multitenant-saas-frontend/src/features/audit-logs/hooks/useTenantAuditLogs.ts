import {
    keepPreviousData,
    useQuery,
} from '@tanstack/react-query'

import { auditLogsApi } from '../api/auditLogsApi'
import type { TenantAuditLogsQueryParams } from '../types/auditLogs'

export const tenantAuditLogsQueryKeys = {
    all: ['tenant-audit-logs'] as const,
    tenant: (tenantId: string) => [
        ...tenantAuditLogsQueryKeys.all,
        tenantId,
    ] as const,
    list: (
        tenantId: string,
        params: TenantAuditLogsQueryParams,
    ) => [
        ...tenantAuditLogsQueryKeys.tenant(tenantId),
        params,
    ] as const,
}

export function useTenantAuditLogs(
    tenantId: string,
    params: TenantAuditLogsQueryParams,
) {
    return useQuery({
        queryKey: tenantAuditLogsQueryKeys.list(
            tenantId,
            params,
        ),
        queryFn: () =>
            auditLogsApi.getAuditLogs(tenantId, params),
        enabled: tenantId.length > 0,
        placeholderData: keepPreviousData,
    })
}
