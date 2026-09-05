import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded'
import CheckCircleRoundedIcon from '@mui/icons-material/CheckCircleRounded'
import CreditCardRoundedIcon from '@mui/icons-material/CreditCardRounded'
import PaymentsRoundedIcon from '@mui/icons-material/PaymentsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import StorageRoundedIcon from '@mui/icons-material/StorageRounded'
import { useState } from 'react'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    LinearProgress,
    Paper,
    Stack,
    Typography,
} from '@mui/material'

import { ApiClientError } from '../api/apiError'
import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useBillingCheckoutConfiguration,
    useCancelBillingSubscription,
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
    return new Intl.NumberFormat(undefined, { currency, style: 'currency' }).format(value)
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
    switch (status) {
        case 'ACTIVE':
            return 'success'
        case 'TRIALING':
            return 'info'
        case 'PAST_DUE':
            return 'warning'
        case 'CANCELLED':
        case 'EXPIRED':
            return 'error'
        default:
            return 'default'
    }
}

function statusMessage(status: TenantSubscriptionStatus): string | null {
    switch (status) {
        case 'TRIALING':
            return 'Your workspace is currently using a trial subscription.'
        case 'PAST_DUE':
            return 'This subscription is past due. Service may be restricted until the provider reports recovery.'
        case 'CANCELLED':
            return 'This subscription has been cancelled. Choose a new plan below to restore paid access.'
        case 'EXPIRED':
            return 'This subscription has expired. Choose a new plan below to restore paid access.'
        default:
            return null
    }
}

function providerLabel(provider: BillingProvider): string {
    return provider === 'RAZORPAY' ? 'Razorpay' : 'Stripe'
}

function providerDescription(provider: BillingProvider): string {
    return provider === 'STRIPE'
        ? 'Card checkout hosted securely by Stripe. Recommended when available.'
        : 'Hosted checkout through Razorpay for payment methods supported by the deployment.'
}

