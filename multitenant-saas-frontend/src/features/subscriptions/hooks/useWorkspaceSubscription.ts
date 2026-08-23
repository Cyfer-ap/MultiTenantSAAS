import { useMutation, useQuery } from '@tanstack/react-query'

import { tenantSubscriptionApi } from '../api/tenantSubscriptionApi'
import type { BillingCheckoutInput } from '../types/subscriptions'

export const workspaceSubscriptionQueryKeys = {
    all: ['workspace-subscription'] as const,
    tenant: (tenantId: string) => [...workspaceSubscriptionQueryKeys.all, tenantId] as const,
    entitlements: (tenantId: string) =>
        [...workspaceSubscriptionQueryKeys.tenant(tenantId), 'entitlements'] as const,
    access: (tenantId: string) =>
        [...workspaceSubscriptionQueryKeys.tenant(tenantId), 'access'] as const,
    checkoutConfiguration: (tenantId: string) =>
        [...workspaceSubscriptionQueryKeys.tenant(tenantId), 'checkout-configuration'] as const,
}

export function useWorkspaceSubscription(tenantId: string) {
    return useQuery({
        queryKey: workspaceSubscriptionQueryKeys.tenant(tenantId),
        queryFn: () => tenantSubscriptionApi.getSubscription(tenantId),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useWorkspaceSubscriptionEntitlements(tenantId: string) {
    return useQuery({
        queryKey: workspaceSubscriptionQueryKeys.entitlements(tenantId),
        queryFn: () => tenantSubscriptionApi.getEntitlements(tenantId),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useWorkspaceSubscriptionAccess(tenantId: string) {
    return useQuery({
        queryKey: workspaceSubscriptionQueryKeys.access(tenantId),
        queryFn: () => tenantSubscriptionApi.getAccess(tenantId),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useBillingCheckoutConfiguration(tenantId: string, enabled = true) {
    return useQuery({
        queryKey: workspaceSubscriptionQueryKeys.checkoutConfiguration(tenantId),
        queryFn: () => tenantSubscriptionApi.getCheckoutConfiguration(tenantId),
        enabled: enabled && tenantId.length > 0,
        retry: false,
    })
}

export function useCreateBillingCheckout() {
    return useMutation({
        mutationFn: ({ tenantId, input }: { tenantId: string; input: BillingCheckoutInput }) =>
            tenantSubscriptionApi.createCheckout(tenantId, input),
    })
}
