import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { authorizationApi } from '../api/authorizationApi'
import type {
    CreateAuthorizationDelegationInput,
    ExplainAuthorizationAccessInput,
} from '../types/authorization'
import { currentAuthorizationQueryKeys } from './useCurrentAuthorization'

export const authorizationDelegationQueryKeys = {
    all: ['authorization-delegation'] as const,
    tenant: (tenantId: string) => [...authorizationDelegationQueryKeys.all, tenantId] as const,
    referenceData: (tenantId: string) =>
        [...authorizationDelegationQueryKeys.tenant(tenantId), 'reference-data'] as const,
    delegations: (tenantId: string) =>
        [...authorizationDelegationQueryKeys.tenant(tenantId), 'delegations'] as const,
}

export function useAuthorizationDelegationReferenceData(tenantId: string) {
    return useQuery({
        queryKey: authorizationDelegationQueryKeys.referenceData(tenantId),
        queryFn: () => authorizationApi.getDelegationReferenceData(tenantId),
        enabled: tenantId.length > 0,
    })
}

export function useAuthorizationDelegations(tenantId: string) {
    return useQuery({
        queryKey: authorizationDelegationQueryKeys.delegations(tenantId),
        queryFn: () => authorizationApi.getDelegations(tenantId),
        enabled: tenantId.length > 0,
    })
}

function useDelegationInvalidation(tenantId: string) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await Promise.all([
            queryClient.invalidateQueries({
                queryKey: authorizationDelegationQueryKeys.tenant(tenantId),
            }),
            queryClient.invalidateQueries({
                queryKey: currentAuthorizationQueryKeys.all,
            }),
        ])
    }
}

export function useCreateAuthorizationDelegation(tenantId: string) {
    const invalidate = useDelegationInvalidation(tenantId)

    return useMutation({
        mutationFn: (input: CreateAuthorizationDelegationInput) =>
            authorizationApi.createDelegation(tenantId, input),
        onSuccess: invalidate,
    })
}

export function useRevokeAuthorizationDelegation(tenantId: string) {
    const invalidate = useDelegationInvalidation(tenantId)

    return useMutation({
        mutationFn: (delegationId: string) =>
            authorizationApi.revokeDelegation(tenantId, delegationId),
        onSuccess: invalidate,
    })
}

export function useExplainAuthorizationAccess(tenantId: string) {
    return useMutation({
        mutationFn: (input: ExplainAuthorizationAccessInput) =>
            authorizationApi.explainAccess(tenantId, input),
    })
}
