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
    Paper,
    Stack,
    Typography,
} from '@mui/material'

import { ApiClientError } from '../api/apiError'
import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useWorkspaceSubscription,
} from '../features/subscriptions/hooks/useWorkspaceSubscription'
import type {
    TenantSubscriptionStatus,
} from '../features/subscriptions/types/subscriptions'

type StatusColor =
    | 'success'
    | 'info'
    | 'warning'
    | 'error'
    | 'default'

function formatMoney(
    value: number,
    currency: string,
): string {
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

function formatLimit(
    value: number | null,
    suffix = '',
): string {
    return value === null
        ? 'Unlimited'
        : `${value.toLocaleString()}${suffix}`
}

function statusColor(
    status: TenantSubscriptionStatus,
): StatusColor {
    if (status === 'ACTIVE') {
        return 'success'
    }

    if (status === 'TRIALING') {
        return 'info'
    }

    if (status === 'PAST_DUE') {
        return 'warning'
    }

    if (
        status === 'CANCELLED' ||
        status === 'EXPIRED'
    ) {
        return 'error'
    }

    return 'default'
}

function statusMessage(
    status: TenantSubscriptionStatus,
): string | null {
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

export function TenantSubscriptionPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const subscriptionQuery =
        useWorkspaceSubscription(tenantId)
    const noSubscription =
        subscriptionQuery.error instanceof
            ApiClientError &&
        subscriptionQuery.error.status === 404
    const subscription = subscriptionQuery.data
    const lifecycleMessage = subscription
        ? statusMessage(subscription.status)
        : null

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
                    <Typography
                        component="h1"
                        variant="h4"
                    >
                        Subscription
                    </Typography>
                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Review your workspace plan,
                        billing period, and configured
                        limits.
                    </Typography>
                </Box>

                <Button
                    disabled={
                        subscriptionQuery.isFetching
                    }
                    onClick={() => {
                        void subscriptionQuery.refetch()
                    }}
                    startIcon={
                        subscriptionQuery.isFetching
                            ? (
                                <CircularProgress
                                    color="inherit"
                                    size={16}
                                />
                            )
                            : (
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
                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 2 }}
                    >
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
                    <PaymentsRoundedIcon
                        color="disabled"
                        sx={{ fontSize: 52 }}
                    />
                    <Typography
                        sx={{ marginTop: 1 }}
                        variant="h6"
                    >
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
                        A platform administrator has not
                        assigned a subscription plan to
                        this workspace yet.
                    </Typography>
                </Paper>
            )}

            {subscriptionQuery.isError &&
                !noSubscription && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void subscriptionQuery
                                        .refetch()
                                }}
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ marginTop: 3 }}
                    >
                        {subscriptionQuery.error
                            instanceof Error
                            ? subscriptionQuery.error
                                .message
                            : 'The subscription could not be loaded.'}
                    </Alert>
                )}

            {subscription && (
                <Stack
                    spacing={2}
                    sx={{ marginTop: 3 }}
                >
                    {lifecycleMessage && (
                        <Alert
                            severity={
                                subscription.status ===
                                'TRIALING'
                                    ? 'info'
                                    : subscription.status ===
                                        'PAST_DUE'
                                        ? 'warning'
                                        : 'error'
                            }
                        >
                            {lifecycleMessage}
                        </Alert>
                    )}

                    {subscription.cancelAtPeriodEnd && (
                        <Alert severity="warning">
                            This subscription is scheduled
                            to end when the current billing
                            period finishes on{' '}
                            {formatDate(
                                subscription
                                    .currentPeriodEnd,
                            )}
                            .
                        </Alert>
                    )}

                    {subscription.plan.status ===
                        'INACTIVE' && (
                        <Alert severity="warning">
                            This plan is no longer offered
                            to new subscriptions. Your
                            current subscription details
                            remain visible.
                        </Alert>
                    )}

                    <Paper
                        sx={{ padding: 3 }}
                        variant="outlined"
                    >
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
                                justifyContent:
                                    'space-between',
                            }}
                        >
                            <Box>
                                <Typography
                                    component="h2"
                                    variant="h5"
                                >
                                    {
                                        subscription.plan
                                            .name
                                    }
                                </Typography>
                                <Typography
                                    color="text.secondary"
                                    sx={{
                                        marginTop: 0.5,
                                    }}
                                >
                                    {
                                        subscription.plan
                                            .code
                                    }
                                </Typography>
                                {subscription.plan
                                    .description && (
                                    <Typography
                                        sx={{
                                            marginTop: 1.5,
                                            maxWidth: 720,
                                        }}
                                    >
                                        {
                                            subscription
                                                .plan
                                                .description
                                        }
                                    </Typography>
                                )}
                            </Box>

                            <Stack
                                direction="row"
                                spacing={1}
                            >
                                <Chip
                                    color={statusColor(
                                        subscription.status,
                                    )}
                                    label={subscription.status
                                        .replaceAll('_', ' ')}
                                    variant="outlined"
                                />
                                <Chip
                                    label={
                                        subscription.plan
                                            .billingInterval ===
                                        'MONTHLY'
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

                        <Typography
                            sx={{ fontWeight: 700 }}
                            variant="h5"
                        >
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
                                {subscription.plan
                                    .billingInterval ===
                                'MONTHLY'
                                    ? 'month'
                                    : 'year'}
                            </Typography>
                        </Typography>
                    </Paper>

                    <Box
                        sx={{
                            display: 'grid',
                            gap: 2,
                            gridTemplateColumns: {
                                xs: '1fr',
                                md: 'repeat(3, 1fr)',
                            },
                        }}
                    >
                        <Paper
                            sx={{ padding: 3 }}
                            variant="outlined"
                        >
                            <Stack
                                direction="row"
                                spacing={1}
                                sx={{
                                    alignItems: 'center',
                                }}
                            >
                                <CalendarMonthRoundedIcon
                                    color="primary"
                                />
                                <Typography variant="h6">
                                    Current period
                                </Typography>
                            </Stack>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 2 }}
                                variant="body2"
                            >
                                Starts
                            </Typography>
                            <Typography>
                                {formatDate(
                                    subscription
                                        .currentPeriodStart,
                                )}
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1.5 }}
                                variant="body2"
                            >
                                Ends
                            </Typography>
                            <Typography>
                                {formatDate(
                                    subscription
                                        .currentPeriodEnd,
                                )}
                            </Typography>
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
                                    <Typography>
                                        {formatDate(
                                            subscription
                                                .trialEndsAt,
                                        )}
                                    </Typography>
                                </>
                            )}
                        </Paper>

                        <Paper
                            sx={{ padding: 3 }}
                            variant="outlined"
                        >
                            <Typography variant="h6">
                                Workspace limits
                            </Typography>
                            <Stack
                                spacing={1.5}
                                sx={{ marginTop: 2 }}
                            >
                                <Stack
                                    direction="row"
                                    sx={{
                                        justifyContent:
                                            'space-between',
                                    }}
                                >
                                    <Typography color="text.secondary">
                                        Users
                                    </Typography>
                                    <Typography
                                        sx={{
                                            fontWeight: 600,
                                        }}
                                    >
                                        {formatLimit(
                                            subscription
                                                .plan
                                                .maxUsers,
                                        )}
                                    </Typography>
                                </Stack>
                                <Stack
                                    direction="row"
                                    sx={{
                                        justifyContent:
                                            'space-between',
                                    }}
                                >
                                    <Typography color="text.secondary">
                                        Projects
                                    </Typography>
                                    <Typography
                                        sx={{
                                            fontWeight: 600,
                                        }}
                                    >
                                        {formatLimit(
                                            subscription
                                                .plan
                                                .maxProjects,
                                        )}
                                    </Typography>
                                </Stack>
                            </Stack>
                        </Paper>

                        <Paper
                            sx={{ padding: 3 }}
                            variant="outlined"
                        >
                            <Stack
                                direction="row"
                                spacing={1}
                                sx={{
                                    alignItems: 'center',
                                }}
                            >
                                <StorageRoundedIcon
                                    color="primary"
                                />
                                <Typography variant="h6">
                                    Storage
                                </Typography>
                            </Stack>
                            <Typography
                                sx={{
                                    fontWeight: 700,
                                    marginTop: 2,
                                }}
                                variant="h5"
                            >
                                {formatLimit(
                                    subscription.plan
                                        .maxStorageMb,
                                    ' MB',
                                )}
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1 }}
                                variant="body2"
                            >
                                Configured plan capacity.
                                Usage tracking will be added
                                with the storage module.
                            </Typography>
                        </Paper>
                    </Box>

                    <Paper
                        sx={{ padding: 3 }}
                        variant="outlined"
                    >
                        <Stack
                            direction={{
                                xs: 'column',
                                sm: 'row',
                            }}
                            spacing={2}
                            sx={{
                                justifyContent:
                                    'space-between',
                            }}
                        >
                            <Box>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                >
                                    Subscription started
                                </Typography>
                                <Typography>
                                    {formatDate(
                                        subscription
                                            .startedAt,
                                    )}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                >
                                    Cancellation recorded
                                </Typography>
                                <Typography>
                                    {formatDate(
                                        subscription
                                            .cancelledAt,
                                    )}
                                </Typography>
                            </Box>
                            <Box>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                >
                                    Last updated
                                </Typography>
                                <Typography>
                                    {formatDate(
                                        subscription
                                            .updatedAt,
                                    )}
                                </Typography>
                            </Box>
                        </Stack>
                    </Paper>
                </Stack>
            )}
        </Box>
    )
}
