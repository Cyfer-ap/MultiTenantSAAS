import {
    keepPreviousData,
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import { invitationsApi } from '../api/invitationsApi'
import type {
    AcceptInvitationInput,
    CreateInvitationInput,
    TenantInvitationsQueryParams,
} from '../types/invitations'

export const tenantInvitationsQueryKeys = {
    all: ['tenant-invitations'] as const,
    tenant: (tenantId: string) => [
        ...tenantInvitationsQueryKeys.all,
        tenantId,
    ] as const,
    list: (
        tenantId: string,
        params: TenantInvitationsQueryParams,
    ) => [
        ...tenantInvitationsQueryKeys.tenant(tenantId),
        params,
    ] as const,
}

export function useTenantInvitations(
    tenantId: string,
    params: TenantInvitationsQueryParams,
) {
    return useQuery({
        queryKey: tenantInvitationsQueryKeys.list(
            tenantId,
            params,
        ),
        queryFn: () =>
            invitationsApi.getInvitations(
                tenantId,
                params,
            ),
        enabled: tenantId.length > 0,
        placeholderData: keepPreviousData,
    })
}

export function useCreateInvitation(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (input: CreateInvitationInput) =>
            invitationsApi.createInvitation(
                tenantId,
                input,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantInvitationsQueryKeys.tenant(
                        tenantId,
                    ),
            })
        },
    })
}

export function useRevokeInvitation(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (invitationId: string) =>
            invitationsApi.revokeInvitation(
                tenantId,
                invitationId,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantInvitationsQueryKeys.tenant(
                        tenantId,
                    ),
            })
        },
    })
}

export function useAcceptInvitation() {
    return useMutation({
        mutationFn: (input: AcceptInvitationInput) =>
            invitationsApi.acceptInvitation(input),
    })
}
