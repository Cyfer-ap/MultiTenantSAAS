import { useQuery } from '@tanstack/react-query'

import {
    tenantSubscriptionApi,
} from '../api/tenantSubscriptionApi'

export const workspaceSubscriptionQueryKeys = {
    all: ['workspace-subscription'] as const,
    tenant: (tenantId: string) => [
        ...workspaceSubscriptionQueryKeys.all,
        tenantId,
    ] as const,
    entitlements: (tenantId: string) => [
        ...workspaceSubscriptionQueryKeys.tenant(
            tenantId,
        ),
        'entitlements',
    ] as const,
    access: (tenantId: string) => [
        ...workspaceSubscriptionQueryKeys.tenant(
            tenantId,
        ),
        'access',
    ] as const,
}

export function useWorkspaceSubscription(
    tenantId: string,
) {
    return useQuery({
        queryKey:
            workspaceSubscriptionQueryKeys.tenant(
                tenantId,
            ),
        queryFn: () =>
            tenantSubscriptionApi.getSubscription(
                tenantId,
            ),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useWorkspaceSubscriptionEntitlements(
    tenantId: string,
) {
    return useQuery({
        queryKey:
            workspaceSubscriptionQueryKeys.entitlements(
                tenantId,
            ),
        queryFn: () =>
            tenantSubscriptionApi.getEntitlements(
                tenantId,
            ),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useWorkspaceSubscriptionAccess(
    tenantId: string,
) {
    return useQuery({
        queryKey:
            workspaceSubscriptionQueryKeys.access(
                tenantId,
            ),
        queryFn: () =>
            tenantSubscriptionApi.getAccess(tenantId),
        enabled: tenantId.length > 0,
        retry: false,
    })
}
