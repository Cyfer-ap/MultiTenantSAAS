import { useQuery } from '@tanstack/react-query'

import { subscriptionHistoryApi } from '../api/subscriptionHistoryApi'

export const subscriptionHistoryQueryKeys = {
    all: ['subscription-history'] as const,
    tenant: (tenantId: string) => [...subscriptionHistoryQueryKeys.all, 'tenant', tenantId] as const,
    tenantPage: (tenantId: string, page: number, size: number) =>
        [...subscriptionHistoryQueryKeys.tenant(tenantId), page, size] as const,
    systemTenant: (tenantId: string) =>
        [...subscriptionHistoryQueryKeys.all, 'system-tenant', tenantId] as const,
    systemTenantPage: (tenantId: string, page: number, size: number) =>
        [...subscriptionHistoryQueryKeys.systemTenant(tenantId), page, size] as const,
}

export function useTenantSubscriptionHistory(tenantId: string, page = 0, size = 10) {
    return useQuery({
        queryKey: subscriptionHistoryQueryKeys.tenantPage(tenantId, page, size),
        queryFn: () => subscriptionHistoryApi.getTenantHistory(tenantId, page, size),
        enabled: tenantId.length > 0,
        retry: false,
    })
}

export function useSystemTenantSubscriptionHistory(
    tenantId: string | null,
    page = 0,
    size = 20,
) {
    const resolvedTenantId = tenantId ?? ''
    return useQuery({
        queryKey: subscriptionHistoryQueryKeys.systemTenantPage(resolvedTenantId, page, size),
        queryFn: () => subscriptionHistoryApi.getSystemTenantHistory(resolvedTenantId, page, size),
        enabled: Boolean(tenantId),
        retry: false,
    })
}
