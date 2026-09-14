import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Paper,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material'
import { Link } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { useMyWork } from '../hooks/useMyWork'
import type { MyWorkAttention, MyWorkItem } from '../types/myWork'

const attentionOrder: MyWorkAttention[] = [
    'OVERDUE',
    'BLOCKED',
    'DUE_SOON',
    'IN_PROGRESS',
    'ASSIGNED',
]

const attentionLabels: Record<MyWorkAttention, string> = {
    OVERDUE: 'Overdue',
    BLOCKED: 'Blocked',
    DUE_SOON: 'Due soon',
    IN_PROGRESS: 'In progress',
    ASSIGNED: 'Assigned',
}

const statusLabels: Record<MyWorkItem['status'], string> = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
}

const priorityLabels: Record<MyWorkItem['priority'], string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    URGENT: 'Urgent',
}

function formatDueAt(value: string | null): string {
    if (!value) return 'No due date'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return 'Due date unavailable'

    return `Due ${new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)}`
}

function getAttentionColor(
    attention: MyWorkAttention,
): 'default' | 'error' | 'warning' | 'info' | 'primary' {
    if (attention === 'OVERDUE') return 'error'
    if (attention === 'BLOCKED') return 'warning'
    if (attention === 'DUE_SOON') return 'warning'
    if (attention === 'IN_PROGRESS') return 'primary'
    return 'default'
}

function getPriorityColor(
    priority: MyWorkItem['priority'],
): 'default' | 'error' | 'warning' | 'info' {
    if (priority === 'URGENT') return 'error'
    if (priority === 'HIGH') return 'warning'
    if (priority === 'MEDIUM') return 'info'
    return 'default'
}

function SummaryCard({
    label,
    value,
    emphasis,
}: {
    label: string
    value: number
    emphasis?: boolean
}) {
    return (
        <Paper variant="outlined" sx={{ padding: 2 }}>
            <Typography color="text.secondary" variant="caption">
                {label}
            </Typography>
            <Typography color={emphasis && value > 0 ? 'error.main' : 'text.primary'} variant="h4">
                {value}
            </Typography>
        </Paper>
    )
}

function WorkItemRow({ item }: { item: MyWorkItem }) {
    return (
        <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
            <Button
                component={Link}
                to={item.targetUrl}
                sx={{
                    color: 'inherit',
                    display: 'block',
                    padding: 2,
                    textAlign: 'left',
                    textTransform: 'none',
                    width: '100%',
                }}
            >
                <Stack spacing={1}>
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1}
                        sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
                    >
                        <Box sx={{ minWidth: 0 }}>
                            <Typography sx={{ fontWeight: 700 }}>{item.title}</Typography>
                            <Typography color="text.secondary" variant="body2">
                                {item.projectName}
                            </Typography>
                        </Box>
                        <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                            <Chip
                                color={getAttentionColor(item.attention)}
                                label={attentionLabels[item.attention]}
                                size="small"
                                variant="outlined"
                            />
                            <Chip
                                color={getPriorityColor(item.priority)}
                                label={priorityLabels[item.priority]}
                                size="small"
                            />
                        </Stack>
                    </Stack>
                    <Stack direction="row" spacing={1.5} sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                        <Typography color="text.secondary" variant="caption">
                            {statusLabels[item.status]}
                        </Typography>
                        <Typography color="text.secondary" variant="caption">
                            {formatDueAt(item.dueAt)}
                        </Typography>
                    </Stack>
                </Stack>
            </Button>
        </Paper>
    )
}

export function MyWorkPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const workQuery = useMyWork(tenantId, 100)

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        My Work
                    </Typography>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Your assigned open tasks, ordered by what needs attention first.
                    </Typography>
                </Box>
                <Button
                    disabled={workQuery.isFetching}
                    onClick={() => void workQuery.refetch()}
                    startIcon={
                        workQuery.isFetching ? (
                            <CircularProgress color="inherit" size={16} />
                        ) : (
                            <RefreshRoundedIcon />
                        )
                    }
                    size="small"
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            {workQuery.isPending && (
                <Box
                    aria-label="Loading My Work"
                    role="status"
                    sx={{ display: 'grid', gap: 2, marginTop: 3 }}
                >
                    <Skeleton height={96} variant="rounded" />
                    <Skeleton height={120} variant="rounded" />
                    <Skeleton height={120} variant="rounded" />
                </Box>
            )}

            {workQuery.isError && (
                <Alert
                    action={
                        <Button
                            color="inherit"
                            onClick={() => void workQuery.refetch()}
                            size="small"
                        >
                            Retry
                        </Button>
                    }
                    severity="error"
                    sx={{ marginTop: 3 }}
                >
                    My Work could not be loaded.
                </Alert>
            )}

            {workQuery.data && (
                <>
                    <Box
                        sx={{
                            display: 'grid',
                            gap: 1.5,
                            gridTemplateColumns: {
                                xs: 'repeat(2, minmax(0, 1fr))',
                                md: 'repeat(5, minmax(0, 1fr))',
                            },
                            marginTop: 3,
                        }}
                    >
                        <SummaryCard label="Open" value={workQuery.data.summary.totalOpen} />
                        <SummaryCard
                            emphasis
                            label="Overdue"
                            value={workQuery.data.summary.overdue}
                        />
                        <SummaryCard label="Due soon" value={workQuery.data.summary.dueSoon} />
                        <SummaryCard label="Blocked" value={workQuery.data.summary.blocked} />
                        <SummaryCard
                            label="In progress"
                            value={workQuery.data.summary.inProgress}
                        />
                    </Box>

                    {workQuery.data.items.length === 0 ? (
                        <Paper
                            variant="outlined"
                            sx={{ marginTop: 3, padding: 4, textAlign: 'center' }}
                        >
                            <Typography variant="h6">Nothing needs your attention</Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 0.5 }}
                                variant="body2"
                            >
                                You do not currently have any readable open tasks assigned to you.
                            </Typography>
                        </Paper>
                    ) : (
                        <Stack spacing={3} sx={{ marginTop: 3 }}>
                            {attentionOrder.map((attention) => {
                                const items = workQuery.data.items.filter(
                                    (item) => item.attention === attention,
                                )
                                if (items.length === 0) return null

                                return (
                                    <Box key={attention}>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: 'baseline', marginBottom: 1 }}
                                        >
                                            <Typography component="h2" variant="h6">
                                                {attentionLabels[attention]}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {items.length}
                                            </Typography>
                                        </Stack>
                                        <Stack spacing={1}>
                                            {items.map((item) => (
                                                <WorkItemRow item={item} key={item.taskId} />
                                            ))}
                                        </Stack>
                                    </Box>
                                )
                            })}
                        </Stack>
                    )}

                    <Typography color="text.secondary" sx={{ marginTop: 3 }} variant="caption">
                        Due soon means within the next {workQuery.data.dueSoonHours} hours.
                        Completed and cancelled tasks are excluded.
                    </Typography>
                </>
            )}
        </Box>
    )
}
