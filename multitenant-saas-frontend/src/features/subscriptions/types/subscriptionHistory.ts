import type { PageResponse } from '../../../types/api'
import type { BillingInterval, BillingProvider, TenantSubscriptionStatus } from './subscriptions'

export type TenantSubscriptionHistoryEventType =
    | 'MIGRATED_CURRENT_STATE'
    | 'STARTED'
    | 'PLAN_CHANGED'
    | 'LIFECYCLE_UPDATED'
    | 'PROVIDER_SYNCHRONIZED'
    | 'PROVIDER_RECONCILED'

export interface TenantSubscriptionHistoryEntry {
    id: string
    subscriptionId: string
    tenantId: string
    tenantName: string
    planId: string
    planCode: string
    planName: string
    planDescription: string | null
    billingInterval: BillingInterval
    price: number
    currency: string
    maxUsers: number | null
    maxProjects: number | null
    maxStorageMb: number | null
    status: TenantSubscriptionStatus
    startedAt: string
    currentPeriodStart: string
    currentPeriodEnd: string
    trialEndsAt: string | null
    cancelAtPeriodEnd: boolean
    cancelledAt: string | null
    billingProvider: BillingProvider | null
    providerSubscriptionId: string | null
    providerEventCreatedAt: string | null
    eventType: TenantSubscriptionHistoryEventType
    recordedAt: string
}

export type TenantSubscriptionHistoryPage = PageResponse<TenantSubscriptionHistoryEntry>
