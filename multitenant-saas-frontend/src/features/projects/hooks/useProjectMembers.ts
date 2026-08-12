import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { projectMembersApi } from '../api/projectMembersApi'
import type {
    AddProjectMemberInput,
    ProjectMembersQueryParams,
    UpdateProjectMemberRoleInput,
} from '../types/projects'

export const projectMembersQueryKeys = {
    all: ['project-members'] as const,
    project: (tenantId: string, projectId: string) =>
        [...projectMembersQueryKeys.all, tenantId, projectId] as const,
    list: (tenantId: string, projectId: string, params: ProjectMembersQueryParams) =>
        [...projectMembersQueryKeys.project(tenantId, projectId), params] as const,
    member: (tenantId: string, projectId: string, userId: string) =>
        [...projectMembersQueryKeys.project(tenantId, projectId), 'member', userId] as const,
}

export function useProjectMembers(
    tenantId: string,
    projectId: string,
    params: ProjectMembersQueryParams,
    enabled = true,
) {
    return useQuery({
        queryKey: projectMembersQueryKeys.list(tenantId, projectId, params),
        queryFn: () => projectMembersApi.getMembers(tenantId, projectId, params),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0,
        placeholderData: keepPreviousData,
    })
}

export function useProjectMember(
    tenantId: string,
    projectId: string,
    userId: string,
    enabled = true,
) {
    return useQuery({
        queryKey: projectMembersQueryKeys.member(tenantId, projectId, userId),
        queryFn: () => projectMembersApi.getMember(tenantId, projectId, userId),
        enabled: enabled && tenantId.length > 0 && projectId.length > 0 && userId.length > 0,
    })
}

interface MemberMutationInput<TInput> {
    userId: string
    input: TInput
}

function useInvalidateProjectMembers(tenantId: string, projectId: string) {
    const queryClient = useQueryClient()

    return async (): Promise<void> => {
        await queryClient.invalidateQueries({
            queryKey: projectMembersQueryKeys.project(tenantId, projectId),
        })
    }
}

export function useAddProjectMember(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectMembers(tenantId, projectId)

    return useMutation({
        mutationFn: (input: AddProjectMemberInput) =>
            projectMembersApi.addMember(tenantId, projectId, input),
        onSuccess: invalidate,
    })
}

export function useUpdateProjectMemberRole(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectMembers(tenantId, projectId)

    return useMutation({
        mutationFn: ({ userId, input }: MemberMutationInput<UpdateProjectMemberRoleInput>) =>
            projectMembersApi.updateMemberRole(tenantId, projectId, userId, input),
        onSuccess: invalidate,
    })
}

export function useRemoveProjectMember(tenantId: string, projectId: string) {
    const invalidate = useInvalidateProjectMembers(tenantId, projectId)

    return useMutation({
        mutationFn: (userId: string) => projectMembersApi.removeMember(tenantId, projectId, userId),
        onSuccess: invalidate,
    })
}
