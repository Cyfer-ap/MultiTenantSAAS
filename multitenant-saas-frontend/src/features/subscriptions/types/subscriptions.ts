export type BillingInterval = 'MONTHLY' | 'YEARLY'
export type SubscriptionPlanStatus = 'ACTIVE' | 'INACTIVE'
export type TenantSubscriptionStatus = 'TRIALING' | 'ACTIVE' | 'PAST_DUE' | 'CANCELLED' | 'EXPIRED'
export type BillingProvider = 'STRIPE' | 'RAZORPAY'

export interface BillingCheckoutConfiguration {
    plans: SubscriptionPlan[]
    providers: BillingProvider[]
}

export interface BillingCheckoutInput {
    planCode: string
    provider: BillingProvider
}

export interface BillingCheckoutSession {
    sessionId: string
    checkoutUrl: string
    provider: BillingProvider
}

export type SubscriptionAccessLevel = 'FULL_ACCESS' | 'GRACE_ACCESS' | 'BLOCKED'

export type SubscriptionAccessReason =
    | 'ACTIVE'
    | 'TRIAL_ACTIVE'
    | 'PAST_DUE_GRACE'
    | 'NO_SUBSCRIPTION'
    | 'PLAN_INACTIVE'
    | 'CANCELLED'
    | 'EXPIRED'
    | 'PERIOD_EXPIRED'
    | 'TRIAL_EXPIRED'

export interface SubscriptionPlan {
    id: string
    code: string
    name: string
    description: string | null
    billingInterval: BillingInterval
    price: number
    currency: string
    maxUsers: number | null
    maxProjects: number | null
    maxStorageMb: number | null
    status: SubscriptionPlanStatus
    createdAt: string
    updatedAt: string
}

export interface TenantSubscription {
    id: string
    tenantId: string
    tenantName: string
    plan: SubscriptionPlan
    status: TenantSubscriptionStatus
    startedAt: string
    currentPeriodStart: string
    currentPeriodEnd: string
    trialEndsAt: string | null
    cancelAtPeriodEnd: boolean
    cancelledAt: string | null
    createdAt: string
    updatedAt: string
}

export interface SubscriptionResourceEntitlement {
    used: number
    limit: number | null
    remaining: number | null
    unlimited: boolean
    limitReached: boolean
    overLimit: boolean
    creationAllowed: boolean
}

export interface TenantSubscriptionEntitlements {
    tenantId: string
    subscriptionId: string | null
    planId: string | null
    planCode: string | null
    planName: string | null
    subscriptionStatus: TenantSubscriptionStatus | null
    accessLevel: SubscriptionAccessLevel
    accessReason: SubscriptionAccessReason
    serviceAvailable: boolean
    mutationsAllowed: boolean
    cancelAtPeriodEnd: boolean
    currentPeriodEnd: string | null
    trialEndsAt: string | null
    evaluatedAt: string
    users: SubscriptionResourceEntitlement
    projects: SubscriptionResourceEntitlement
}

export interface WorkspaceSubscriptionAccess {
    tenantId: string
    subscriptionStatus: TenantSubscriptionStatus | null
    accessLevel: SubscriptionAccessLevel
    accessReason: SubscriptionAccessReason
    serviceAvailable: boolean
    mutationsAllowed: boolean
    userCreationAllowed: boolean
    projectCreationAllowed: boolean
    userLimitReached: boolean
    projectLimitReached: boolean
    cancelAtPeriodEnd: boolean
    currentPeriodEnd: string | null
    trialEndsAt: string | null
    evaluatedAt: string
}

export interface CreateSubscriptionPlanInput {
    code: string
    name: string
    description: string | null
    billingInterval: BillingInterval
    price: number
    currency: string
    maxUsers: number | null
    maxProjects: number | null
    maxStorageMb: number | null
}

export type UpdateSubscriptionPlanInput = Omit<CreateSubscriptionPlanInput, 'code'>

export interface UpdateSubscriptionPlanStatusInput {
    status: SubscriptionPlanStatus
}

export interface StartTenantSubscriptionInput {
    planId: string
    status: 'TRIALING' | 'ACTIVE'
    startedAt: string | null
    currentPeriodStart: string | null
    currentPeriodEnd: string
    trialEndsAt: string | null
    cancelAtPeriodEnd: boolean
}

export interface ChangeTenantSubscriptionPlanInput {
    planId: string
    currentPeriodStart: string
    currentPeriodEnd: string
}

export interface UpdateTenantSubscriptionLifecycleInput {
    status: TenantSubscriptionStatus
    cancelAtPeriodEnd: boolean
    currentPeriodEnd: string | null
    trialEndsAt: string | null
}
