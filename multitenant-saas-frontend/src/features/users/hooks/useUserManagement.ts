import {
    useMutation,
    useQueryClient,
} from '@tanstack/react-query'

import { workspaceSubscriptionQueryKeys } from '../../subscriptions/hooks/useWorkspaceSubscription'
import { usersApi } from '../api/usersApi'
import type {
    CreateTenantUserInput,
    UpdateTenantUserInput,
    UpdateTenantUserRoleInput,
    UpdateTenantUserStatusInput,
} from '../types/users'
import { tenantUsersQueryKeys } from './useTenantUsers'

interface UserMutationInput<TInput> {
    userId: string
    input: TInput
}

export function useCreateTenantUser(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (input: CreateTenantUserInput) =>
            usersApi.createUser(tenantId, input),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey:
                        tenantUsersQueryKeys.tenant(tenantId),
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

export function useUpdateTenantUser(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            userId,
            input,
        }: UserMutationInput<UpdateTenantUserInput>) =>
            usersApi.updateUser(tenantId, userId, input),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantUsersQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useUpdateTenantUserRole(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            userId,
            input,
        }: UserMutationInput<UpdateTenantUserRoleInput>) =>
            usersApi.updateUserRole(
                tenantId,
                userId,
                input,
            ),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantUsersQueryKeys.tenant(tenantId),
            })
        },
    })
}

export function useUpdateTenantUserStatus(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({
            userId,
            input,
        }: UserMutationInput<UpdateTenantUserStatusInput>) =>
            usersApi.updateUserStatus(
                tenantId,
                userId,
                input,
            ),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey:
                        tenantUsersQueryKeys.tenant(tenantId),
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

export function useUnlockTenantUserLogin(
    tenantId: string,
) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (userId: string) =>
            usersApi.unlockUserLogin(tenantId, userId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey:
                    tenantUsersQueryKeys.tenant(tenantId),
            })
        },
    })
}
