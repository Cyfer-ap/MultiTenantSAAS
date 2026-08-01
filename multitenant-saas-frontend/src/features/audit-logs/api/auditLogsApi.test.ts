import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import type { TenantAuditLog } from '../types/auditLogs'
import { auditLogsApi } from './auditLogsApi'

const auditLog: TenantAuditLog = {
    id: 'audit-1',
    tenantId: 'tenant-1',
    actorType: 'TENANT_USER',
    actorUserId: 'user-1',
    actorUserEmail: 'ada@example.com',
    actorSystemAdminId: null,
    actorSystemAdminEmail: null,
    targetUserId: 'user-2',
    targetUserEmail: 'grace@example.com',
    action: 'USER_ROLE_UPDATED',
    success: true,
    message: 'User role updated successfully.',
    createdAt: '2026-08-01T10:30:00Z',
}

describe('auditLogsApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads a filtered tenant audit-log page', async () => {
        const params = {
            page: 1,
            size: 25,
            sortBy: 'action' as const,
            sortDir: 'asc' as const,
            action: 'USER_ROLE_UPDATED' as const,
            success: true,
        }
        const page = {
            content: [auditLog],
            page: 1,
            size: 25,
            totalElements: 26,
            totalPages: 2,
            first: false,
            last: true,
        }
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValue({
                data: {
                    success: true,
                    message: 'Audit logs fetched successfully',
                    data: page,
                    timestamp: '2026-08-01T10:31:00Z',
                },
            })

        await expect(
            auditLogsApi.getAuditLogs('tenant-1', params),
        ).resolves.toEqual(page)

        expect(get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/audit-logs',
            { params },
        )
    })
})
