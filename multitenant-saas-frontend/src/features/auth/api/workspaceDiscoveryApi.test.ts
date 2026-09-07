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

describe('authApi workspace discovery and OIDC', () => {
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

    it('verifies a code and returns SSO-aware workspace options', async () => {
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
                    authenticationMode: 'PASSWORD_OR_SSO' as const,
                    identityProviderDisplayName: 'Acme Identity',
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

    it('starts tenant OIDC login with the persistent-session preference', async () => {
        const request = { keepSignedIn: true }
        const response = {
            authorizationUrl: 'https://idp.example.com/authorize?state=opaque',
            expiresAt: '2026-09-08T01:00:00Z',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.startOidcLogin('tenant-1', request)).resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith('/api/tenants/tenant-1/auth/oidc/start', request)
    })

    it('exchanges the opaque browser handoff through the public session endpoint', async () => {
        const response = {
            tenantId: 'tenant-1',
            userId: 'user-1',
            fullName: 'Grace Hopper',
            email: 'grace@example.com',
            role: 'TENANT_USER' as const,
            accessToken: 'access-token',
            refreshToken: null,
            csrfToken: 'csrf-token',
            tokenType: 'Bearer',
            expiresInSeconds: 3600,
            persistentSession: false,
            message: 'Login successful',
        }
        const post = vi
            .spyOn(publicHttpClient, 'post')
            .mockResolvedValue(successfulResponse(response))

        await expect(authApi.completeOidcLogin('opaque-handoff')).resolves.toEqual(response)
        expect(post).toHaveBeenCalledWith('/api/auth/oidc/session', { code: 'opaque-handoff' })
    })
})
