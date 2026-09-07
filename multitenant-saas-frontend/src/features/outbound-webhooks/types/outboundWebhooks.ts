import type { PageResponse } from '../../../types/api'

export type OutboundWebhookEventType =
    | 'PROJECT_CREATED'
    | 'PROJECT_UPDATED'
    | 'PROJECT_ARCHIVED'
    | 'TASK_CREATED'
    | 'TASK_UPDATED'
    | 'TASK_COMPLETED'
    | 'COMMENT_CREATED'
    | 'COMMENT_REPLIED'
    | 'MEMBER_ADDED'
    | 'MEMBER_REMOVED'
    | 'SUBSCRIPTION_UPDATED'
    | 'SUBSCRIPTION_CANCELLED'

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
    endpoint: OutboundWebhookEndpoint
    signingSecret: string
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

export type OutboundWebhookDeliveryPage = PageResponse<OutboundWebhookDelivery>