function canRequestCancellation(status: TenantSubscriptionStatus): boolean {
    return status === 'ACTIVE' || status === 'TRIALING' || status === 'PAST_DUE'
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
    const limit = entitlement?.limit ?? configuredLimit
    const used = entitlement?.used
    const unlimited = entitlement?.unlimited ?? limit === null
    const progress =
        used === undefined || unlimited
            ? 0
            : limit === null || limit <= 0
              ? 100
              : Math.min(100, (used / limit) * 100)

    let detail = 'Current usage is unavailable.'
    if (loading) {
        detail = 'Loading current usage…'
    } else if (entitlement?.overLimit) {
        detail = 'Usage is above the current plan limit.'
    } else if (entitlement?.limitReached) {
        detail = 'Plan limit reached.'
    } else if (entitlement && !entitlement.creationAllowed) {
        detail = 'New resource creation is restricted.'
    } else if (entitlement?.unlimited) {
        detail = 'Unlimited plan capacity.'
    } else if (entitlement) {
        detail = `${entitlement.remaining ?? 0} slot${entitlement.remaining === 1 ? '' : 's'} remaining.`
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
            <Typography sx={{ fontWeight: 700, marginTop: 2 }} variant="h5">
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
    const [selectedPlanCode, setSelectedPlanCode] = useState<string | null>(null)
    const [cancellationDialogOpen, setCancellationDialogOpen] = useState(false)

    const subscriptionQuery = useWorkspaceSubscription(tenantId)
    const entitlementQuery = useWorkspaceSubscriptionEntitlements(tenantId)
    const subscription = subscriptionQuery.data
    const entitlements = entitlementQuery.data
    const noSubscription =
        subscriptionQuery.error instanceof ApiClientError && subscriptionQuery.error.status === 404
    const canManageBilling = session?.role === 'TENANT_ADMIN' || session?.role === 'TENANT_MANAGER'
    const checkoutEligible =
        noSubscription || subscription?.status === 'CANCELLED' || subscription?.status === 'EXPIRED'
    const refreshing = subscriptionQuery.isFetching || entitlementQuery.isFetching

    const checkoutConfigurationQuery = useBillingCheckoutConfiguration(
        tenantId,
        canManageBilling && checkoutEligible,
    )
    const checkoutMutation = useCreateBillingCheckout()
    const cancellationMutation = useCancelBillingSubscription()
    const checkoutOutcome = new URLSearchParams(window.location.search).get('checkout')
    const selectedPlan = checkoutConfigurationQuery.data?.plans.find(
        (plan) => plan.code === selectedPlanCode,
    )
    const lifecycleMessage = subscription ? statusMessage(subscription.status) : null
    const cancellationAvailable = Boolean(
        subscription &&
        canManageBilling &&
        !subscription.cancelAtPeriodEnd &&
        canRequestCancellation(subscription.status),
    )

    const beginCheckout = (plan: SubscriptionPlan, provider: BillingProvider) => {
        checkoutMutation.mutate(
            {
                tenantId,
                input: { planCode: plan.code, provider },
            },
            {
                onSuccess: (checkout) => {
                    window.location.assign(checkout.checkoutUrl)
                },
            },
        )
    }

    const requestCancellation = () => {
        cancellationMutation.mutate(
            { tenantId },
            {
                onSuccess: () => {
                    setCancellationDialogOpen(false)
                    void Promise.all([subscriptionQuery.refetch(), entitlementQuery.refetch()])
                },
            },
        )
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Subscription & billing
                    </Typography>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Manage your workspace plan, usage limits, billing lifecycle, and secure
                        checkout.
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

            {checkoutOutcome === 'success' && (
                <Alert icon={<CheckCircleRoundedIcon />} severity="success" sx={{ marginTop: 3 }}>
                    Checkout completed. We are confirming the subscription from the signed provider
                    webhook. If the new plan is not visible yet, refresh this page shortly.
                </Alert>
            )}
            {(checkoutOutcome === 'cancelled' || checkoutOutcome === 'cancel') && (
                <Alert severity="info" sx={{ marginTop: 3 }}>
                    Checkout was cancelled. No subscription change was made; you can choose a plan
                    again whenever you are ready.
                </Alert>
            )}

            {subscriptionQuery.isPending && (
                <Paper sx={{ marginTop: 3, padding: 8, textAlign: 'center' }} variant="outlined">
                    <CircularProgress />
                    <Typography color="text.secondary" sx={{ marginTop: 2 }}>
                        Loading subscription…
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
                            Cancellation is scheduled for the end of the current billing period on{' '}
                            {formatDate(subscription.currentPeriodEnd)}. Provider webhooks remain
                            the source of truth for the final state.
                        </Alert>
                    )}
                    {cancellationMutation.isSuccess && (
                        <Alert severity="success">
                            Cancellation was requested from the payment provider. The local status
                            will update after the signed provider webhook is received.
                        </Alert>
                    )}
                    {cancellationMutation.isError && (
                        <Alert severity="error">
                            {cancellationMutation.error instanceof Error
                                ? cancellationMutation.error.message
                                : 'The cancellation request could not be submitted.'}
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
                            direction={{ xs: 'column', md: 'row' }}
                            spacing={3}
                            sx={{
                                alignItems: { md: 'flex-start' },
                                justifyContent: 'space-between',
                            }}
                        >
                            <Box sx={{ maxWidth: 760 }}>
                                <Typography color="text.secondary" variant="overline">
                                    Current plan
                                </Typography>
                                <Typography component="h2" variant="h5">
                                    {subscription.plan.name}
                                </Typography>
                                <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                                    {subscription.plan.code}
                                </Typography>
                                {subscription.plan.description && (
                                    <Typography sx={{ marginTop: 1.5 }}>
                                        {subscription.plan.description}
                                    </Typography>
                                )}
                                <Typography sx={{ fontWeight: 700, marginTop: 2 }} variant="h5">
                                    {formatMoney(
                                        subscription.plan.price,
                                        subscription.plan.currency,
                                    )}
                                    <Typography
                                        color="text.secondary"
                                        component="span"
                                        variant="body1"
                                    >
                                        {' '}
                                        /{' '}
                                        {subscription.plan.billingInterval === 'MONTHLY'
                                            ? 'month'
                                            : 'year'}
                                    </Typography>
                                </Typography>
                            </Box>
                            <Stack spacing={1.5} sx={{ minWidth: { md: 220 } }}>
                                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                                    <Chip
                                        color={statusColor(subscription.status)}
                                        label={subscription.status.replaceAll('_', ' ')}
                                        variant="outlined"
                                    />
                                    <Chip
                                        label={
                                            subscription.plan.billingInterval === 'MONTHLY'
                                                ? 'Monthly billing'
                                                : 'Yearly billing'
                                        }
                                        variant="outlined"
                                    />
                                </Box>
                                {cancellationAvailable && (
                                    <Button
                                        color="error"
                                        onClick={() => {
                                            setCancellationDialogOpen(true)
                                        }}
                                        variant="outlined"
                                    >
                                        Cancel subscription
                                    </Button>
                                )}
                            </Stack>
                        </Stack>
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
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
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
                                        sx={{ marginTop: 1.5 }}
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
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                <StorageRoundedIcon color="primary" />
                                <Typography variant="h6">Storage</Typography>
                            </Stack>
                            <Typography sx={{ fontWeight: 700, marginTop: 2 }} variant="h5">
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
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={2}
                            sx={{ justifyContent: 'space-between' }}
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

            {noSubscription && (
                <Paper sx={{ marginTop: 3, padding: 4 }} variant="outlined">
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={2}
                        sx={{ alignItems: { sm: 'center' } }}
                    >
                        <PaymentsRoundedIcon color="primary" sx={{ fontSize: 44 }} />
                        <Box>
                            <Typography variant="h6">Start a workspace subscription</Typography>
                            <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                                This workspace does not have an active subscription. Compare the
                                available plans below and choose the one that fits your team.
                            </Typography>
                        </Box>
                    </Stack>
                </Paper>
            )}

            {canManageBilling && checkoutEligible && checkoutConfigurationQuery.isPending && (
                <Paper sx={{ marginTop: 3, padding: 4, textAlign: 'center' }} variant="outlined">
                    <CircularProgress size={28} />
                    <Typography color="text.secondary" sx={{ marginTop: 1.5 }}>
                        Loading subscription plans…
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
                <Stack spacing={3} sx={{ marginTop: 3 }}>
                    <Paper sx={{ padding: 3 }} variant="outlined">
                        <Typography color="primary" variant="overline">
                            Step 1 of 2
                        </Typography>
                        <Typography component="h2" variant="h5">
                            Choose your plan
                        </Typography>
                        <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                            Compare plan capacity first. Payment methods are shown only after you
                            select a plan.
                        </Typography>

                        {checkoutConfigurationQuery.data.plans.length === 0 ? (
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
                                {checkoutConfigurationQuery.data.plans.map((plan) => {
                                    const selected = selectedPlanCode === plan.code
                                    return (
                                        <Paper
                                            key={plan.id}
                                            sx={{
                                                borderColor: selected ? 'primary.main' : undefined,
                                                borderWidth: selected ? 2 : 1,
                                                padding: 3,
                                            }}
                                            variant="outlined"
                                        >
                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                sx={{
                                                    alignItems: 'flex-start',
                                                    justifyContent: 'space-between',
                                                }}
                                            >
                                                <Box>
                                                    <Typography component="h3" variant="h6">
                                                        {plan.name}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="body2"
                                                    >
                                                        {plan.code}
                                                    </Typography>
                                                </Box>
                                                {selected && (
                                                    <Chip
                                                        color="primary"
                                                        label="Selected"
                                                        size="small"
                                                    />
                                                )}
                                            </Stack>
                                            {plan.description && (
                                                <Typography sx={{ marginTop: 1.5 }}>
                                                    {plan.description}
                                                </Typography>
                                            )}
                                            <Typography
                                                sx={{ fontWeight: 700, marginTop: 2 }}
                                                variant="h4"
                                            >
                                                {formatMoney(plan.price, plan.currency)}
                                                <Typography
                                                    color="text.secondary"
                                                    component="span"
                                                    variant="body2"
                                                >
                                                    {' '}
                                                    /{' '}
                                                    {plan.billingInterval === 'MONTHLY'
                                                        ? 'month'
                                                        : 'year'}
                                                </Typography>
                                            </Typography>
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                    flexWrap: 'wrap',
                                                    gap: 1,
                                                    marginTop: 2,
                                                }}
                                            >
                                                <Chip
                                                    label={`${formatLimit(plan.maxUsers)} users`}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                                <Chip
                                                    label={`${formatLimit(plan.maxProjects)} projects`}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                                <Chip
                                                    label={`${formatLimit(plan.maxStorageMb)} MB storage`}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </Box>
                                            <Button
                                                fullWidth
                                                onClick={() => {
                                                    setSelectedPlanCode(plan.code)
                                                }}
                                                sx={{ marginTop: 3 }}
                                                variant={selected ? 'contained' : 'outlined'}
                                            >
                                                {selected ? 'Plan selected' : `Choose ${plan.name}`}
                                            </Button>
                                        </Paper>
                                    )
                                })}
                            </Box>
                        )}
                    </Paper>

                    {selectedPlan && (
                        <Paper sx={{ padding: 3 }} variant="outlined">
                            <Typography color="primary" variant="overline">
                                Step 2 of 2
                            </Typography>
                            <Typography component="h2" variant="h5">
                                Select a payment method
                            </Typography>
                            <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                                You selected {selectedPlan.name} at{' '}
                                {formatMoney(selectedPlan.price, selectedPlan.currency)} per{' '}
                                {selectedPlan.billingInterval === 'MONTHLY' ? 'month' : 'year'}.
                                Checkout opens on the provider&apos;s secure hosted page.
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
                                    {checkoutConfigurationQuery.data.providers.map((provider) => (
                                        <Paper
                                            key={provider}
                                            sx={{ padding: 2.5 }}
                                            variant="outlined"
                                        >
                                            <Stack
                                                direction="row"
                                                spacing={1}
                                                sx={{
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                }}
                                            >
                                                <Stack
                                                    direction="row"
                                                    spacing={1}
                                                    sx={{ alignItems: 'center' }}
                                                >
                                                    <CreditCardRoundedIcon color="primary" />
                                                    <Typography variant="h6">
                                                        {providerLabel(provider)}
                                                    </Typography>
                                                </Stack>
                                                <Chip
                                                    color={
                                                        provider === 'STRIPE'
                                                            ? 'primary'
                                                            : 'default'
                                                    }
                                                    label={
                                                        provider === 'STRIPE'
                                                            ? 'Recommended'
                                                            : 'Available'
                                                    }
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </Stack>
                                            <Typography
                                                color="text.secondary"
                                                sx={{ marginTop: 1.5 }}
                                                variant="body2"
                                            >
                                                {providerDescription(provider)}
                                            </Typography>
                                            <Button
                                                disabled={checkoutMutation.isPending}
                                                fullWidth
                                                onClick={() => {
                                                    beginCheckout(selectedPlan, provider)
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
                                                sx={{ marginTop: 2 }}
                                                variant="contained"
                                            >
                                                Pay securely with {providerLabel(provider)}
                                            </Button>
                                        </Paper>
                                    ))}
                                </Box>
                            )}
                        </Paper>
                    )}
                </Stack>
            )}

            {!canManageBilling && checkoutEligible && (
                <Alert severity="info" sx={{ marginTop: 3 }}>
                    A tenant administrator or manager must choose and purchase a subscription for
                    this workspace.
                </Alert>
            )}

            <Dialog
                onClose={() => {
                    if (!cancellationMutation.isPending) {
                        setCancellationDialogOpen(false)
                    }
                }}
                open={cancellationDialogOpen}
            >
                <DialogTitle>Cancel subscription?</DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        The request is sent to the linked payment provider and is normally applied
                        at the end of the current billing period. Local subscription state remains
                        webhook-authoritative, so the page may not change immediately.
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={cancellationMutation.isPending}
                        onClick={() => {
                            setCancellationDialogOpen(false)
                        }}
                    >
                        Keep subscription
                    </Button>
                    <Button
                        color="error"
                        disabled={cancellationMutation.isPending}
                        onClick={requestCancellation}
                        variant="contained"
                    >
                        {cancellationMutation.isPending ? 'Requesting…' : 'Confirm cancellation'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    )
}
