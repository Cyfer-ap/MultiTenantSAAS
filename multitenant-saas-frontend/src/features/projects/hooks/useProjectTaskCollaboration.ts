import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { projectTaskCollaborationApi } from '../api/projectTaskCollaborationApi'
import type {
    TaskAttachmentUploadInput,
    TaskCollaborationPageParams,
    TaskCommentInput,
} from '../types/taskCollaboration'

export const taskCollaborationQueryKeys = {
    all: ['task-collaboration'] as const,
    task: (tenantId: string, projectId: string, taskId: string) =>
        [...taskCollaborationQueryKeys.all, tenantId, projectId, taskId] as const,
    comments: (
        tenantId: string,
        projectId: string,
        taskId: string,
        params: TaskCollaborationPageParams,
    ) =>
        [
            ...taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
            'comments',
            params,
        ] as const,
    pinnedComments: (tenantId: string, projectId: string, taskId: string) =>
        [...taskCollaborationQueryKeys.task(tenantId, projectId, taskId), 'comments', 'pinned'] as const,
    replies: (
        tenantId: string,
        projectId: string,
        taskId: string,
        commentId: string,
        params: TaskCollaborationPageParams,
    ) =>
        [
            ...taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
            'comments',
            commentId,
            'replies',
            params,
        ] as const,
    attachments: (
        tenantId: string,
        projectId: string,
        taskId: string,
        params: TaskCollaborationPageParams,
    ) =>
        [
            ...taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
            'attachments',
            params,
        ] as const,
    activity: (
        tenantId: string,
        projectId: string,
        taskId: string,
        params: TaskCollaborationPageParams,
    ) =>
        [
            ...taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
            'activity',
            params,
        ] as const,
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

export function usePinnedTaskComments(
    tenantId: string,
    projectId: string,
    taskId: string,
    enabled = true,
) {
    return useQuery({
        queryKey: taskCollaborationQueryKeys.pinnedComments(tenantId, projectId, taskId),
        queryFn: () => projectTaskCollaborationApi.getPinnedComments(tenantId, projectId, taskId),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0 && taskId.length > 0,
    })
}

export function useTaskCommentReplies(
    tenantId: string,
    projectId: string,
    taskId: string,
    commentId: string,
    params: TaskCollaborationPageParams,
    enabled = true,
) {
    return useQuery({
        queryKey: taskCollaborationQueryKeys.replies(
            tenantId,
            projectId,
            taskId,
            commentId,
            params,
        ),
        queryFn: () =>
            projectTaskCollaborationApi.getReplies(
                tenantId,
                projectId,
                taskId,
                commentId,
                params,
            ),
        enabled:
            enabled &&
            tenantId.length > 0 &&
            projectId.length > 0 &&
            taskId.length > 0 &&
            commentId.length > 0,
    })
}

export function useTaskAttachments(
    tenantId: string,
    projectId: string,
    taskId: string,
    params: TaskCollaborationPageParams,
    enabled = true,
) {
    return useQuery({
        queryKey: taskCollaborationQueryKeys.attachments(tenantId, projectId, taskId, params),
        queryFn: () =>
            projectTaskCollaborationApi.getAttachments(tenantId, projectId, taskId, params),
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

export function useCreateTaskCommentReply(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: ({ commentId, input }: CommentMutationInput) =>
            projectTaskCollaborationApi.createReply(
                tenantId,
                projectId,
                taskId,
                commentId,
                input,
            ),
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

export function usePinTaskComment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (commentId: string) =>
            projectTaskCollaborationApi.pinComment(tenantId, projectId, taskId, commentId),
        onSuccess: invalidate,
    })
}

export function useUnpinTaskComment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (commentId: string) =>
            projectTaskCollaborationApi.unpinComment(tenantId, projectId, taskId, commentId),
        onSuccess: invalidate,
    })
}

export function useUploadTaskAttachment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (input: TaskAttachmentUploadInput) =>
            projectTaskCollaborationApi.uploadTaskAttachment(tenantId, projectId, taskId, input),
        onSuccess: invalidate,
    })
}

export function useDeleteTaskAttachment(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useInvalidateTaskCollaboration(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (attachmentId: string) =>
            projectTaskCollaborationApi.deleteAttachment(tenantId, projectId, taskId, attachmentId),
        onSuccess: invalidate,
    })
}

export function useTaskAttachmentDownload(tenantId: string, projectId: string, taskId: string) {
    return useMutation({
        mutationFn: (attachmentId: string) =>
            projectTaskCollaborationApi.getAttachmentDownload(
                tenantId,
                projectId,
                taskId,
                attachmentId,
            ),
    })
}
