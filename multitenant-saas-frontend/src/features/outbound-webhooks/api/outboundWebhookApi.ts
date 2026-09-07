import { httpClient } from '../../../api/httpClient'
import type { ApiResponse } from '../../../types/api'
import type {
    OutboundWebhookDelivery,
    OutboundWebhookDeliveryDetail,
    OutboundWebhookDeliveryFilters,
    OutboundWebhookDeliveryPage,
    OutboundWebhookEndpoint,
    OutboundWebhookEndpointCreated,
    OutboundWebhookEndpointInput,
    OutboundWebhookEventType,
    OutboundWebhookSecretRotated,
} from '../types/outboundWebhooks'

async function listEndpoints(tenantId: string): Promise<OutboundWebhookEndpoint[]> {
    const response = await httpClient.get<ApiResponse<OutboundWebhookEndpoint[]>>(
        `/api/tenants/${tenantId}/outbound-webhooks`,
    )

    return response.data.data
}

async function getEventCatalog(tenantId: string): Promise<OutboundWebhookEventType[]> {
    const response = await httpClient.get<ApiResponse<OutboundWebhookEventType[]>>(
        `/api/tenants/${tenantId}/outbound-webhooks/events`,
    )

    return response.data.data
}

async function createEndpoint(
    tenantId: string,
    input: OutboundWebhookEndpointInput,
): Promise<OutboundWebhookEndpointCreated> {
    const response = await httpClient.post<ApiResponse<OutboundWebhookEndpointCreated>>(
        `/api/tenants/${tenantId}/outbound-webhooks`,
        input,
    )

    return response.data.data
}

async function updateEndpoint(
    tenantId: string,
    endpointId: string,
    input: OutboundWebhookEndpointInput,
): Promise<OutboundWebhookEndpoint> {
    const response = await httpClient.put<ApiResponse<OutboundWebhookEndpoint>>(
        `/api/tenants/${tenantId}/outbound-webhooks/${endpointId}`,
        input,
    )

    return response.data.data
}

async function rotateSecret(
    tenantId: string,
    endpointId: string,
): Promise<OutboundWebhookSecretRotated> {
    const response = await httpClient.post<ApiResponse<OutboundWebhookSecretRotated>>(
        `/api/tenants/${tenantId}/outbound-webhooks/${endpointId}/rotate-secret`,
    )

    return response.data.data
}

async function archiveEndpoint(
    tenantId: string,
    endpointId: string,
): Promise<OutboundWebhookEndpoint> {
    const response = await httpClient.delete<ApiResponse<OutboundWebhookEndpoint>>(
        `/api/tenants/${tenantId}/outbound-webhooks/${endpointId}`,
    )

    return response.data.data
}

async function listDeliveries(
    tenantId: string,
    filters: OutboundWebhookDeliveryFilters = {},
): Promise<OutboundWebhookDeliveryPage> {
    const response = await httpClient.get<ApiResponse<OutboundWebhookDeliveryPage>>(
        `/api/tenants/${tenantId}/outbound-webhooks/deliveries`,
        {
            params: {
                endpointId: filters.endpointId || undefined,
                status: filters.status || undefined,
                page: filters.page ?? 0,
                size: filters.size ?? 20,
            },
        },
    )

    return response.data.data
}

async function getDelivery(
    tenantId: string,
    deliveryId: string,
): Promise<OutboundWebhookDeliveryDetail> {
    const response = await httpClient.get<ApiResponse<OutboundWebhookDeliveryDetail>>(
        `/api/tenants/${tenantId}/outbound-webhooks/deliveries/${deliveryId}`,
    )

    return response.data.data
}

async function replayDelivery(
    tenantId: string,
    deliveryId: string,
): Promise<OutboundWebhookDelivery> {
    const response = await httpClient.post<ApiResponse<OutboundWebhookDelivery>>(
        `/api/tenants/${tenantId}/outbound-webhooks/deliveries/${deliveryId}/replay`,
    )

    return response.data.data
}

export const outboundWebhookApi = {
    listEndpoints,
    getEventCatalog,
    createEndpoint,
    updateEndpoint,
    rotateSecret,
    archiveEndpoint,
    listDeliveries,
    getDelivery,
    replayDelivery,
}
