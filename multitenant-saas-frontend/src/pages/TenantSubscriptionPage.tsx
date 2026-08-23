import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded'
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import StorageRoundedIcon from '@mui/icons-material/StorageRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    LinearProgress,
    Paper,
    Stack,
    Typography,
} from '@mui/material'

import { ApiClientError } from '../api/apiError'
import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useBillingCheckoutConfiguration,
    useCreateBillingCheckout,
    useWorkspaceSubscription,
    useWorkspaceSubscriptionEntitlements,
} from '../features/subscriptions/hooks/useWorkspaceSubscription'
import type {
    BillingProvider,
    SubscriptionPlan,
    SubscriptionResourceEntitlement,
    TenantSubscriptionStatus,
} from '../features/subscriptions/types/subscriptions'

type StatusColor = 'success' | 'info' | 'warning' | 'error' | 'default'

function formatMoney(value: number, currency: string): string {
    return new Intl.NumberFormat(undefined, {
        style: 'currency',
        currency,
    }).format(value)
}

function formatDate(value: string | null): string {
    if (!value) {
        return '—'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function formatLimit(value: number | null, suffix = ''): string {
    return value === null ? 'Unlimited' : `${value.toLocaleString()}${suffix}`
}

function statusColor(status: TenantSubscriptionStatus): StatusColor {
    if (status === 'ACTIVE') {
        return 'success'
    }

    if (status === 'TRIALING') {
        return 'info'
    }

    if (status === 'PAST_DUE') {
        return 'warning'
    }

    if (status === 'CANCELLED' || status === 'EXPIRED') {
        return 'error'
    }

    return 'default'
}

function providerLabel(provider: BillingProvider): string {
    return provider === 'RAZORPAY' ? 'Razorpay' : 'Stripe'
}

function statusMessage(status: TenantSubscriptionStatus): string | null {
    if (status === 'TRIALING') {
        return 'Your workspace is currently using a trial subscription.'
    }

    if (status === 'PAST_DUE') {
        return 'This subscription is past due. Contact the platform administrator to prevent service restrictions.'
    }

    if (status === 'CANCELLED') {
        return 'This subscription has been cancelled.'
    }

    if (status === 'EXPIRED') {
        return 'This subscription has expired.'
    }

    return null
}

interface ResourceUsageCardProps {
    title: string
    configuredLimit: number | null
    entitlement?: SubscriptionResourceEntitlement
    loading: boolean
}

function ResourceUsageCard({
    title,
    configuredLimit,
    entitlement,
    loading,
}: ResourceUsageCardProps) {
    const limit = entitlement ? entitlement.limit : configuredLimit
    const unlimited = entitlement?.unlimited ?? limit === null
    const used = entitlement?.used
    const progress =
        used === undefined || unlimited
            ? 0
            : limit === null || limit <= 0
              ? 100
              : Math.min(100, (used / limit) * 100)

    let detail = 'Current usage is unavailable.'

    if (loading) {
        detail = 'Loading current usage…'
    } else if (entitlement) {
        if (entitlement.overLimit) {
            detail = 'Usage is above the current plan limit.'
        } else if (entitlement.limitReached) {
            detail = 'Plan limit reached.'
        } else if (!entitlement.creationAllowed) {
            detail = 'New resource creation is restricted.'
        } else if (entitlement.unlimited) {
            detail = 'Unlimited plan capacity.'
        } else {
            detail = `${entitlement.remaining ?? 0} slot${
                entitlement.remaining === 1 ? '' : 's'
            } remaining.`
        }
    }

    const usageLabel =
        used === undefined
            ? formatLimit(limit)
            : unlimited
              ? `${used.toLocaleString()} in use`
              : `${used.toLocaleString()} / ${(limit ?? 0).toLocaleString()}`

    return (
        <Paper sx={{ padding: 3 }} variant="outlined">
            <Typography variant="h6">{title}</Typography>
            <Typography
                sx={{
                    fontWeight: 700,
                    marginTop: 2,
                }}
                variant="h5"
            >
                {usageLabel}
            </Typography>

            {used !== undefined && !unlimited && (
                <LinearProgress
                    aria-label={`${title} usage`}
                    sx={{ marginTop: 2 }}
                    value={progress}
                    variant="determinate"
                />
            )}

            <Typography color="text.secondary" sx={{ marginTop: 1 }} variant="body2">
                {detail}
            </Typography>
        </Paper>
    )
}

export function TenantSubscriptionPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const subscriptionQuery = useWorkspaceSubscription(tenantId)
    const entitlementQuery = useWorkspaceSubscriptionEntitlements(tenantId)
    const noSubscription =
        subscriptionQuery.error instanceof ApiClientError && subscriptionQuery.error.status === 404
    const subscription = subscriptionQuery.data
    const entitlements = entitlementQuery.data
    const refreshing = subscriptionQuery.isFetching || entitlementQuery.isFetching
    const lifecycleMessage = subscription ? statusMessage(subscription.status) : null
    const canManageBilling =
        session?.role === 'TENANT_ADMIN' || session?.role === 'TENANT_MANAGER'
    const checkoutEligible =
        noSubscription ||
        subscription?.status === 'CANCELLED' ||
        subscription?.status === 'EXPIRED'
    const checkoutConfigurationQuery = useBillingCheckoutConfiguration(
        tenantId,
        canManageBilling && checkoutEligible,
    )
    const checkoutMutation = useCreateBillingCheckout()

    const beginCheckout = (plan: SubscriptionPlan, provider: BillingProvider) => {
        checkoutMutation.mutate(
            {
                tenantId,
                input: {
                    planCode: plan.code,
                    provider,
                },
            },
            {
                onSuccess: (checkout) => {
                    window.location.assign(checkout.checkoutUrl)
                },
            },
        )
    }

    return (
        <Box>
            <Stack
                direction={{
                    xs: 'column',
                    sm: 'row',
                }}
                spacing={2}
                sx={{
                    alignItems: {
                        sm: 'flex-start',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Subscription
                    </Typography>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Review your workspace plan, billing period, and configured limits.
                    </Typography>
                </Box>

                <Button
                    disabled={refreshing}
                    onClick={() => {
                        void Promise.all([subscriptionQuery.refetch(), entitlementQuery.refetch()])
                    }}
                    startIcon={
                        refreshing ? (
                            <CircularProgress color="inherit" size={16} />
                        ) : (
                            <RefreshRoundedIcon />
                        )
                    }
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            {subscriptionQuery.isPending && (
                <Paper
                    sx={{
                        marginTop: 3,
                        padding: 8,
                        textAlign: 'center',
                    }}
                    variant="outlined"
                >
                    <CircularProgress />
                    <Typography color="text.secondary" sx={{ marginTop: 2 }}>
                        Loading subscription…
                    </Typography>
                </Paper>
            )}

            {noSubscription && (
                <Paper
                    sx={{
                        marginTop: 3,
                        padding: 6,
                        textAlign: 'center',
                    }}
                    variant="outlined"
                >
                    <PaymentsRoundedIcon color="disabled" sx={{ fontSize: 52 }} />
                    <Typography sx={{ marginTop: 1 }} variant="h6">
                        No subscription assigned
                    </Typography>
                    <Typography
                        color="text.secondary"
                        sx={{
                            marginX: 'auto',
                            marginTop: 1,
                            maxWidth: 560,
                        }}
                    >
                        A platform administrator has not assigned a subscription plan to this
                        workspace yet.
                    </Typography>
                </Paper>
            )}

            {subscriptionQuery.isError && !noSubscription && (
                <Alert
                    action={
                        <Button
                            color="inherit"
                            onClick={() => {
                                void subscriptionQuery.refetch()
                            }}
                        >
                            Retry
                        </Button>
                    }
                    severity="error"
                    sx={{ marginTop: 3 }}
                >
                    {subscriptionQuery.error instanceof Error
                        ? subscriptionQuery.error.message
                        : 'The subscription could not be loaded.'}
                </Alert>
            )}

            {canManageBilling && checkoutEligible && checkoutConfigurationQuery.isPending && (
                <Paper
                    sx={{
                        marginTop: 3,
                        padding: 4,
                        textAlign: 'center',
                    }}
                    variant="outlined"
                >
                    <CircularProgress size={28} />
                    <Typography color="text.secondary" sx={{ marginTop: 1.5 }}>
                        Loading billing options…
                    </Typography>
                </Paper>
            )}

            {canManageBilling && checkoutEligible && checkoutConfigurationQuery.isError && (
                <Alert severity="error" sx={{ marginTop: 3 }}>
                    {checkoutConfigurationQuery.error instanceof Error
                        ? checkoutConfigurationQuery.error.message
                        : 'Billing checkout options could not be loaded.'}
                </Alert>
            )}

            {canManageBilling && checkoutEligible && checkoutConfigurationQuery.data && (
                <Paper sx={{ marginTop: 3, padding: 3 }} variant="outlined">
                    <Typography component="h2" variant="h5">
                        Choose a subscription
                    </Typography>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Select a paid plan and continue securely on the payment provider&apos;s
                        hosted checkout page.
                    </Typography>

                    {checkoutMutation.isError && (
                        <Alert severity="error" sx={{ marginTop: 2 }}>
                            {checkoutMutation.error instanceof Error
                                ? checkoutMutation.error.message
                                : 'The billing checkout could not be started.'}
                        </Alert>
                    )}

                    {checkoutConfigurationQuery.data.providers.length === 0 ? (
                        <Alert severity="info" sx={{ marginTop: 2 }}>
                            Online billing is not enabled for this deployment yet.
                        </Alert>
                    ) : checkoutConfigurationQuery.data.plans.length === 0 ? (
                        <Alert severity="info" sx={{ marginTop: 2 }}>
                            No paid subscription plans are currently available.
                        </Alert>
                    ) : (
                        <Box
                            sx={{
                                display: 'grid',
                                gap: 2,
                                gridTemplateColumns: {
                                    xs: '1fr',
                                    md: 'repeat(2, minmax(0, 1fr))',
                                },
                                marginTop: 3,
                            }}
                        >
                            {checkoutConfigurationQuery.data.plans.map((plan) => (
                                <Paper key={plan.id} sx={{ padding: 2.5 }} variant="outlined">
                                    <Typography component="h3" variant="h6">
                                        {plan.name}
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        {plan.code}
                                    </Typography>
                                    {plan.description && (
                                        <Typography sx={{ marginTop: 1.5 }}>
                                            {plan.description}
                                        </Typography>
                                    )}
                                    <Typography sx={{ fontWeight: 700, marginTop: 2 }} variant="h5">
                                        {formatMoney(plan.price, plan.currency)}
                                        <Typography
                                            color="text.secondary"
                                            component="span"
                                            variant="body2"
                                        >
                                            {' '}
                                            / {plan.billingInterval === 'MONTHLY' ? 'month' : 'year'}
                                        </Typography>
                                    </Typography>
                                    <Stack spacing={1} sx={{ marginTop: 2 }}>
                                        {checkoutConfigurationQuery.data.providers.map(
                                            (provider) => (
                                                <Button
                                                    disabled={checkoutMutation.isPending}
                                                    key={provider}
                                                    onClick={() => {
                                                        beginCheckout(plan, provider)
                                                    }}
                                                    startIcon={
                                                        checkoutMutation.isPending ? (
                                                            <CircularProgress
                                                                color="inherit"
                                                                size={16}
                                                            />
                                                        ) : (
                                                            <PaymentsRoundedIcon />
                                                        )
                                                    }
                                                    variant="contained"
                                                >
                                                    Continue with {providerLabel(provider)}
                                                </Button>
                                            ),
                                        )}
                                    </Stack>
                                </Paper>
                            ))}
                        </Box>
                    )}
                </Paper>
            )}

            {subscription && (
                <Stack spacing={2} sx={{ marginTop: 3 }}>
                    {lifecycleMessage && (
                        <Alert
                            severity={
                                subscription.status === 'TRIALING'
                                    ? 'info'
                                    : subscription.status === 'PAST_DUE'
                                      ? 'warning'
                                      : 'error'
                            }
                        >
                            {lifecycleMessage}
                        </Alert>
                    )}

                    {subscription.cancelAtPeriodEnd && (
                        <Alert severity="warning">
                            This subscription is scheduled to end when the current billing period
                            finishes on {formatDate(subscription.currentPeriodEnd)}.
                        </Alert>
                    )}

                    {subscription.plan.status === 'INACTIVE' && (
                        <Alert severity="warning">
                            This plan is no longer offered to new subscriptions. Your current
                            subscription details remain visible.
                        </Alert>
                    )}

                    <Paper sx={{ padding: 3 }} variant="outlined">
                        <Stack
                            direction={{
                                xs: 'column',
                                sm: 'row',
                            }}
                            spacing={2}
                            sx={{
                                alignItems: {
                                    sm: 'flex-start',
                                },
                                justifyContent: 'space-between',
                            }}
                        >
                            <Box>
                                <Typography component="h2" variant="h5">
                                    {subscription.plan.name}
                                </Typography>
                                <Typography
                                    color="text.secondary"
                                    sx={{
                                        marginTop: 0.5,
                                    }}
                                >
                                    {subscription.plan.code}
                                </Typography>
                                {subscription.plan.description && (
                                    <Typography
                                        sx={{
                                            marginTop: 1.5,
                                            maxWidth: 720,
                                        }}
                                    >
                                        {subscription.plan.description}
                                    </Typography>
                                )}
                            </Box>

                            <Stack direction="row" spacing={1}>
                                <Chip
                                    color={statusColor(subscription.status)}
                                    label={subscription.status.replaceAll('_', ' ')}
                                    variant="outlined"
                                />
                                <Chip
                                    label={
                                        subscription.plan.billingInterval === 'MONTHLY'
                                            ? 'Monthly'
                                            : 'Yearly'
                                    }
                                    variant="outlined"
                                />
                            </Stack>
                        </Stack>

                        <Divider
                            sx={{
                                marginBottom: 3,
                                marginTop: 3,
                            }}
                        />

                        <Typography sx={{ fontWeight: 700 }} variant="h5">
                            {formatMoney(subscription.plan.price, subscription.plan.currency)}
                            <Typography color="text.secondary" component="span" variant="body1">
                                {' '}
                                /{' '}
                                {subscription.plan.billingInterval === 'MONTHLY' ? 'month' : 'year'}
                            </Typography>
                        </Typography>
                    </Paper>

                    <Box
                        sx={{
                            display: 'grid',
                            gap: 2,
                            gridTemplateColumns: {
                                xs: '1fr',
                                md: 'repeat(2, 1fr)',
                                lg: 'repeat(4, 1fr)',
                            },
                        }}
                    >
                        <Paper sx={{ padding: 3 }} variant="outlined">
                            <Stack
                                direction="row"
                                spacing={1}
                                sx={{
                                    alignItems: 'center',
                                }}
                            >
                                <CalendarMonthRoundedIcon color="primary" />
                                <Typography variant="h6">Current period</Typography>
                            </Stack>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 2 }}
                                variant="body2"
                            >
                                Starts
                            </Typography>
                            <Typography>{formatDate(subscription.currentPeriodStart)}</Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1.5 }}
                                variant="body2"
                            >
                                Ends
                            </Typography>
                            <Typography>{formatDate(subscription.currentPeriodEnd)}</Typography>
                            {subscription.trialEndsAt && (
                                <>
                                    <Typography
                                        color="text.secondary"
                                        sx={{
                                            marginTop: 1.5,
                                        }}
                                        variant="body2"
                                    >
                                        Trial ends
                                    </Typography>
                                    <Typography>{formatDate(subscription.trialEndsAt)}</Typography>
                                </>
                            )}
                        </Paper>

                        <ResourceUsageCard
                            configuredLimit={subscription.plan.maxUsers}
                            entitlement={entitlements?.users}
                            loading={entitlementQuery.isPending}
                            title="Active users"
                        />

                        <ResourceUsageCard
                            configuredLimit={subscription.plan.maxProjects}
                            entitlement={entitlements?.projects}
                            loading={entitlementQuery.isPending}
                            title="Projects"
                        />

                        <Paper sx={{ padding: 3 }} variant="outlined">
                            <Stack
                                direction="row"
                                spacing={1}
                                sx={{
                                    alignItems: 'center',
                                }}
                            >
                                <StorageRoundedIcon color="primary" />
                                <Typography variant="h6">Storage</Typography>
                            </Stack>
                            <Typography
                                sx={{
                                    fontWeight: 700,
                                    marginTop: 2,
                                }}
                                variant="h5"
                            >
                                {formatLimit(subscription.plan.maxStorageMb, ' MB')}
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1 }}
                                variant="body2"
                            >
                                Configured plan capacity. Usage tracking will be added with the
                                storage module.
                            </Typography>
                        </Paper>
                    </Box>

                    <Paper sx={{ padding: 3 }} variant="outlined">
                        <Stack
                            direction={{
                                xs: 'column',
                                sm: 'row',
                            }}
                            spacing={2}
                            sx={{
                                justifyContent: 'space-between',
                            }}
                        >
                            <Box>
                                <Typography color="text.secondary" variant="body2">
                                    Subscription started
                                </Typography>
                                <Typography>{formatDate(subscription.startedAt)}</Typography>
                            </Box>
                            <Box>
                                <Typography color="text.secondary" variant="body2">
                                    Cancellation recorded
                                </Typography>
                                <Typography>{formatDate(subscription.cancelledAt)}</Typography>
                            </Box>
                            <Box>
                                <Typography color="text.secondary" variant="body2">
                                    Last updated
                                </Typography>
                                <Typography>{formatDate(subscription.updatedAt)}</Typography>
                            </Box>
                        </Stack>
                    </Paper>
                </Stack>
            )}
        </Box>
    )
}
