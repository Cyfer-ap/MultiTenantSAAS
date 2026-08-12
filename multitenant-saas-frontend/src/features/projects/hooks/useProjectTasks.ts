import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { projectTasksApi } from '../api/projectTasksApi'
import type {
    CreateProjectTaskInput,
    ProjectTaskDetailsInput,
    ProjectTasksQueryParams,
    UpdateProjectTaskAssigneeInput,
    UpdateProjectTaskStatusInput,
} from '../types/projectTasks'

export const projectTasksQueryKeys = {
    all: ['project-tasks'] as const,
    project: (tenantId: string, projectId: string) =>
        [...projectTasksQueryKeys.all, tenantId, projectId] as const,
    list: (tenantId: string, projectId: string, params: ProjectTasksQueryParams) =>
        [...projectTasksQueryKeys.project(tenantId, projectId), params] as const,
}

export function useProjectTasks(
    tenantId: string,
    projectId: string,
    params: ProjectTasksQueryParams,
    enabled = true,
) {
    return useQuery({
        queryKey: projectTasksQueryKeys.list(tenantId, projectId, params),
        queryFn: () => projectTasksApi.getTasks(tenantId, projectId, params),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0,
        placeholderData: keepPreviousData,
    })
}

interface TaskMutationInput<TInput> {
    taskId: string
    input: TInput
}

function useInvalidateProjectTasks(tenantId: string, projectId: string) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await queryClient.invalidateQueries({
            queryKey: projectTasksQueryKeys.project(tenantId, projectId),
        })
    }
}

export function useCreateProjectTask(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectTasks(tenantId, projectId)

    return useMutation({
        mutationFn: (input: CreateProjectTaskInput) =>
            projectTasksApi.createTask(tenantId, projectId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateProjectTask(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectTasks(tenantId, projectId)

    return useMutation({
        mutationFn: ({ taskId, input }: TaskMutationInput<ProjectTaskDetailsInput>) =>
            projectTasksApi.updateTask(tenantId, projectId, taskId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateProjectTaskStatus(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectTasks(tenantId, projectId)

    return useMutation({
        mutationFn: ({ taskId, input }: TaskMutationInput<UpdateProjectTaskStatusInput>) =>
            projectTasksApi.updateTaskStatus(tenantId, projectId, taskId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateProjectTaskAssignee(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectTasks(tenantId, projectId)

    return useMutation({
        mutationFn: ({ taskId, input }: TaskMutationInput<UpdateProjectTaskAssigneeInput>) =>
            projectTasksApi.updateTaskAssignee(tenantId, projectId, taskId, input),
        onSuccess: invalidate,
    })
}

export function useCancelProjectTask(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectTasks(tenantId, projectId)

    return useMutation({
        mutationFn: (taskId: string) => projectTasksApi.cancelTask(tenantId, projectId, taskId),
        onSuccess: invalidate,
    })
}
