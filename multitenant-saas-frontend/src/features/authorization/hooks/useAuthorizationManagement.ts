import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { authorizationApi } from '../api/authorizationApi'
import { currentAuthorizationQueryKeys } from './useCurrentAuthorization'
import type {
    CreateAuthorizationRoleInput,
    CreateAuthorizationUserRoleAssignmentInput,
    ReplaceAuthorizationRolePermissionsInput,
} from '../types/authorization'

export const authorizationManagementQueryKeys = {
    all: ['authorization-management'] as const,
    tenant: (tenantId: string) => [...authorizationManagementQueryKeys.all, tenantId] as const,
    permissions: (tenantId: string) =>
        [...authorizationManagementQueryKeys.tenant(tenantId), 'permissions'] as const,
    roles: (tenantId: string) =>
        [...authorizationManagementQueryKeys.tenant(tenantId), 'roles'] as const,
    assignmentReferenceData: (tenantId: string) =>
        [
            ...authorizationManagementQueryKeys.tenant(tenantId),
            'assignment-reference-data',
        ] as const,
    userAssignments: (tenantId: string, userId: string) =>
        [...authorizationManagementQueryKeys.tenant(tenantId), 'assignments', userId] as const,
}

export function useAuthorizationPermissions(tenantId: string) {
    return useQuery({
        queryKey: authorizationManagementQueryKeys.permissions(tenantId),
        queryFn: () => authorizationApi.getPermissions(tenantId),
        enabled: tenantId.length > 0,
    })
}

export function useAuthorizationRoles(tenantId: string) {
    return useQuery({
        queryKey: authorizationManagementQueryKeys.roles(tenantId),
        queryFn: () => authorizationApi.getRoles(tenantId),
        enabled: tenantId.length > 0,
    })
}

export function useAuthorizationAssignmentReferenceData(tenantId: string) {
    return useQuery({
        queryKey: authorizationManagementQueryKeys.assignmentReferenceData(tenantId),
        queryFn: () => authorizationApi.getAssignmentReferenceData(tenantId),
        enabled: tenantId.length > 0,
    })
}

export function useUserAuthorizationAssignments(tenantId: string, userId: string) {
    return useQuery({
        queryKey: authorizationManagementQueryKeys.userAssignments(tenantId, userId),
        queryFn: () => authorizationApi.getUserAssignments(tenantId, userId),
        enabled: tenantId.length > 0 && userId.length > 0,
    })
}

function useAuthorizationInvalidation(tenantId: string) {
    const queryClient = useQueryClient()

    return {
        invalidateRoles: async (): Promise<void> => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: authorizationManagementQueryKeys.roles(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey: currentAuthorizationQueryKeys.all,
                }),
            ])
        },
        invalidateAssignments: async (userId: string): Promise<void> => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: authorizationManagementQueryKeys.userAssignments(tenantId, userId),
                }),
                queryClient.invalidateQueries({
                    queryKey: currentAuthorizationQueryKeys.all,
                }),
            ])
        },
    }
}

export function useInitializeDefaultRoles(tenantId: string) {
    const { invalidateRoles } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: () => authorizationApi.initializeDefaultRoles(tenantId),
        onSuccess: invalidateRoles,
    })
}

export function useCreateAuthorizationRole(tenantId: string) {
    const { invalidateRoles } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: (input: CreateAuthorizationRoleInput) =>
            authorizationApi.createRole(tenantId, input),
        onSuccess: invalidateRoles,
    })
}

interface ReplaceRolePermissionsMutationInput {
    roleId: string
    input: ReplaceAuthorizationRolePermissionsInput
}

export function useReplaceAuthorizationRolePermissions(tenantId: string) {
    const { invalidateRoles } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: ({ roleId, input }: ReplaceRolePermissionsMutationInput) =>
            authorizationApi.replaceRolePermissions(tenantId, roleId, input),
        onSuccess: invalidateRoles,
    })
}

export function useDeactivateAuthorizationRole(tenantId: string) {
    const { invalidateRoles } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: (roleId: string) => authorizationApi.deactivateRole(tenantId, roleId),
        onSuccess: invalidateRoles,
    })
}

export function useCreateAuthorizationAssignment(tenantId: string) {
    const { invalidateAssignments } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: (input: CreateAuthorizationUserRoleAssignmentInput) =>
            authorizationApi.createAssignment(tenantId, input),
        onSuccess: async (assignment) => {
            await invalidateAssignments(assignment.userId)
        },
    })
}

interface DeactivateAssignmentMutationInput {
    assignmentId: string
    userId: string
}

export function useDeactivateAuthorizationAssignment(tenantId: string) {
    const { invalidateAssignments } = useAuthorizationInvalidation(tenantId)

    return useMutation({
        mutationFn: ({ assignmentId }: DeactivateAssignmentMutationInput) =>
            authorizationApi.deactivateAssignment(tenantId, assignmentId),
        onSuccess: async (_assignment, variables) => {
            await invalidateAssignments(variables.userId)
        },
    })
}
