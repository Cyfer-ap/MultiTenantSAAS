import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { savedViewsApi } from '../api/savedViewsApi'
import type {
    CreateSavedViewInput,
    SavedViewTarget,
    UpdateSavedViewInput,
} from '../types/savedViews'

export const savedViewQueryKeys = {
    all: ['saved-views'] as const,
    tenant: (tenantId: string) => [...savedViewQueryKeys.all, tenantId] as const,
    scope: (tenantId: string, target: SavedViewTarget, contextId: string | null) =>
        [...savedViewQueryKeys.tenant(tenantId), target, contextId ?? 'none'] as const,
}

export function useSavedViews(
    tenantId: string,
    target: SavedViewTarget,
    contextId: string | null,
) {
    return useQuery({
        queryKey: savedViewQueryKeys.scope(tenantId, target, contextId),
        queryFn: () => savedViewsApi.list(tenantId, target, contextId),
        enabled: tenantId.length > 0,
        staleTime: 30_000,
    })
}

export function useCreateSavedView(tenantId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (input: CreateSavedViewInput) => savedViewsApi.create(tenantId, input),
        onSuccess: async (savedView) => {
            await queryClient.invalidateQueries({
                queryKey: savedViewQueryKeys.scope(
                    tenantId,
                    savedView.target,
                    savedView.contextId,
                ),
            })
        },
    })
}

export function useUpdateSavedView(
    tenantId: string,
    target: SavedViewTarget,
    contextId: string | null,
) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ viewId, input }: { viewId: string; input: UpdateSavedViewInput }) =>
            savedViewsApi.update(tenantId, viewId, input),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: savedViewQueryKeys.scope(tenantId, target, contextId),
            })
        },
    })
}

export function useDeleteSavedView(
    tenantId: string,
    target: SavedViewTarget,
    contextId: string | null,
) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (viewId: string) => savedViewsApi.remove(tenantId, viewId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: savedViewQueryKeys.scope(tenantId, target, contextId),
            })
        },
    })
}
