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
