import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import OpenInNewRoundedIcon from '@mui/icons-material/OpenInNewRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
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
import type { ChipProps } from '@mui/material'
import { Link, useParams } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { useProjectRisk } from '../hooks/useProjectRisk'
import type {
    ProjectRiskSeverity,
    ProjectRiskSignal,
    ProjectRiskSignalType,
} from '../types/projectRisk'

const signalLabels: Record<ProjectRiskSignalType, string> = {
    OVERDUE_TASK: 'Overdue task',
    BLOCKED_TASK: 'Blocked task',
    STALE_TASK: 'Stale task',
    UNASSIGNED_CRITICAL_TASK: 'Unassigned critical work',
    DEPENDENCY_BOTTLENECK: 'Dependency bottleneck',
}

function severityColor(severity: ProjectRiskSeverity): ChipProps['color'] {
    switch (severity) {
        case 'CRITICAL':
        case 'HIGH':
            return 'error'
        case 'MEDIUM':
            return 'warning'
        case 'LOW':
            return 'info'
        default:
            return 'success'
    }
}

function formatDate(value: string | null): string | null {
    if (!value) return null
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function SignalCard({ projectId, signal }: { projectId: string; signal: ProjectRiskSignal }) {
    const dueAt = formatDate(signal.dueAt)
    const updatedAt = formatDate(signal.updatedAt)

    return (
        <Paper variant="outlined" sx={{ padding: 2 }}>
            <Stack spacing={1.5}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} alignItems={{ sm: 'center' }}>
                    <Chip
                        color={severityColor(signal.severity)}
                        label={signal.severity}
                        size="small"
                    />
                    <Chip label={signalLabels[signal.type]} size="small" variant="outlined" />
                    <Typography sx={{ fontWeight: 700 }}>{signal.taskTitle}</Typography>
                </Stack>

                <Typography>{signal.explanation}</Typography>

                <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
                    {dueAt ? (
                        <Typography color="text.secondary" variant="body2">
                            Due: {dueAt}
                        </Typography>
                    ) : null}
                    {updatedAt ? (
                        <Typography color="text.secondary" variant="body2">
                            Last task change: {updatedAt}
                        </Typography>
                    ) : null}
                    {signal.affectedTaskCount > 1 ? (
                        <Typography color="text.secondary" variant="body2">
                            Affected tasks: {signal.affectedTaskCount}
                        </Typography>
                    ) : null}
                </Stack>

                <Box>
                    <Button
                        component={Link}
                        endIcon={<OpenInNewRoundedIcon />}
                        size="small"
                        to={`/projects/${projectId}?task=${signal.taskId}`}
                    >
                        Open task
                    </Button>
                </Box>
            </Stack>
        </Paper>
    )
}

export function ProjectRiskPage() {
    const { session } = useAuth()
    const { projectId = '' } = useParams()
    const tenantId = session?.tenantId ?? ''
    const riskQuery = useProjectRisk(tenantId, projectId)

    if (riskQuery.isPending) {
        return (
            <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
                <CircularProgress aria-label="Loading project risk" />
            </Box>
        )
    }

    if (riskQuery.isError || !riskQuery.data) {
        return (
            <Stack spacing={2}>
                <Button component={Link} startIcon={<ArrowBackRoundedIcon />} to={`/projects/${projectId}`}>
                    Back to project
                </Button>
                <Alert
                    action={
                        <Button color="inherit" onClick={() => void riskQuery.refetch()} size="small">
                            Retry
                        </Button>
                    }
                    severity="error"
                >
                    Project risk could not be loaded.
                </Alert>
            </Stack>
        )
    }

    const { summary } = riskQuery.data
    const summaryItems = [
        ['Overdue', summary.overdueTasks],
        ['Blocked', summary.blockedTasks],
        ['Stale', summary.staleTasks],
        ['Unassigned high/urgent', summary.unassignedCriticalTasks],
        ['Dependency bottlenecks', summary.dependencyBottlenecks],
    ] as const

    return (
        <Stack spacing={3}>
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                justifyContent="space-between"
                alignItems={{ md: 'center' }}
            >
                <Stack spacing={0.5}>
                    <Button
                        component={Link}
                        startIcon={<ArrowBackRoundedIcon />}
                        sx={{ alignSelf: 'flex-start' }}
                        to={`/projects/${projectId}`}
                    >
                        Back to project
                    </Button>
                    <Stack direction="row" spacing={1} alignItems="center">
                        <WarningAmberRoundedIcon />
                        <Typography variant="h4">Project Health / Risk Radar</Typography>
                    </Stack>
                    <Typography color="text.secondary">
                        Explainable, advisory signals from the project&apos;s current task and dependency state.
                    </Typography>
                </Stack>
                <Stack direction="row" spacing={1} alignItems="center">
                    <Chip
                        color={severityColor(riskQuery.data.riskLevel)}
                        label={`Overall: ${riskQuery.data.riskLevel}`}
                    />
                    <Button
                        onClick={() => void riskQuery.refetch()}
                        startIcon={<RefreshRoundedIcon />}
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Alert severity="info">
                Risk Radar does not rank people or infer workload capacity. It only reports the documented
                project signals below, and it never changes project state.
            </Alert>

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
                }}
            >
                {summaryItems.map(([label, value]) => (
                    <Paper key={label} variant="outlined" sx={{ padding: 2 }}>
                        <Typography color="text.secondary" variant="body2">
                            {label}
                        </Typography>
                        <Typography variant="h4">{value}</Typography>
                    </Paper>
                ))}
            </Box>

            <Typography color="text.secondary" variant="body2">
                {summary.openTasks} open of {summary.totalTasks} evaluated tasks · stale threshold{' '}
                {riskQuery.data.staleAfterDays} days
            </Typography>

            <Divider />

            <Stack spacing={2}>
                <Typography variant="h5">Why this project is flagged</Typography>
                {riskQuery.data.signals.length === 0 ? (
                    <Alert severity="success">No current Risk Radar signals were found.</Alert>
                ) : (
                    riskQuery.data.signals.map((signal) => (
                        <SignalCard
                            key={`${signal.type}-${signal.taskId}`}
                            projectId={projectId}
                            signal={signal}
                        />
                    ))
                )}
            </Stack>

            {riskQuery.data.limitations.length > 0 ? (
                <Paper variant="outlined" sx={{ padding: 2 }}>
                    <Typography sx={{ fontWeight: 700 }}>Interpretation limits</Typography>
                    <Stack component="ul" spacing={0.5} sx={{ marginBottom: 0, paddingLeft: 3 }}>
                        {riskQuery.data.limitations.map((limitation) => (
                            <Typography component="li" key={limitation} color="text.secondary" variant="body2">
                                {limitation}
                            </Typography>
                        ))}
                    </Stack>
                </Paper>
            ) : null}
        </Stack>
    )
}
