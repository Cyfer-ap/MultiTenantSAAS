import { beforeEach, describe, expect, it, vi } from 'vitest'

import { httpClient } from '../../../api/httpClient'
import { outboundWebhookApi } from './outboundWebhookApi'

const endpoint = {
    id: 'endpoint-1',
    tenantId: 'tenant-1',
    name: 'Operations webhook',
    url: 'https://example.com/hooks',
    enabled: true,
    events: ['project.created' as const],
    secretHint: '****12345678',
    secretVersion: 1,
    createdByUserId: 'user-1',
    updatedByUserId: 'user-1',
    secretRotatedAt: '2026-09-07T12:00:00Z',
    createdAt: '2026-09-07T12:00:00Z',
    updatedAt: '2026-09-07T12:00:00Z',
}

const delivery = {
    id: 'delivery-1',
    endpointId: endpoint.id,
    endpointName: endpoint.name,
    endpointUrl: endpoint.url,
    eventId: 'event-1',
    eventType: 'project.created' as const,
    eventOccurredAt: '2026-09-07T12:00:00Z',
    status: 'FAILED' as const,
    attemptCount: 3,
    replayCount: 0,
    nextAttemptAt: null,
    lastHttpStatus: 503,
    lastError: 'Service unavailable',
    sentAt: null,
    createdAt: '2026-09-07T12:00:00Z',
    updatedAt: '2026-09-07T12:03:00Z',
}

describe('outboundWebhookApi', () => {
    beforeEach(() => {
        vi.restoreAllMocks()
    })

    it('loads endpoints and event catalog', async () => {
        const get = vi
            .spyOn(httpClient, 'get')
            .mockResolvedValueOnce({ data: { success: true, message: 'ok', data: [endpoint] } })
            .mockResolvedValueOnce({
                data: {
                    success: true,
                    message: 'ok',
                    data: ['project.created', 'task.completed'],
                },
            })

        await expect(outboundWebhookApi.listEndpoints('tenant-1')).resolves.toEqual([endpoint])
        await expect(outboundWebhookApi.getEventCatalog('tenant-1')).resolves.toEqual([
            'project.created',
            'task.completed',
        ])

        expect(get).toHaveBeenNthCalledWith(1, '/api/tenants/tenant-1/outbound-webhooks')
        expect(get).toHaveBeenNthCalledWith(2, '/api/tenants/tenant-1/outbound-webhooks/events')
    })

    it('creates an endpoint using wire event names and returns the one-time secret', async () => {
        const created = { endpoint, signingSecret: 'mwh_secret' }
        const post = vi.spyOn(httpClient, 'post').mockResolvedValue({
            data: { success: true, message: 'created', data: created },
        })

        await expect(
            outboundWebhookApi.createEndpoint('tenant-1', {
                name: endpoint.name,
                url: endpoint.url,
                enabled: true,
                events: ['project.created'],
            }),
        ).resolves.toEqual(created)

        expect(post).toHaveBeenCalledWith('/api/tenants/tenant-1/outbound-webhooks', {
            name: endpoint.name,
            url: endpoint.url,
            enabled: true,
            events: ['project.created'],
        })
    })

    it('passes delivery filters and pagination to the backend', async () => {
        const page = {
            content: [delivery],
            page: 2,
            size: 20,
            totalElements: 41,
            totalPages: 3,
            first: false,
            last: true,
        }
        const get = vi.spyOn(httpClient, 'get').mockResolvedValue({
            data: { success: true, message: 'ok', data: page },
        })

        await expect(
            outboundWebhookApi.listDeliveries('tenant-1', {
                endpointId: endpoint.id,
                status: 'FAILED',
                page: 2,
                size: 20,
            }),
        ).resolves.toEqual(page)

        expect(get).toHaveBeenCalledWith('/api/tenants/tenant-1/outbound-webhooks/deliveries', {
            params: {
                endpointId: endpoint.id,
                status: 'FAILED',
                page: 2,
                size: 20,
            },
        })
    })

    it('rotates secrets and replays terminal deliveries through dedicated actions', async () => {
        const rotated = {
            endpointId: endpoint.id,
            secretHint: '****87654321',
            secretVersion: 2,
            signingSecret: 'mwh_new_secret',
            secretRotatedAt: '2026-09-07T13:00:00Z',
        }
        const post = vi
            .spyOn(httpClient, 'post')
            .mockResolvedValueOnce({ data: { success: true, message: 'rotated', data: rotated } })
            .mockResolvedValueOnce({
                data: {
                    success: true,
                    message: 'replayed',
                    data: { ...delivery, status: 'PENDING', attemptCount: 0, replayCount: 1 },
                },
            })

        await expect(outboundWebhookApi.rotateSecret('tenant-1', endpoint.id)).resolves.toEqual(
            rotated,
        )
        await expect(
            outboundWebhookApi.replayDelivery('tenant-1', delivery.id),
        ).resolves.toMatchObject({
            id: delivery.id,
            status: 'PENDING',
            replayCount: 1,
        })

        expect(post).toHaveBeenNthCalledWith(
            1,
            `/api/tenants/tenant-1/outbound-webhooks/${endpoint.id}/rotate-secret`,
        )
        expect(post).toHaveBeenNthCalledWith(
            2,
            `/api/tenants/tenant-1/outbound-webhooks/deliveries/${delivery.id}/replay`,
        )
    })
})
