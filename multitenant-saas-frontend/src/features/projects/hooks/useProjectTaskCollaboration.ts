import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { projectTaskCollaborationApi } from '../api/projectTaskCollaborationApi'
import type { TaskCollaborationPageParams, TaskCommentInput } from '../types/taskCollaboration'

export const taskCollaborationQueryKeys = {
    all: ['task-collaboration'] as const,
    task: (tenantId: string, projectId: string, taskId: string) =>
        [...taskCollaborationQueryKeys.all, tenantId, projectId, taskId] as const,
    comments: (
        tenantId: string,
        projectId: string,
        taskId: string,
        params: TaskCollaborationPageParams,
    ) => [...taskCollaborationQueryKeys.task(tenantId, projectId, taskId), 'comments', params] as const,
    activity: (
        tenantId: string,
        projectId: string,
        taskId: string,
        params: TaskCollaborationPageParams,
    ) => [...taskCollaborationQueryKeys.task(tenantId, projectId, taskId), 'activity', params] as const,
}

export function useTaskComments(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
    enabled = true,
) {
    return useQuery({
        queryKey: taskCollaborationQueryKeys.comments(tenantId, projectId, taskId, params),
        queryFn: () => projectTaskCollaborationApi.getComments(tenantId, projectId, taskId, params),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0 && taskId.length > 0,
    })
}

export function useTaskActivity(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
    enabled = true,
) {
    return useQuery({
        queryKey: taskCollaborationQueryKeys.activity(tenantId, projectId, taskId, params),
        queryFn: () => projectTaskCollaborationApi.getActivity(tenantId, projectId, taskId, params),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0 && taskId.length > 0,
    })
}

interface CommentMutationInput {
    commentId: string
    input: TaskCommentInput
}

function useInvalidateTaskCollaboration(tenantId: string, projectId: string, taskId: string) {
    const queryClient = useQueryClient()

    return async () => {
        await queryClient.invalidateQueries({
            queryKey: taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
        })
    }
}

export function useCreateTaskComment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (input: TaskCommentInput) =>
            projectTaskCollaborationApi.createComment(tenantId, projectId, taskId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateTaskComment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: ({ commentId, input }: CommentMutationInput) =>
            projectTaskCollaborationApi.updateComment(
                tenantId,
                projectId,
                taskId,
                commentId,
                input,
            ),
        onSuccess: invalidate,
    })
}

export function useDeleteTaskComment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (commentId: string) =>
            projectTaskCollaborationApi.deleteComment(tenantId, projectId, taskId, commentId),
        onSuccess: invalidate,
    })
}
