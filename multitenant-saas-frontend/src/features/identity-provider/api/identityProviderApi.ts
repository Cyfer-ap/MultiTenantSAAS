import { ApiClientError } from '../../../api/apiError'
import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    TenantIdentityProvider,
    TenantIdentityProviderCreateInput,
    TenantIdentityProviderSecretRotated,
    TenantIdentityProviderUpdateInput,
    TenantIdentityProviderVerification,
    TenantSsoMode,
} from '../types/identityProvider'

const providerPath = (tenantId: string): string =>
    `/api/tenants/${tenantId}/identity-provider`

async function get(tenantId: string): Promise<TenantIdentityProvider | null> {
    try {
        const response = await httpClient.get<ApiResponse<TenantIdentityProvider>>(
            providerPath(tenantId),
        )
        return response.data.data
    } catch (error: unknown) {
        if (error instanceof ApiClientError && error.status === 404) {
            return null
        }
        throw error
    }
}

async function create(
    tenantId: string,
    input: TenantIdentityProviderCreateInput,
): Promise<TenantIdentityProvider> {
    const response = await httpClient.post<ApiResponse<TenantIdentityProvider>>(
        providerPath(tenantId),
        input,
    )
    return response.data.data
}

async function update(
    tenantId: string,
    input: TenantIdentityProviderUpdateInput,
): Promise<TenantIdentityProvider> {
    const response = await httpClient.put<ApiResponse<TenantIdentityProvider>>(
        providerPath(tenantId),
        input,
    )
    return response.data.data
}

async function updatePolicy(
    tenantId: string,
    ssoMode: TenantSsoMode,
): Promise<TenantIdentityProvider> {
    const response = await httpClient.put<ApiResponse<TenantIdentityProvider>>(
        `${providerPath(tenantId)}/policy`,
        { ssoMode },
    )
    return response.data.data
}

async function verify(tenantId: string): Promise<TenantIdentityProviderVerification> {
    const response = await httpClient.post<ApiResponse<TenantIdentityProviderVerification>>(
        `${providerPath(tenantId)}/verify`,
    )
    return response.data.data
}

async function rotateClientSecret(
    tenantId: string,
    clientSecret: string,
): Promise<TenantIdentityProviderSecretRotated> {
    const response = await httpClient.post<ApiResponse<TenantIdentityProviderSecretRotated>>(
        `${providerPath(tenantId)}/rotate-client-secret`,
        { clientSecret },
    )
    return response.data.data
}

async function enable(tenantId: string): Promise<TenantIdentityProvider> {
    const response = await httpClient.post<ApiResponse<TenantIdentityProvider>>(
        `${providerPath(tenantId)}/enable`,
    )
    return response.data.data
}

async function disable(tenantId: string): Promise<TenantIdentityProvider> {
    const response = await httpClient.delete<ApiResponse<TenantIdentityProvider>>(
        providerPath(tenantId),
    )
    return response.data.data
}

export const identityProviderApi = {
    get,
    create,
    update,
    updatePolicy,
    verify,
    rotateClientSecret,
    enable,
    disable,
}
