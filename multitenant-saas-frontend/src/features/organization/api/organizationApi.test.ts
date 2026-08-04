import {
    beforeEach,
    describe,
    expect,
    it,
    vi,
} from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { organizationApi } from './organizationApi'

vi.mock('../../../api/httpClient', () => ({
    httpClient: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
        patch: vi.fn(),
    },
}))

describe('organizationApi', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('loads the tenant hierarchy', async () => {
        vi.mocked(httpClient.get).mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: [],
                timestamp: '2026-08-05T00:00:00Z',
            },
        })

        await organizationApi.getTree('tenant-1')

        expect(httpClient.get).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/organization/units/tree',
        )
    })

    it('creates a scoped organizational assignment', async () => {
        const input = {
            userId: 'user-1',
            organizationalUnitId: 'unit-1',
            reportsToAssignmentId: null,
            positionTitle: 'Engineering lead',
            primaryAssignment: true,
            validFrom: null,
            validUntil: null,
        }

        vi.mocked(httpClient.post).mockResolvedValue({
            data: {
                success: true,
                message: 'ok',
                data: {},
                timestamp: '2026-08-05T00:00:00Z',
            },
        })

        await organizationApi.createAssignment(
            'tenant-1',
            input,
        )

        expect(httpClient.post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/organization/assignments',
            input,
        )
    })
})
