import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { approvalsApi } from './approvalsApi'

vi.mock('../../../api/httpClient', () => ({
    httpClient: {
        get: vi.fn(),
        post: vi.fn(),
        put: vi.fn(),
    },
}))

const page = {
    content: [],
    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 0,
    first: true,
    last: true,
}

const response = (data: unknown) => Promise.resolve({ data: { data } })

beforeEach(() => {
    vi.clearAllMocks()
})

describe('approvalsApi', () => {
    it('uses project-scoped definition and inbox routes', async () => {
        vi.mocked(httpClient.get).mockImplementation(() => response(page))

        await approvalsApi.listDefinitions('tenant-1', 'project-1')
        await approvalsApi.inbox('tenant-1', 'project-1')
        await approvalsApi.history('tenant-1', 'project-1')

        expect(httpClient.get).toHaveBeenNthCalledWith(
            1,
            '/api/tenants/tenant-1/projects/project-1/approvals/definitions',
            { params: { page: 0, size: 100 } },
        )
        expect(httpClient.get).toHaveBeenNthCalledWith(
            2,
            '/api/tenants/tenant-1/projects/project-1/approvals/inbox',
            { params: { page: 0, size: 25 } },
        )
        expect(httpClient.get).toHaveBeenNthCalledWith(
            3,
            '/api/tenants/tenant-1/projects/project-1/approvals/requests',
            { params: { page: 0, size: 25 } },
        )
    })

    it('posts a bounded decision to the request route', async () => {
        vi.mocked(httpClient.post).mockImplementation(() => response({ id: 'request-1' }))

        await approvalsApi.decide('tenant-1', 'project-1', 'request-1', {
            outcome: 'APPROVED',
            comment: 'Ready to continue',
        })

        expect(httpClient.post).toHaveBeenCalledWith(
            '/api/tenants/tenant-1/projects/project-1/approvals/requests/request-1/decision',
            { outcome: 'APPROVED', comment: 'Ready to continue' },
        )
    })
})
