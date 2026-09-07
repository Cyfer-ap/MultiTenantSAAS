import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Paper,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material'

import type {
    TenantSubscriptionHistoryEntry,
    TenantSubscriptionHistoryEventType,
    TenantSubscriptionHistoryPage,
} from '../types/subscriptionHistory'
import type { TenantSubscriptionStatus } from '../types/subscriptions'

interface SubscriptionHistoryPanelProps {
    data?: TenantSubscriptionHistoryPage
    error?: unknown
    fetching?: boolean
    loading?: boolean
    onPageChange: (page: number) => void
    onRefresh: () => void
    page: number
    showProviderReference?: boolean
    title?: string
}

function formatDate(value: string | null): string {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function formatMoney(value: number, currency: string): string {
    return new Intl.NumberFormat(undefined, {
        currency,
        style: 'currency',
    }).format(value)
}

function eventLabel(eventType: TenantSubscriptionHistoryEventType): string {
    switch (eventType) {
        case 'MIGRATED_CURRENT_STATE':
            return 'Imported baseline'
        case 'STARTED':
            return 'Subscription started'
        case 'PLAN_CHANGED':
            return 'Plan changed'
        case 'LIFECYCLE_UPDATED':
            return 'Lifecycle updated'
        case 'PROVIDER_SYNCHRONIZED':
            return 'Provider update'
        case 'PROVIDER_RECONCILED':
            return 'Provider reconciliation'
    }
}

function statusColor(
    status: TenantSubscriptionStatus,
): 'success' | 'info' | 'warning' | 'error' | 'default' {
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

function providerLabel(entry: TenantSubscriptionHistoryEntry): string {
    if (entry.billingProvider === 'STRIPE') return 'Stripe'
    if (entry.billingProvider === 'RAZORPAY') return 'Razorpay'
    return 'Internal'
}

export function SubscriptionHistoryPanel({
    data,
    error,
    fetching = false,
    loading = false,
    onPageChange,
    onRefresh,
    page,
    showProviderReference = false,
    title = 'Subscription history',
}: SubscriptionHistoryPanelProps) {
    const totalPages = data?.totalPages ?? 0
    const totalElements = data?.totalElements ?? 0

    return (
        <Paper sx={{ p: { xs: 2, md: 3 } }} variant="outlined">
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
                    <HistoryRoundedIcon color="primary" sx={{ mt: 0.25 }} />
                    <Box>
                        <Typography component="h2" variant="h6">
                            {title}
                        </Typography>
                        <Typography color="text.secondary" variant="body2">
                            Immutable snapshots preserve plan and lifecycle changes, including
                            verified provider updates and reconciliation.
                        </Typography>
                    </Box>
                </Stack>
                <Button
                    disabled={fetching}
                    onClick={onRefresh}
                    size="small"
                    startIcon={
                        fetching ? (
                            <CircularProgress color="inherit" size={14} />
                        ) : (
                            <RefreshRoundedIcon />
                        )
                    }
                    variant="outlined"
                >
                    Refresh history
                </Button>
            </Stack>

            {error && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {error instanceof Error
                        ? error.message
                        : 'Subscription history could not be loaded.'}
                </Alert>
            )}

            {loading && (
                <Stack sx={{ alignItems: 'center', py: 6 }}>
                    <CircularProgress size={28} />
                    <Typography color="text.secondary" sx={{ mt: 1.5 }}>
                        Loading subscription history…
                    </Typography>
                </Stack>
            )}

            {!loading && !error && data && data.content.length === 0 && (
                <Box sx={{ py: 5, textAlign: 'center' }}>
                    <Typography color="text.secondary">
                        No subscription history has been recorded yet.
                    </Typography>
                </Box>
            )}

            {!loading && !error && data && data.content.length > 0 && (
                <>
                    <TableContainer sx={{ mt: 2 }}>
                        <Table aria-label="Subscription history">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Recorded</TableCell>
                                    <TableCell>Change</TableCell>
                                    <TableCell>Plan</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Source</TableCell>
                                    <TableCell>Billing period</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {data.content.map((entry) => (
                                    <TableRow hover key={entry.id}>
                                        <TableCell sx={{ minWidth: 170 }}>
                                            {formatDate(entry.recordedAt)}
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 170 }}>
                                            <Typography sx={{ fontWeight: 600 }} variant="body2">
                                                {eventLabel(entry.eventType)}
                                            </Typography>
                                            {entry.cancelAtPeriodEnd && (
                                                <Typography color="warning.main" variant="caption">
                                                    Cancels at period end
                                                </Typography>
                                            )}
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 190 }}>
                                            <Typography sx={{ fontWeight: 600 }} variant="body2">
                                                {entry.planName}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {entry.planCode} ·{' '}
                                                {formatMoney(entry.price, entry.currency)} /{' '}
                                                {entry.billingInterval === 'MONTHLY'
                                                    ? 'month'
                                                    : 'year'}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Chip
                                                color={statusColor(entry.status)}
                                                label={entry.status.replaceAll('_', ' ')}
                                                size="small"
                                                variant="outlined"
                                            />
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 160 }}>
                                            <Typography variant="body2">
                                                {providerLabel(entry)}
                                            </Typography>
                                            {showProviderReference &&
                                                entry.providerSubscriptionId && (
                                                    <Typography
                                                        color="text.secondary"
                                                        sx={{
                                                            fontFamily: 'monospace',
                                                            overflowWrap: 'anywhere',
                                                        }}
                                                        variant="caption"
                                                    >
                                                        {entry.providerSubscriptionId}
                                                    </Typography>
                                                )}
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 220 }}>
                                            <Typography variant="body2">
                                                {formatDate(entry.currentPeriodStart)}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                through {formatDate(entry.currentPeriodEnd)}
                                            </Typography>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>

                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1.5}
                        sx={{
                            alignItems: { sm: 'center' },
                            justifyContent: 'space-between',
                            mt: 2,
                        }}
                    >
                        <Typography color="text.secondary" variant="body2">
                            Page {totalPages === 0 ? 0 : page + 1} of {totalPages} · {totalElements}{' '}
                            record{totalElements === 1 ? '' : 's'}
                        </Typography>
                        <Stack direction="row" spacing={1}>
                            <Button
                                disabled={data.first || page <= 0 || fetching}
                                onClick={() => {
                                    onPageChange(Math.max(0, page - 1))
                                }}
                                size="small"
                                variant="outlined"
                            >
                                Previous
                            </Button>
                            <Button
                                disabled={data.last || page + 1 >= totalPages || fetching}
                                onClick={() => {
                                    onPageChange(page + 1)
                                }}
                                size="small"
                                variant="outlined"
                            >
                                Next
                            </Button>
                        </Stack>
                    </Stack>
                </>
            )}
        </Paper>
    )
}
