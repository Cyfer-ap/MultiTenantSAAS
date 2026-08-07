import {
    useMutation,
    useQueryClient,
} from '@tanstack/react-query'

import { workspaceSubscriptionQueryKeys } from '../../subscriptions/hooks/useWorkspaceSubscription'
import { projectsApi } from '../api/projectsApi'
import type {
    ProjectDetailsInput,
    UpdateProjectStatusInput,
} from '../types/projects'
import { tenantProjectsQueryKeys } from './useTenantProjects'

interface ProjectMutationInput<TInput> {
    projectId: string
    input: TInput
}

export function useCreateTenantProject(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (input: ProjectDetailsInput) =>
            projectsApi.createProject(tenantId, input),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey:
                        tenantProjectsQueryKeys.tenant(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey:
                        workspaceSubscriptionQueryKeys.tenant(
                            tenantId,
                        ),
                }),
            ])
        },
    })
}

export function useUpdateTenantProject(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            projectId,
            input,
        }: ProjectMutationInput<ProjectDetailsInput>) =>
            projectsApi.updateProject(
                tenantId,
                projectId,
                input,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantProjectsQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useUpdateTenantProjectStatus(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            projectId,
            input,
        }: ProjectMutationInput<UpdateProjectStatusInput>) =>
            projectsApi.updateProjectStatus(
                tenantId,
                projectId,
                input,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantProjectsQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useArchiveTenantProject(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (projectId: string) =>
            projectsApi.archiveProject(
                tenantId,
                projectId,
            ),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey:
                        tenantProjectsQueryKeys.tenant(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey:
                        workspaceSubscriptionQueryKeys.tenant(
                            tenantId,
                        ),
                }),
            ])
        },
    })
}
