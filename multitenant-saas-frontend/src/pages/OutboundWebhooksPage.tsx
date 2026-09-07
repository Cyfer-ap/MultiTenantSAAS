import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import ReplayRoundedIcon from '@mui/icons-material/ReplayRounded'
import VpnKeyRoundedIcon from '@mui/icons-material/VpnKeyRounded'
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    FormControlLabel,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Snackbar,
    Stack,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { useEffect, useMemo, useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useArchiveOutboundWebhookEndpoint,
    useCreateOutboundWebhookEndpoint,
    useOutboundWebhookDeliveries,
    useOutboundWebhookDelivery,
    useOutboundWebhookEndpoints,
    useOutboundWebhookEventCatalog,
    useReplayOutboundWebhookDelivery,
    useRotateOutboundWebhookSecret,
    useUpdateOutboundWebhookEndpoint,
} from '../features/outbound-webhooks/hooks/useOutboundWebhooks'
import type {
    OutboundWebhookDelivery,
    OutboundWebhookDeliveryStatus,
    OutboundWebhookEndpoint,
    OutboundWebhookEndpointInput,
    OutboundWebhookEventType,
} from '../features/outbound-webhooks/types/outboundWebhooks'

const deliveryStatuses: OutboundWebhookDeliveryStatus[] = [
    'PENDING',
    'PROCESSING',
    'RETRY',
    'SENT',
    'FAILED',
]

