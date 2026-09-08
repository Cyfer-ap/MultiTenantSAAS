import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { identityProviderApi } from './identityProviderApi'

const provider = {
    id: 'provider-1',
    tenantId: 'tenant-1',
    protocol: 'OIDC' as const,
    displayName: 'Company SSO',
    issuerUri: 'https://idp.example.com',
    clientId: 'client-1',
    scopes: ['openid', 'profile', 'email'],
    status: 'VERIFIED' as const,
    ssoMode: 'OPTIONAL' as const,
    clientSecretHint: '****secret',
    secretVersion: 1,
    verifiedAt: '2026-09-08T05:00:00Z',
    disabledAt: null,
    secretRotatedAt: '2026-09-08T04:00:00Z',
    createdAt: '2026-09-08T03:00:00Z',
    updatedAt: '2026-09-08T05:00:00Z',
}

describe('identityProviderApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads tenant identity-provider configuration', async () => {
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: { success: true, message: 'ok', data: provider },
        })

        await expect(identityProviderApi.get('tenant-1')).resolves.toEqual(provider)
        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/identity-provider')
    })

    it('uses write-only client secret only when creating or rotating credentials', async () => {
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValueOnce({
                data: { success: true, message: 'created', data: { ...provider, status: 'DRAFT' } },
            })
            .mockResolvedValueOnce({
                data: {
                    success: true,
                    message: 'rotated',
                    data: {
                        id: provider.id,
                        clientSecretHint: '****new',
                        secretVersion: 2,
                        status: 'DRAFT',
                        secretRotatedAt: '2026-09-08T06:00:00Z',
                    },
                },
            })

        const createInput = {
            displayName: provider.displayName,
            protocol: 'OIDC' as const,
            issuerUri: provider.issuerUri,
            clientId: provider.clientId,
            clientSecret: 'super-secret',
            scopes: provider.scopes,
        }

        await identityProviderApi.create('tenant-1', createInput)
        await identityProviderApi.rotateClientSecret('tenant-1', 'replacement-secret')

        expect(post).toHaveBeenNthCalledWith(
            1,
            '/api/tenants/tenant-1/identity-provider',
            createInput,
        )
        expect(post).toHaveBeenNthCalledWith(
            2,
            '/api/tenants/tenant-1/identity-provider/rotate-client-secret',
            { clientSecret: 'replacement-secret' },
        )
    })

    it('uses dedicated verification, policy, enable, and disable endpoints', async () => {
        const put = vi.spyOn(httpClient, 'put').mockResolvedValue({
            data: { success: true, message: 'updated', data: { ...provider, ssoMode: 'REQUIRED' } },
        })
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValueOnce({
                data: {
                    success: true,
                    message: 'verified',
                    data: {
                        id: provider.id,
                        status: 'VERIFIED',
                        issuerUri: provider.issuerUri,
                        authorizationEndpoint: 'https://idp.example.com/authorize',
                        tokenEndpoint: 'https://idp.example.com/token',
                        jwkSetUri: 'https://idp.example.com/jwks',
                        verifiedAt: provider.verifiedAt,
                    },
                },
            })
            .mockResolvedValueOnce({
                data: { success: true, message: 'enabled', data: { ...provider, status: 'DRAFT' } },
            })
        const remove = vi.spyOn(httpClient, 'delete').mockResolvedValue({
            data: { success: true, message: 'disabled', data: { ...provider, status: 'DISABLED' } },
        })

        await identityProviderApi.updatePolicy('tenant-1', 'REQUIRED')
        await identityProviderApi.verify('tenant-1')
        await identityProviderApi.enable('tenant-1')
        await identityProviderApi.disable('tenant-1')

        expect(put).toHaveBeenCalledWith('/api/tenants/tenant-1/identity-provider/policy', {
            ssoMode: 'REQUIRED',
        })
        expect(post).toHaveBeenNthCalledWith(1, '/api/tenants/tenant-1/identity-provider/verify')
        expect(post).toHaveBeenNthCalledWith(2, '/api/tenants/tenant-1/identity-provider/enable')
        expect(remove).toHaveBeenCalledWith('/api/tenants/tenant-1/identity-provider')
    })
})
