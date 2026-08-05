import {
    useMutation,
    useQuery,
    useQueryClient,
} from '@tanstack/react-query'

import { platformAuditLogQueryKeys } from '../../system-admin/hooks/usePlatformAuditLogs'
import { systemSubscriptionApi } from '../api/systemSubscriptionApi'
import type {
    ChangeTenantSubscriptionPlanInput,
    CreateSubscriptionPlanInput,
    StartTenantSubscriptionInput,
    SubscriptionPlanStatus,
    UpdateSubscriptionPlanInput,
    UpdateTenantSubscriptionLifecycleInput,
} from '../types/subscriptions'

export const systemSubscriptionQueryKeys = {
    all: ['system-admin', 'subscriptions'] as const,
    plans: (activeOnly: boolean) => [
        ...systemSubscriptionQueryKeys.all,
        'plans',
        activeOnly,
    ] as const,
    tenant: (tenantId: string) => [
        ...systemSubscriptionQueryKeys.all,
        'tenant',
        tenantId,
    ] as const,
}

export function useSubscriptionPlans(activeOnly = false) {
    return useQuery({
        queryKey: systemSubscriptionQueryKeys.plans(activeOnly),
        queryFn: () => systemSubscriptionApi.getPlans(activeOnly),
    })
}

export function useTenantSubscription(tenantId: string | null) {
    return useQuery({
        queryKey: systemSubscriptionQueryKeys.tenant(tenantId ?? ''),
        queryFn: () => systemSubscriptionApi.getTenantSubscription(tenantId ?? ''),
        enabled: Boolean(tenantId),
        retry: false,
    })
}

function useInvalidateSubscriptions() {
    const queryClient = useQueryClient()
    return async (tenantId?: string) => {
        await Promise.all([
            queryClient.invalidateQueries({
                queryKey: systemSubscriptionQueryKeys.all,
            }),
            queryClient.invalidateQueries({
                queryKey: platformAuditLogQueryKeys.all,
            }),
            ...(tenantId
                ? [queryClient.invalidateQueries({
                    queryKey: systemSubscriptionQueryKeys.tenant(tenantId),
                })]
                : []),
        ])
    }
}

export function useCreateSubscriptionPlan() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: (input: CreateSubscriptionPlanInput) =>
            systemSubscriptionApi.createPlan(input),
        onSuccess: () => invalidate(),
    })
}

export function useUpdateSubscriptionPlan() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: ({ planId, input }: {
            planId: string
            input: UpdateSubscriptionPlanInput
        }) => systemSubscriptionApi.updatePlan(planId, input),
        onSuccess: () => invalidate(),
    })
}

export function useUpdateSubscriptionPlanStatus() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: ({ planId, status }: {
            planId: string
            status: SubscriptionPlanStatus
        }) => systemSubscriptionApi.updatePlanStatus(planId, status),
        onSuccess: () => invalidate(),
    })
}

export function useStartTenantSubscription() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: ({ tenantId, input }: {
            tenantId: string
            input: StartTenantSubscriptionInput
        }) => systemSubscriptionApi.startTenantSubscription(tenantId, input),
        onSuccess: (_data, variables) => invalidate(variables.tenantId),
    })
}

export function useChangeTenantSubscriptionPlan() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: ({ tenantId, input }: {
            tenantId: string
            input: ChangeTenantSubscriptionPlanInput
        }) => systemSubscriptionApi.changeTenantPlan(tenantId, input),
        onSuccess: (_data, variables) => invalidate(variables.tenantId),
    })
}

export function useUpdateTenantSubscriptionLifecycle() {
    const invalidate = useInvalidateSubscriptions()
    return useMutation({
        mutationFn: ({ tenantId, input }: {
            tenantId: string
            input: UpdateTenantSubscriptionLifecycleInput
        }) => systemSubscriptionApi.updateTenantLifecycle(tenantId, input),
        onSuccess: (_data, variables) => invalidate(variables.tenantId),
    })
}
