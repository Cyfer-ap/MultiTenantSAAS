import { Alert } from '@mui/material'

import type {
    SubscriptionAccessReason,
    WorkspaceSubscriptionAccess,
} from '../types/subscriptions'

interface WorkspaceSubscriptionBannerProps {
    access: WorkspaceSubscriptionAccess | null
}

function formatDate(value: string | null): string {
    if (!value) {
        return 'the end of the current period'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return 'the end of the current period'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function blockedMessage(
    reason: SubscriptionAccessReason,
): string {
    switch (reason) {
        case 'NO_SUBSCRIPTION':
            return 'This workspace has no active subscription. Creating or reactivating users and creating projects is unavailable.'
        case 'PLAN_INACTIVE':
            return 'This workspace plan is inactive. Creating or reactivating users and creating projects is unavailable.'
        case 'CANCELLED':
            return 'This workspace subscription has been cancelled. Creating or reactivating users and creating projects is unavailable.'
        case 'EXPIRED':
            return 'This workspace subscription has expired. Creating or reactivating users and creating projects is unavailable.'
        case 'PERIOD_EXPIRED':
            return 'The current billing period has ended. Creating or reactivating users and creating projects is unavailable.'
        case 'TRIAL_EXPIRED':
            return 'This workspace trial has ended. Creating or reactivating users and creating projects is unavailable.'
        case 'ACTIVE':
        case 'TRIAL_ACTIVE':
        case 'PAST_DUE_GRACE':
            return 'The current subscription does not allow new subscription-controlled resources.'
    }
}

export function WorkspaceSubscriptionBanner({
    access,
}: WorkspaceSubscriptionBannerProps) {
    if (!access) {
        return null
    }

    if (access.accessLevel === 'BLOCKED') {
        return (
            <Alert
                aria-label="Workspace subscription status"
                severity="error"
                sx={{ marginBottom: 2 }}
            >
                {blockedMessage(access.accessReason)}
            </Alert>
        )
    }

    if (access.accessLevel === 'GRACE_ACCESS') {
        return (
            <Alert
                aria-label="Workspace subscription status"
                severity="warning"
                sx={{ marginBottom: 2 }}
            >
                This workspace is in past-due grace access
                through{' '}
                {formatDate(access.currentPeriodEnd)}.
                New users and projects remain available
                while plan capacity allows.
            </Alert>
        )
    }

    if (access.cancelAtPeriodEnd) {
        return (
            <Alert
                aria-label="Workspace subscription status"
                severity="warning"
                sx={{ marginBottom: 2 }}
            >
                This subscription is scheduled to end on{' '}
                {formatDate(access.currentPeriodEnd)}.
            </Alert>
        )
    }

    if (access.accessReason === 'TRIAL_ACTIVE') {
        return (
            <Alert
                aria-label="Workspace subscription status"
                severity="info"
                sx={{ marginBottom: 2 }}
            >
                Trial access is active until{' '}
                {formatDate(access.trialEndsAt)}.
            </Alert>
        )
    }

    return null
}
