import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient, publicHttpClient } from '../../../api/httpClient'
import { externalAccessApi } from './externalAccessApi'

vi.mock('../../../api/httpClient', () => ({
    httpClient: {
        get: vi.fn(),
        post: vi.fn(),
        delete: vi.fn(),
    },
    publicHttpClient: {
        get: vi.fn(),
        post: vi.fn(),
    },
}))

const response = (data: unknown) => Promise.resolve({ data: { data } })

beforeEach(() => {
    vi.clearAllMocks()
})

describe('externalAccessApi', () => {
    it('keeps grant management on the authenticated project API', async () => {
        vi.mocked(httpClient.get).mockImplementation(() =>
            response({
                content: [],
                page: 0,
                size: 100,
                totalElements: 0,
                totalPages: 0,
                first: true,
                last: true,
            }),
        )
        vi.mocked(httpClient.post).mockImplementation(() =>
            response({ grant: { id: 'grant-1' }, invitationToken: 'invite' }),
        )
        vi.mocked(httpClient.delete).mockImplementation(() =>
            response({ id: 'grant-1', state: 'REVOKED' }),
        )

        await externalAccessApi.listGrants('tenant-1', 'project-1')
        await externalAccessApi.createGrant('tenant-1', 'project-1', {
            guestName: 'Client',
            guestEmail: 'client@example.com',
            expiresAt: '2026-10-01T00:00:00.000Z',
            capabilities: ['PROJECT_READ'],
        })
        await externalAccessApi.revokeGrant('tenant-1', 'project-1', 'grant-1')

        const base = '/api/tenants/tenant-1/projects/project-1/external-access-grants'
        expect(httpClient.get).toHaveBeenCalledWith(base, { params: { page: 0, size: 100 } })
        expect(httpClient.post).toHaveBeenCalledWith(base, {
            guestName: 'Client',
            guestEmail: 'client@example.com',
            expiresAt: '2026-10-01T00:00:00.000Z',
            capabilities: ['PROJECT_READ'],
        })
        expect(httpClient.delete).toHaveBeenCalledWith(`${base}/grant-1`)
        expect(publicHttpClient.get).not.toHaveBeenCalled()
        expect(publicHttpClient.post).not.toHaveBeenCalled()
    })

    it('uses only the public client and guest session header for guest access', async () => {
        vi.mocked(publicHttpClient.post).mockImplementation(() =>
            response({ sessionToken: 'session-token' }),
        )
        vi.mocked(publicHttpClient.get).mockImplementation(() => response({ tasks: [] }))

        await externalAccessApi.exchangeInvitation('invite-token')
        await externalAccessApi.getGuestSession('session-token')
        await externalAccessApi.getGuestTasks('session-token')

        expect(publicHttpClient.post).toHaveBeenCalledWith('/api/public/guest-portal/exchange', {
            invitationToken: 'invite-token',
        })
        expect(publicHttpClient.get).toHaveBeenNthCalledWith(
            1,
            '/api/public/guest-portal/session',
            { headers: { 'X-Guest-Session': 'session-token' } },
        )
        expect(publicHttpClient.get).toHaveBeenNthCalledWith(
            2,
            '/api/public/guest-portal/tasks',
            { headers: { 'X-Guest-Session': 'session-token' } },
        )
        expect(httpClient.get).not.toHaveBeenCalled()
        expect(httpClient.post).not.toHaveBeenCalled()
    })
})