function formatDateTime(value: string | null): string {
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

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function eventLabel(eventType: OutboundWebhookEventType): string {
    return eventType
        .split('.')
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(' ')
}

function statusColor(
    status: OutboundWebhookDeliveryStatus,
): 'default' | 'info' | 'success' | 'warning' | 'error' {
    if (status === 'SENT') {
        return 'success'
    }
    if (status === 'FAILED') {
        return 'error'
    }
    if (status === 'RETRY') {
        return 'warning'
    }
    if (status === 'PROCESSING') {
        return 'info'
    }
    return 'default'
}

interface EndpointDialogProps {
    open: boolean
    endpoint: OutboundWebhookEndpoint | null
    eventCatalog: OutboundWebhookEventType[]
    saving: boolean
    onClose: () => void
    onSave: (input: OutboundWebhookEndpointInput) => void
}

function EndpointDialog({
    open,
    endpoint,
    eventCatalog,
    saving,
    onClose,
    onSave,
}: EndpointDialogProps) {
    const [name, setName] = useState('')
    const [url, setUrl] = useState('')
    const [enabled, setEnabled] = useState(true)
    const [events, setEvents] = useState<OutboundWebhookEventType[]>([])

    useEffect(() => {
        if (!open) {
            return
        }

        setName(endpoint?.name ?? '')
        setUrl(endpoint?.url ?? '')
        setEnabled(endpoint?.enabled ?? true)
        setEvents(endpoint?.events ?? [])
    }, [endpoint, open])

    const canSave = name.trim().length > 0 && url.trim().length > 0 && events.length > 0 && !saving

    const toggleEvent = (eventType: OutboundWebhookEventType): void => {
        setEvents((current) =>
            current.includes(eventType)
                ? current.filter((item) => item !== eventType)
                : [...current, eventType],
        )
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={saving ? undefined : onClose} open={open}>
            <DialogTitle>
                {endpoint ? 'Edit webhook endpoint' : 'Create webhook endpoint'}
            </DialogTitle>
            <DialogContent>
                <Stack spacing={2.25} sx={{ pt: 1 }}>
                    <TextField
                        autoFocus
                        label="Endpoint name"
                        onChange={(event) => setName(event.target.value)}
                        value={name}
                    />
                    <TextField
                        helperText="HTTPS endpoints only. Private/internal network destinations are rejected by the backend."
                        label="Destination URL"
                        onChange={(event) => setUrl(event.target.value)}
                        placeholder="https://example.com/webhooks/saas"
                        value={url}
                    />
                    <FormControlLabel
                        control={
                            <Switch
                                checked={enabled}
                                onChange={(event) => setEnabled(event.target.checked)}
                            />
                        }
                        label="Enable deliveries"
                    />
                    <Box>
                        <Typography sx={{ fontWeight: 700 }} variant="subtitle2">
                            Events
                        </Typography>
                        <Typography color="text.secondary" variant="body2">
                            Choose at least one event to deliver to this endpoint.
                        </Typography>
                        <Stack sx={{ mt: 1 }}>
                            {eventCatalog.map((eventType) => (
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={events.includes(eventType)}
                                            onChange={() => toggleEvent(eventType)}
                                        />
                                    }
                                    key={eventType}
                                    label={eventLabel(eventType)}
                                />
                            ))}
                        </Stack>
                    </Box>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={saving} onClick={onClose}>
                    Cancel
                </Button>
                <Button
                    disabled={!canSave}
                    onClick={() =>
                        onSave({
                            name: name.trim(),
                            url: url.trim(),
                            enabled,
                            events,
                        })
                    }
                    variant="contained"
                >
                    {saving ? (
                        <CircularProgress color="inherit" size={20} />
                    ) : endpoint ? (
                        'Save'
                    ) : (
                        'Create'
                    )}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface SecretDialogProps {
    open: boolean
    secret: string
    title: string
    onClose: () => void
}

function SecretDialog({ open, secret, title, onClose }: SecretDialogProps) {
    const [copied, setCopied] = useState(false)

    useEffect(() => {
        if (open) {
            setCopied(false)
        }
    }, [open])

    const copySecret = async (): Promise<void> => {
        await navigator.clipboard.writeText(secret)
        setCopied(true)
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
            <DialogTitle>{title}</DialogTitle>
            <DialogContent>
                <Alert severity="warning" sx={{ mb: 2 }}>
                    Store this signing secret now. It will not be shown again after this dialog is
                    closed.
                </Alert>
                <TextField
                    fullWidth
                    slotProps={{ input: { readOnly: true } }}
                    label="Signing secret"
                    value={secret}
                />
            </DialogContent>
            <DialogActions>
                <Button onClick={() => void copySecret()} startIcon={<ContentCopyRoundedIcon />}>
                    {copied ? 'Copied' : 'Copy secret'}
                </Button>
                <Button onClick={onClose} variant="contained">
                    I stored it
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function OutboundWebhooksPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''

    const [endpointDialogOpen, setEndpointDialogOpen] = useState(false)
    const [editingEndpoint, setEditingEndpoint] = useState<OutboundWebhookEndpoint | null>(null)
    const [archiveTarget, setArchiveTarget] = useState<OutboundWebhookEndpoint | null>(null)
    const [secret, setSecret] = useState('')
    const [secretTitle, setSecretTitle] = useState('Webhook signing secret')
    const [feedback, setFeedback] = useState<string | null>(null)
    const [endpointFilter, setEndpointFilter] = useState('ALL')
    const [statusFilter, setStatusFilter] = useState<'ALL' | OutboundWebhookDeliveryStatus>('ALL')
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [selectedDeliveryId, setSelectedDeliveryId] = useState<string | null>(null)

    const endpointsQuery = useOutboundWebhookEndpoints(tenantId)
    const eventCatalogQuery = useOutboundWebhookEventCatalog(tenantId)
    const deliveriesQuery = useOutboundWebhookDeliveries(tenantId, {
        page,
        size,
        ...(endpointFilter === 'ALL' ? {} : { endpointId: endpointFilter }),
        ...(statusFilter === 'ALL' ? {} : { status: statusFilter }),
    })
    const deliveryQuery = useOutboundWebhookDelivery(tenantId, selectedDeliveryId)

    const createEndpoint = useCreateOutboundWebhookEndpoint(tenantId)
    const updateEndpoint = useUpdateOutboundWebhookEndpoint(tenantId)
    const rotateSecret = useRotateOutboundWebhookSecret(tenantId)
    const archiveEndpoint = useArchiveOutboundWebhookEndpoint(tenantId)
    const replayDelivery = useReplayOutboundWebhookDelivery(tenantId)

    const endpointById = useMemo(
        () => new Map((endpointsQuery.data ?? []).map((endpoint) => [endpoint.id, endpoint])),
        [endpointsQuery.data],
    )

    const openCreateDialog = (): void => {
        setEditingEndpoint(null)
        setEndpointDialogOpen(true)
    }

    const openEditDialog = (endpoint: OutboundWebhookEndpoint): void => {
        setEditingEndpoint(endpoint)
        setEndpointDialogOpen(true)
    }

    const saveEndpoint = (input: OutboundWebhookEndpointInput): void => {
        if (editingEndpoint) {
            updateEndpoint.mutate(
                { endpointId: editingEndpoint.id, input },
                {
                    onSuccess: () => {
                        setEndpointDialogOpen(false)
                        setEditingEndpoint(null)
                        setFeedback('Webhook endpoint updated.')
                    },
                },
            )
            return
        }

        createEndpoint.mutate(input, {
            onSuccess: (created) => {
                setEndpointDialogOpen(false)
                setSecret(created.signingSecret)
                setSecretTitle('New webhook signing secret')
                setFeedback('Webhook endpoint created.')
            },
        })
    }

    const rotateEndpointSecret = (endpoint: OutboundWebhookEndpoint): void => {
        rotateSecret.mutate(endpoint.id, {
            onSuccess: (rotated) => {
                setSecret(rotated.signingSecret)
                setSecretTitle(`New signing secret for ${endpoint.name}`)
                setFeedback('Webhook signing secret rotated.')
            },
        })
    }

    const confirmArchive = (): void => {
        if (!archiveTarget) {
            return
        }

        archiveEndpoint.mutate(archiveTarget.id, {
            onSuccess: () => {
                if (endpointFilter === archiveTarget.id) {
                    setEndpointFilter('ALL')
                    setPage(0)
                }
                setArchiveTarget(null)
                setFeedback('Webhook endpoint archived.')
            },
        })
    }

    const replaySelectedDelivery = (delivery: OutboundWebhookDelivery): void => {
        replayDelivery.mutate(delivery.id, {
            onSuccess: () => {
                setFeedback('Webhook delivery queued for replay.')
            },
        })
    }

    const mutationError =
        createEndpoint.error ??
        updateEndpoint.error ??
        rotateSecret.error ??
        archiveEndpoint.error ??
        replayDelivery.error

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Integrations
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Configure tenant outbound webhooks and inspect delivery health.
                    </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                    <Button
                        disabled={endpointsQuery.isFetching || deliveriesQuery.isFetching}
                        onClick={() => {
                            void Promise.all([endpointsQuery.refetch(), deliveriesQuery.refetch()])
                        }}
                        startIcon={<RefreshRoundedIcon />}
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                    <Button
                        disabled={!eventCatalogQuery.data?.length}
                        onClick={openCreateDialog}
                        startIcon={<AddRoundedIcon />}
                        variant="contained"
                    >
                        Add webhook
                    </Button>
                </Stack>
            </Stack>

            {mutationError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {getErrorMessage(
                        mutationError,
                        'The webhook operation could not be completed.',
                    )}
                </Alert>
            )}

            <Paper variant="outlined" sx={{ mt: 3, overflow: 'hidden' }}>
                <Box sx={{ p: 2.5 }}>
                    <Typography variant="h6">Webhook endpoints</Typography>
                    <Typography color="text.secondary" variant="body2">
                        Signing secrets are shown only once when created or rotated.
                    </Typography>
                </Box>
                <Divider />

                {endpointsQuery.isPending && (
                    <Stack sx={{ alignItems: 'center', p: 4 }}>
                        <CircularProgress size={28} />
                    </Stack>
                )}

                {endpointsQuery.isError && (
                    <Alert severity="error" sx={{ m: 2 }}>
                        {getErrorMessage(
                            endpointsQuery.error,
                            'Webhook endpoints could not be loaded.',
                        )}
                    </Alert>
                )}

                {endpointsQuery.isSuccess && (
                    <TableContainer>
                        <Table aria-label="Outbound webhook endpoints">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Endpoint</TableCell>
                                    <TableCell>Events</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Secret</TableCell>
                                    <TableCell align="right">Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {endpointsQuery.data.map((endpoint) => (
                                    <TableRow key={endpoint.id}>
                                        <TableCell>
                                            <Typography sx={{ fontWeight: 700 }} variant="body2">
                                                {endpoint.name}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {endpoint.url}
                                            </Typography>
                                        </TableCell>
                                        <TableCell>
                                            <Stack
                                                direction="row"
                                                sx={{ flexWrap: 'wrap', gap: 0.5 }}
                                            >
                                                {endpoint.events.map((eventType) => (
                                                    <Chip
                                                        key={eventType}
                                                        label={eventLabel(eventType)}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                ))}
                                            </Stack>
                                        </TableCell>
                                        <TableCell>
                                            <Chip
                                                color={endpoint.enabled ? 'success' : 'default'}
                                                label={endpoint.enabled ? 'Enabled' : 'Disabled'}
                                                size="small"
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Typography variant="body2">
                                                {endpoint.secretHint}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                Version {endpoint.secretVersion}
                                            </Typography>
                                        </TableCell>
                                        <TableCell align="right">
                                            <Stack
                                                direction="row"
                                                spacing={0.5}
                                                sx={{ justifyContent: 'flex-end' }}
                                            >
                                                <Button
                                                    onClick={() => openEditDialog(endpoint)}
                                                    size="small"
                                                    startIcon={<EditOutlinedIcon />}
                                                >
                                                    Edit
                                                </Button>
                                                <Button
                                                    disabled={rotateSecret.isPending}
                                                    onClick={() => rotateEndpointSecret(endpoint)}
                                                    size="small"
                                                    startIcon={<VpnKeyRoundedIcon />}
                                                >
                                                    Rotate
                                                </Button>
                                                <Button
                                                    color="error"
                                                    onClick={() => setArchiveTarget(endpoint)}
                                                    size="small"
                                                    startIcon={<DeleteOutlineRoundedIcon />}
                                                >
                                                    Archive
                                                </Button>
                                            </Stack>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                )}

                {endpointsQuery.isSuccess && endpointsQuery.data.length === 0 && (
                    <Box sx={{ p: 4, textAlign: 'center' }}>
                        <Typography variant="h6">No outbound webhooks</Typography>
                        <Typography color="text.secondary" variant="body2">
                            Add an endpoint to begin delivering tenant events to an external system.
                        </Typography>
                    </Box>
                )}
            </Paper>

            <Paper variant="outlined" sx={{ mt: 3, overflow: 'hidden' }}>
                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between', p: 2.5 }}
                >
                    <Box>
                        <Typography variant="h6">Delivery history</Typography>
                        <Typography color="text.secondary" variant="body2">
                            Inspect attempts, failures, stored payloads, and replay terminal
                            deliveries.
                        </Typography>
                    </Box>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                        <FormControl size="small" sx={{ minWidth: 190 }}>
                            <InputLabel id="webhook-endpoint-filter-label">Endpoint</InputLabel>
                            <Select
                                label="Endpoint"
                                labelId="webhook-endpoint-filter-label"
                                onChange={(event) => {
                                    setEndpointFilter(event.target.value)
                                    setPage(0)
                                }}
                                value={endpointFilter}
                            >
                                <MenuItem value="ALL">All endpoints</MenuItem>
                                {(endpointsQuery.data ?? []).map((endpoint) => (
                                    <MenuItem key={endpoint.id} value={endpoint.id}>
                                        {endpoint.name}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                        <FormControl size="small" sx={{ minWidth: 160 }}>
                            <InputLabel id="webhook-status-filter-label">Status</InputLabel>
                            <Select
                                label="Status"
                                labelId="webhook-status-filter-label"
                                onChange={(event) => {
                                    setStatusFilter(
                                        event.target.value as 'ALL' | OutboundWebhookDeliveryStatus,
                                    )
                                    setPage(0)
                                }}
                                value={statusFilter}
                            >
                                <MenuItem value="ALL">All statuses</MenuItem>
                                {deliveryStatuses.map((status) => (
                                    <MenuItem key={status} value={status}>
                                        {status}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>
                    </Stack>
                </Stack>
                <Divider />

                {deliveriesQuery.isPending && (
                    <Stack sx={{ alignItems: 'center', p: 4 }}>
                        <CircularProgress size={28} />
                    </Stack>
                )}

                {deliveriesQuery.isError && (
                    <Alert severity="error" sx={{ m: 2 }}>
                        {getErrorMessage(
                            deliveriesQuery.error,
                            'Webhook deliveries could not be loaded.',
                        )}
                    </Alert>
                )}

                {deliveriesQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Outbound webhook deliveries">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>Event</TableCell>
                                        <TableCell>Endpoint</TableCell>
                                        <TableCell>Status</TableCell>
                                        <TableCell>Attempts</TableCell>
                                        <TableCell>HTTP</TableCell>
                                        <TableCell>Created</TableCell>
                                        <TableCell align="right">Details</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {deliveriesQuery.data.content.map((delivery) => (
                                        <TableRow key={delivery.id}>
                                            <TableCell>{eventLabel(delivery.eventType)}</TableCell>
                                            <TableCell>{delivery.endpointName}</TableCell>
                                            <TableCell>
                                                <Chip
                                                    color={statusColor(delivery.status)}
                                                    label={delivery.status}
                                                    size="small"
                                                />
                                            </TableCell>
                                            <TableCell>
                                                {delivery.attemptCount}
                                                {delivery.replayCount > 0
                                                    ? ` · ${delivery.replayCount} replay${delivery.replayCount === 1 ? '' : 's'}`
                                                    : ''}
                                            </TableCell>
                                            <TableCell>{delivery.lastHttpStatus ?? '—'}</TableCell>
                                            <TableCell>
                                                {formatDateTime(delivery.createdAt)}
                                            </TableCell>
                                            <TableCell align="right">
                                                <Button
                                                    onClick={() =>
                                                        setSelectedDeliveryId(delivery.id)
                                                    }
                                                    size="small"
                                                >
                                                    View
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        {deliveriesQuery.data.content.length === 0 && (
                            <Box sx={{ p: 4, textAlign: 'center' }}>
                                <Typography variant="h6">No deliveries found</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Deliveries will appear here after subscribed tenant events
                                    occur.
                                </Typography>
                            </Box>
                        )}
                        <TablePagination
                            component="div"
                            count={deliveriesQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => setPage(nextPage)}
                            onRowsPerPageChange={(event) => {
                                setSize(Number(event.target.value))
                                setPage(0)
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[10, 20, 50]}
                        />
                    </>
                )}
            </Paper>

            <EndpointDialog
                endpoint={editingEndpoint}
                eventCatalog={eventCatalogQuery.data ?? []}
                onClose={() => {
                    setEndpointDialogOpen(false)
                    setEditingEndpoint(null)
                }}
                onSave={saveEndpoint}
                open={endpointDialogOpen}
                saving={createEndpoint.isPending || updateEndpoint.isPending}
            />

            <SecretDialog
                onClose={() => setSecret('')}
                open={Boolean(secret)}
                secret={secret}
                title={secretTitle}
            />

            <Dialog onClose={() => setArchiveTarget(null)} open={Boolean(archiveTarget)}>
                <DialogTitle>Archive webhook endpoint?</DialogTitle>
                <DialogContent>
                    <Typography>
                        {archiveTarget
                            ? `${archiveTarget.name} will stop receiving new events. Historical deliveries remain available.`
                            : ''}
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={archiveEndpoint.isPending}
                        onClick={() => setArchiveTarget(null)}
                    >
                        Cancel
                    </Button>
                    <Button
                        color="error"
                        disabled={archiveEndpoint.isPending}
                        onClick={confirmArchive}
                        variant="contained"
                    >
                        Archive
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog
                fullWidth
                maxWidth="md"
                onClose={() => setSelectedDeliveryId(null)}
                open={Boolean(selectedDeliveryId)}
            >
                <DialogTitle>Webhook delivery</DialogTitle>
                <DialogContent>
                    {deliveryQuery.isPending && (
                        <Stack sx={{ alignItems: 'center', p: 4 }}>
                            <CircularProgress size={28} />
                        </Stack>
                    )}
                    {deliveryQuery.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                deliveryQuery.error,
                                'Delivery details could not be loaded.',
                            )}
                        </Alert>
                    )}
                    {deliveryQuery.data && (
                        <Stack spacing={2.5}>
                            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
                                <Box sx={{ flex: 1 }}>
                                    <Typography color="text.secondary" variant="caption">
                                        Event
                                    </Typography>
                                    <Typography>
                                        {eventLabel(deliveryQuery.data.delivery.eventType)}
                                    </Typography>
                                </Box>
                                <Box sx={{ flex: 1 }}>
                                    <Typography color="text.secondary" variant="caption">
                                        Endpoint
                                    </Typography>
                                    <Typography>
                                        {deliveryQuery.data.delivery.endpointName}
                                    </Typography>
                                </Box>
                                <Box>
                                    <Typography color="text.secondary" variant="caption">
                                        Status
                                    </Typography>
                                    <Box sx={{ mt: 0.5 }}>
                                        <Chip
                                            color={statusColor(deliveryQuery.data.delivery.status)}
                                            label={deliveryQuery.data.delivery.status}
                                            size="small"
                                        />
                                    </Box>
                                </Box>
                            </Stack>

                            {deliveryQuery.data.delivery.lastError && (
                                <Alert severity="error">
                                    {deliveryQuery.data.delivery.lastError}
                                </Alert>
                            )}

                            <Box>
                                <Typography sx={{ fontWeight: 700 }} variant="subtitle2">
                                    Stored payload
                                </Typography>
                                <Box
                                    component="pre"
                                    sx={{
                                        bgcolor: 'action.hover',
                                        borderRadius: 1,
                                        fontFamily: 'monospace',
                                        fontSize: 13,
                                        m: 0,
                                        mt: 1,
                                        overflow: 'auto',
                                        p: 2,
                                        whiteSpace: 'pre-wrap',
                                        wordBreak: 'break-word',
                                    }}
                                >
                                    {deliveryQuery.data.payloadJson}
                                </Box>
                            </Box>

                            <Box>
                                <Typography sx={{ fontWeight: 700 }} variant="subtitle2">
                                    Attempts
                                </Typography>
                                <TableContainer component={Paper} sx={{ mt: 1 }} variant="outlined">
                                    <Table size="small">
                                        <TableHead>
                                            <TableRow>
                                                <TableCell>Cycle</TableCell>
                                                <TableCell>Attempt</TableCell>
                                                <TableCell>Outcome</TableCell>
                                                <TableCell>HTTP</TableCell>
                                                <TableCell>Started</TableCell>
                                                <TableCell>Error</TableCell>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {deliveryQuery.data.attempts.map((attempt) => (
                                                <TableRow key={attempt.id}>
                                                    <TableCell>{attempt.replayNumber}</TableCell>
                                                    <TableCell>{attempt.attemptNumber}</TableCell>
                                                    <TableCell>{attempt.outcome}</TableCell>
                                                    <TableCell>
                                                        {attempt.httpStatus ?? '—'}
                                                    </TableCell>
                                                    <TableCell>
                                                        {formatDateTime(attempt.startedAt)}
                                                    </TableCell>
                                                    <TableCell>{attempt.error ?? '—'}</TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                                {deliveryQuery.data.attempts.length === 0 && (
                                    <Typography
                                        color="text.secondary"
                                        sx={{ mt: 1 }}
                                        variant="body2"
                                    >
                                        No delivery attempt has been recorded yet.
                                    </Typography>
                                )}
                            </Box>
                        </Stack>
                    )}
                </DialogContent>
                <DialogActions>
                    {deliveryQuery.data &&
                        (deliveryQuery.data.delivery.status === 'SENT' ||
                            deliveryQuery.data.delivery.status === 'FAILED') && (
                            <Button
                                disabled={
                                    replayDelivery.isPending ||
                                    endpointById.get(deliveryQuery.data.delivery.endpointId)
                                        ?.enabled === false
                                }
                                onClick={() => replaySelectedDelivery(deliveryQuery.data.delivery)}
                                startIcon={<ReplayRoundedIcon />}
                            >
                                Replay
                            </Button>
                        )}
                    <Button onClick={() => setSelectedDeliveryId(null)} variant="contained">
                        Close
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                autoHideDuration={4000}
                message={feedback}
                onClose={() => setFeedback(null)}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
