import { beforeEach, describe, expect, it, vi } from 'vitest'

import { publicHttpClient } from '../../../api/httpClient'
import { authApi } from './authApi'

function successfulResponse<T>(data: T) {
    return {
        data: {
            success: true,
            message: 'Success',
            data,
            timestamp: '2026-08-17T12:00:00Z',
        },
    }
}

describe('authApi workspace discovery', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('starts workspace discovery through the public endpoint', async () => {
        const request = {
            email: 'grace@example.com',
        }
        const response = {
            verificationRequired: true,
            challengeId: 'challenge-1',
            workspaces: [],
            workspaceGrantId: null,
            expiresInSeconds: 600,
            message: 'Check your email.',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.startWorkspaceDiscovery(request)).resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith('/api/auth/workspaces/start', request)
    })

    it('verifies a code and returns workspace options', async () => {
        const request = {
            challengeId: 'challenge-1',
            code: '123456',
            trustBrowser: true,
        }
        const response = {
            workspaces: [
                {
                    tenantId: 'tenant-1',
                    name: 'Research Lab',
                    slug: 'research-lab',
                },
            ],
            workspaceGrantId: 'grant-1',
            trustedBrowserToken: 'new-trusted-token',
            message: 'Email verified successfully.',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.verifyWorkspaceDiscovery(request)).resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith('/api/auth/workspaces/verify', request)
    })
})
