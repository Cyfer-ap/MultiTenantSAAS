import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { personalWorkspaceApi } from '../api/personalWorkspaceApi'
import type { PersonalResourceType } from '../types/personalWorkspace'

export const personalWorkspaceQueryKeys = {
    all: ['personal-workspace'] as const,
    tenant: (tenantId: string) => [...personalWorkspaceQueryKeys.all, tenantId] as const,
    overview: (tenantId: string, limit: number) =>
        [...personalWorkspaceQueryKeys.tenant(tenantId), 'overview', limit] as const,
}

export function usePersonalWorkspace(tenantId: string, limit = 12) {
    return useQuery({
        queryKey: personalWorkspaceQueryKeys.overview(tenantId, limit),
        queryFn: () => personalWorkspaceApi.getOverview(tenantId, limit),
        enabled: tenantId.length > 0,
        staleTime: 15_000,
    })
}

export function useFavoritePersonalResource(tenantId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ type, resourceId }: { type: PersonalResourceType; resourceId: string }) =>
            personalWorkspaceApi.favorite(tenantId, type, resourceId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: personalWorkspaceQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useUnfavoritePersonalResource(tenantId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ type, resourceId }: { type: PersonalResourceType; resourceId: string }) =>
            personalWorkspaceApi.unfavorite(tenantId, type, resourceId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: personalWorkspaceQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useRecordRecentPersonalResource(tenantId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ type, resourceId }: { type: PersonalResourceType; resourceId: string }) =>
            personalWorkspaceApi.recordRecent(tenantId, type, resourceId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: personalWorkspaceQueryKeys.tenant(tenantId),
            })
        },
    })
}
