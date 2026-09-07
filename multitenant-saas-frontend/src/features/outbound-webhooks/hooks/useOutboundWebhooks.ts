import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { outboundWebhookApi } from '../api/outboundWebhookApi'
import type {
    OutboundWebhookDeliveryFilters,
    OutboundWebhookEndpointInput,
} from '../types/outboundWebhooks'

export const outboundWebhookQueryKeys = {
    all: ['outbound-webhooks'] as const,
    tenant: (tenantId: string) => [...outboundWebhookQueryKeys.all, tenantId] as const,
    endpoints: (tenantId: string) => [...outboundWebhookQueryKeys.tenant(tenantId), 'endpoints'] as const,
    eventCatalog: (tenantId: string) =>
        [...outboundWebhookQueryKeys.tenant(tenantId), 'event-catalog'] as const,
    deliveriesRoot: (tenantId: string) =>
        [...outboundWebhookQueryKeys.tenant(tenantId), 'deliveries'] as const,
    deliveries: (tenantId: string, filters: OutboundWebhookDeliveryFilters) =>
        [...outboundWebhookQueryKeys.deliveriesRoot(tenantId), filters] as const,
    delivery: (tenantId: string, deliveryId: string) =>
        [...outboundWebhookQueryKeys.deliveriesRoot(tenantId), deliveryId] as const,
}

export function useOutboundWebhookEndpoints(tenantId: string) {
    return useQuery({
        queryKey: outboundWebhookQueryKeys.endpoints(tenantId),
        queryFn: () => outboundWebhookApi.listEndpoints(tenantId),
        enabled: Boolean(tenantId),
    })
}

export function useOutboundWebhookEventCatalog(tenantId: string) {
    return useQuery({
        queryKey: outboundWebhookQueryKeys.eventCatalog(tenantId),
        queryFn: () => outboundWebhookApi.getEventCatalog(tenantId),
        enabled: Boolean(tenantId),
        staleTime: 5 * 60 * 1000,
    })
}

export function useOutboundWebhookDeliveries(
    tenantId: string,
    filters: OutboundWebhookDeliveryFilters,
) {
    return useQuery({
        queryKey: outboundWebhookQueryKeys.deliveries(tenantId, filters),
        queryFn: () => outboundWebhookApi.listDeliveries(tenantId, filters),
        enabled: Boolean(tenantId),
    })
}

export function useOutboundWebhookDelivery(tenantId: string, deliveryId: string | null) {
    return useQuery({
        queryKey: outboundWebhookQueryKeys.delivery(tenantId, deliveryId ?? ''),
        queryFn: () => outboundWebhookApi.getDelivery(tenantId, deliveryId ?? ''),
        enabled: Boolean(tenantId && deliveryId),
    })
}

export function useCreateOutboundWebhookEndpoint(tenantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (input: OutboundWebhookEndpointInput) =>
            outboundWebhookApi.createEndpoint(tenantId, input),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: outboundWebhookQueryKeys.endpoints(tenantId),
            })
        },
    })
}

export function useUpdateOutboundWebhookEndpoint(tenantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: ({ endpointId, input }: { endpointId: string; input: OutboundWebhookEndpointInput }) =>
            outboundWebhookApi.updateEndpoint(tenantId, endpointId, input),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.endpoints(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.deliveriesRoot(tenantId),
                }),
            ])
        },
    })
}

export function useRotateOutboundWebhookSecret(tenantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (endpointId: string) => outboundWebhookApi.rotateSecret(tenantId, endpointId),
        onSuccess: async () => {
            await queryClient.invalidateQueries({
                queryKey: outboundWebhookQueryKeys.endpoints(tenantId),
            })
        },
    })
}

export function useArchiveOutboundWebhookEndpoint(tenantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (endpointId: string) => outboundWebhookApi.archiveEndpoint(tenantId, endpointId),
        onSuccess: async () => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.endpoints(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.deliveriesRoot(tenantId),
                }),
            ])
        },
    })
}

export function useReplayOutboundWebhookDelivery(tenantId: string) {
    const queryClient = useQueryClient()

    return useMutation({
        mutationFn: (deliveryId: string) => outboundWebhookApi.replayDelivery(tenantId, deliveryId),
        onSuccess: async (delivery) => {
            await Promise.all([
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.deliveriesRoot(tenantId),
                }),
                queryClient.invalidateQueries({
                    queryKey: outboundWebhookQueryKeys.delivery(tenantId, delivery.id),
                }),
            ])
        },
    })
}
