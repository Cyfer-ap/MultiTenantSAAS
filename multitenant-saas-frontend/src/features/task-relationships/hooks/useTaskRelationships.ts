import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { taskCollaborationQueryKeys } from '../../projects/hooks/useProjectTaskCollaboration'
import { taskRelationshipsApi } from '../api/taskRelationshipsApi'
import type { TaskLabelInput, TaskRelationships } from '../types/taskRelationships'

export const taskRelationshipsQueryKeys = {
    all: ['task-relationships'] as const,
    project: (tenantId: string, projectId: string) =>
        [...taskRelationshipsQueryKeys.all, tenantId, projectId] as const,
    task: (tenantId: string, projectId: string, taskId: string) =>
        [...taskRelationshipsQueryKeys.project(tenantId, projectId), 'task', taskId] as const,
    labels: (tenantId: string, projectId: string) =>
        [...taskRelationshipsQueryKeys.project(tenantId, projectId), 'labels'] as const,
}

export function useTaskRelationships(
    tenantId: string,
    projectId: string,
    taskId: string,
    enabled = true,
) {
    return useQuery({
        queryKey: taskRelationshipsQueryKeys.task(tenantId, projectId, taskId),
        queryFn: () => taskRelationshipsApi.getRelationships(tenantId, projectId, taskId),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0 && taskId.length > 0,
    })
}

export function useProjectTaskLabels(tenantId: string, projectId: string, enabled = true) {
    return useQuery({
        queryKey: taskRelationshipsQueryKeys.labels(tenantId, projectId),
        queryFn: () => taskRelationshipsApi.getLabels(tenantId, projectId),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0,
    })
}

function useRelationshipMutationInvalidation(tenantId: string, projectId: string, taskId: string) {
    const queryClient = useQueryClient()

    return async (data?: TaskRelationships): Promise<void> => {
        if (data) {
            queryClient.setQueryData(
                taskRelationshipsQueryKeys.task(tenantId, projectId, taskId),
                data,
            )
        }
        await Promise.all([
            queryClient.invalidateQueries({
                queryKey: taskRelationshipsQueryKeys.project(tenantId, projectId),
            }),
            queryClient.invalidateQueries({
                queryKey: taskCollaborationQueryKeys.task(tenantId, projectId, taskId),
            }),
        ])
    }
}

export function useUpdateTaskParent(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useRelationshipMutationInvalidation(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (parentTaskId: string | null) =>
            taskRelationshipsApi.updateParent(tenantId, projectId, taskId, parentTaskId),
        onSuccess: invalidate,
    })
}

export function useAddTaskDependency(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useRelationshipMutationInvalidation(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (blockingTaskId: string) =>
            taskRelationshipsApi.addDependency(tenantId, projectId, taskId, blockingTaskId),
        onSuccess: invalidate,
    })
}

export function useRemoveTaskDependency(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useRelationshipMutationInvalidation(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (blockingTaskId: string) =>
            taskRelationshipsApi.removeDependency(tenantId, projectId, taskId, blockingTaskId),
        onSuccess: invalidate,
    })
}

export function useAssignTaskLabel(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useRelationshipMutationInvalidation(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (labelId: string) =>
            taskRelationshipsApi.assignLabel(tenantId, projectId, taskId, labelId),
        onSuccess: invalidate,
    })
}

export function useUnassignTaskLabel(tenantId: string, projectId: string, taskId: string) {
    const invalidate = useRelationshipMutationInvalidation(tenantId, projectId, taskId)

    return useMutation({
        mutationFn: (labelId: string) =>
            taskRelationshipsApi.unassignLabel(tenantId, projectId, taskId, labelId),
        onSuccess: invalidate,
    })
}

function useInvalidateLabelCatalog(tenantId: string, projectId: string) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await queryClient.invalidateQueries({
            queryKey: taskRelationshipsQueryKeys.project(tenantId, projectId),
        })
    }
}

export function useCreateTaskLabel(tenantId: string, projectId: string) {
    const invalidate = useInvalidateLabelCatalog(tenantId, projectId)

    return useMutation({
        mutationFn: (input: TaskLabelInput) =>
            taskRelationshipsApi.createLabel(tenantId, projectId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateTaskLabel(tenantId: string, projectId: string) {
    const invalidate = useInvalidateLabelCatalog(tenantId, projectId)

    return useMutation({
        mutationFn: ({ labelId, input }: { labelId: string; input: TaskLabelInput }) =>
            taskRelationshipsApi.updateLabel(tenantId, projectId, labelId, input),
        onSuccess: invalidate,
    })
}

export function useDeleteTaskLabel(tenantId: string, projectId: string) {
    const invalidate = useInvalidateLabelCatalog(tenantId, projectId)

    return useMutation({
        mutationFn: (labelId: string) =>
            taskRelationshipsApi.deleteLabel(tenantId, projectId, labelId),
        onSuccess: invalidate,
    })
}
