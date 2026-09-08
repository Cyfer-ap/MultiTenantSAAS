import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { identityProviderApi } from '../api/identityProviderApi'
import type {
    TenantIdentityProviderCreateInput,
    TenantIdentityProviderUpdateInput,
    TenantSsoMode,
} from '../types/identityProvider'

export const identityProviderQueryKeys = {
    all: ['identity-provider'] as const,
    tenant: (tenantId: string) => [...identityProviderQueryKeys.all, tenantId] as const,
}

function useInvalidateIdentityProvider(tenantId: string): () => Promise<void> {
    const queryClient = useQueryClient()
    return async () => {
        await queryClient.invalidateQueries({
            queryKey: identityProviderQueryKeys.tenant(tenantId),
        })
    }
}

export function useIdentityProvider(tenantId: string) {
    return useQuery({
        queryKey: identityProviderQueryKeys.tenant(tenantId),
        queryFn: () => identityProviderApi.get(tenantId),
        enabled: Boolean(tenantId),
    })
}

export function useCreateIdentityProvider(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: (input: TenantIdentityProviderCreateInput) =>
            identityProviderApi.create(tenantId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateIdentityProvider(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: (input: TenantIdentityProviderUpdateInput) =>
            identityProviderApi.update(tenantId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateIdentityProviderPolicy(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: (ssoMode: TenantSsoMode) => identityProviderApi.updatePolicy(tenantId, ssoMode),
        onSuccess: invalidate,
    })
}

export function useVerifyIdentityProvider(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: () => identityProviderApi.verify(tenantId),
        onSuccess: invalidate,
    })
}

export function useRotateIdentityProviderSecret(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: (clientSecret: string) =>
            identityProviderApi.rotateClientSecret(tenantId, clientSecret),
        onSuccess: invalidate,
    })
}

export function useEnableIdentityProvider(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: () => identityProviderApi.enable(tenantId),
        onSuccess: invalidate,
    })
}

export function useDisableIdentityProvider(tenantId: string) {
    const invalidate = useInvalidateIdentityProvider(tenantId)
    return useMutation({
        mutationFn: () => identityProviderApi.disable(tenantId),
        onSuccess: invalidate,
    })
}
