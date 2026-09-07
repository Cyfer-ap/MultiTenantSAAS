import type { PageResponse } from '../../../types/api'

export type OutboundWebhookEventType =
    | 'project.created'
    | 'project.updated'
    | 'project.archived'
    | 'task.created'
    | 'task.updated'
    | 'task.completed'
    | 'comment.created'
    | 'comment.replied'
    | 'member.added'
    | 'member.removed'
    | 'subscription.updated'
    | 'subscription.cancelled'

export type OutboundWebhookDeliveryStatus =
    | 'PENDING'
    | 'PROCESSING'
    | 'RETRY'
    | 'SENT'
    | 'FAILED'

export type OutboundWebhookDeliveryAttemptOutcome = 'PROCESSING' | 'SUCCESS' | 'FAILURE'

export interface OutboundWebhookEndpoint {
    id: string
    tenantId: string
    name: string
    url: string
    enabled: boolean
    events: OutboundWebhookEventType[]
    secretHint: string
    secretVersion: number
    createdByUserId: string
    updatedByUserId: string
    secretRotatedAt: string
    createdAt: string
    updatedAt: string
}

export interface OutboundWebhookEndpointInput {
    name: string
    url: string
    enabled: boolean
    events: OutboundWebhookEventType[]
}

export interface OutboundWebhookEndpointCreated {
    endpoint: OutboundWebhookEndpoint
    signingSecret: string
}

export interface OutboundWebhookSecretRotated {
    endpointId: string
    secretHint: string
    secretVersion: number
    signingSecret: string
    secretRotatedAt: string
}

export interface OutboundWebhookDelivery {
    id: string
    endpointId: string
    endpointName: string
    endpointUrl: string
    eventId: string
    eventType: OutboundWebhookEventType
    eventOccurredAt: string
    status: OutboundWebhookDeliveryStatus
    attemptCount: number
    replayCount: number
    nextAttemptAt: string | null
    lastHttpStatus: number | null
    lastError: string | null
    sentAt: string | null
    createdAt: string
    updatedAt: string
}

export interface OutboundWebhookDeliveryAttempt {
    id: string
    replayNumber: number
    attemptNumber: number
    outcome: OutboundWebhookDeliveryAttemptOutcome
    httpStatus: number | null
    error: string | null
    startedAt: string
    completedAt: string | null
}

export interface OutboundWebhookDeliveryDetail {
    delivery: OutboundWebhookDelivery
    payloadJson: string
    attempts: OutboundWebhookDeliveryAttempt[]
}

export interface OutboundWebhookDeliveryFilters {
    endpointId?: string
    status?: OutboundWebhookDeliveryStatus
    page?: number
    size?: number
}

export type OutboundWebhookDeliveryPage = PageResponse<OutboundWebhookDelivery>
