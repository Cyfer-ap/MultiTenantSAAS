import type { WorkspaceSubscriptionAccess } from '../types/subscriptions'

const WARNING_WINDOW_MS = 7 * 24 * 60 * 60 * 1000

function formatDate(value: string): string {
    const date = new Date(value)

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function isEndingSoon(value: string | null, now: number): value is string {
    if (!value) {
        return false
    }

    const endAt = new Date(value).getTime()

    if (Number.isNaN(endAt)) {
        return false
    }

    const remaining = endAt - now

    return remaining > 0 && remaining <= WARNING_WINDOW_MS
}

export function getSubscriptionEndingSoonMessage(
    access: WorkspaceSubscriptionAccess | null,
    now = Date.now(),
): string | null {
    if (!access || access.accessLevel === 'BLOCKED') {
        return null
    }

    if (access.cancelAtPeriodEnd && isEndingSoon(access.currentPeriodEnd, now)) {
        return `Your subscription is scheduled to end on ${formatDate(access.currentPeriodEnd)}.`
    }

    if (access.accessReason === 'TRIAL_ACTIVE' && isEndingSoon(access.trialEndsAt, now)) {
        return `Your trial ends on ${formatDate(access.trialEndsAt)}.`
    }

    if (access.accessReason === 'PAST_DUE_GRACE' && isEndingSoon(access.currentPeriodEnd, now)) {
        return `Past-due grace access ends on ${formatDate(access.currentPeriodEnd)}.`
    }

    return null
}
