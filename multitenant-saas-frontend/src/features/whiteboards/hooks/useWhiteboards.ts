import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { projectTasksQueryKeys } from '../../projects/hooks/useProjectTasks'
import { whiteboardsApi } from '../api/whiteboardsApi'
import type {
    ConvertWhiteboardNodeToTaskInput,
    Whiteboard,
    WhiteboardCreateInput,
    WhiteboardUpdateInput,
} from '../types/whiteboards'

export const whiteboardQueryKeys = {
    all: ['whiteboards'] as const,
    list: (tenantId: string, projectId: string) =>
        [...whiteboardQueryKeys.all, 'list', tenantId, projectId] as const,
    detail: (tenantId: string, projectId: string, boardId: string) =>
        [...whiteboardQueryKeys.all, 'detail', tenantId, projectId, boardId] as const,
}

export function useWhiteboards(tenantId: string, projectId: string) {
    return useQuery({
        queryKey: whiteboardQueryKeys.list(tenantId, projectId),
        queryFn: () => whiteboardsApi.list(tenantId, projectId),
        enabled: tenantId.length > 0 && projectId.length > 0,
    })
}

export function useWhiteboard(tenantId: string, projectId: string, boardId: string) {
    return useQuery({
        queryKey: whiteboardQueryKeys.detail(tenantId, projectId, boardId),
        queryFn: () => whiteboardsApi.get(tenantId, projectId, boardId),
        enabled: tenantId.length > 0 && projectId.length > 0 && boardId.length > 0,
    })
}

export function useCreateWhiteboard(tenantId: string, projectId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (input: WhiteboardCreateInput) =>
            whiteboardsApi.create(tenantId, projectId, input),
        onSuccess: async (board) => {
            queryClient.setQueryData(
                whiteboardQueryKeys.detail(tenantId, projectId, board.id),
                board,
            )
            await queryClient.invalidateQueries({
                queryKey: whiteboardQueryKeys.list(tenantId, projectId),
            })
        },
    })
}

export function useUpdateWhiteboard(tenantId: string, projectId: string, boardId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: (input: WhiteboardUpdateInput) =>
            whiteboardsApi.update(tenantId, projectId, boardId, input),
        onSuccess: async (board: Whiteboard) => {
            queryClient.setQueryData(
                whiteboardQueryKeys.detail(tenantId, projectId, board.id),
                board,
            )
            await queryClient.invalidateQueries({
                queryKey: whiteboardQueryKeys.list(tenantId, projectId),
            })
        },
    })
}

export function useDeleteWhiteboard(tenantId: string, projectId: string) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({ boardId, expectedVersion }: { boardId: string; expectedVersion: number }) =>
            whiteboardsApi.remove(tenantId, projectId, boardId, expectedVersion),
        onSuccess: async (_data, variables) => {
            queryClient.removeQueries({
                queryKey: whiteboardQueryKeys.detail(tenantId, projectId, variables.boardId),
            })
            await queryClient.invalidateQueries({
                queryKey: whiteboardQueryKeys.list(tenantId, projectId),
            })
        },
    })
}

export function useConvertWhiteboardNodeToTask(
    tenantId: string,
    projectId: string,
    boardId: string,
) {
    const queryClient = useQueryClient()
    return useMutation({
        mutationFn: ({
            nodeKey,
            input,
        }: {
            nodeKey: string
            input: ConvertWhiteboardNodeToTaskInput
        }) => whiteboardsApi.convertNodeToTask(tenantId, projectId, boardId, nodeKey, input),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: whiteboardQueryKeys.detail(tenantId, projectId, boardId),
                }),
                queryClient.invalidateQueries({
                    queryKey: whiteboardQueryKeys.list(tenantId, projectId),
                }),
                queryClient.invalidateQueries({
                    queryKey: projectTasksQueryKeys.project(tenantId, projectId),
                }),
            ])
        },
    })
}
