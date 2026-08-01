import { httpClient } from '../../../api/httpClient'
import type {
    ApiResponse,
    PageResponse,
} from '../../../types/api'
import type {
    TenantAuditLog,
    TenantAuditLogsQueryParams,
} from '../types/auditLogs'

async function getAuditLogs(
    tenantId: string,
    params: TenantAuditLogsQueryParams,
): Promise<PageResponse<TenantAuditLog>> {
    const response = await httpClient.get<
        ApiResponse<PageResponse<TenantAuditLog>>
    >(`/api/tenants/${tenantId}/audit-logs`, {
        params,
    })

    return response.data.data
}

export const auditLogsApi = {
    getAuditLogs,
}
