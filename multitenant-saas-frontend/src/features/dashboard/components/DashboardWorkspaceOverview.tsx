import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import StarRoundedIcon from '@mui/icons-material/StarRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    Paper,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { useMyWork } from '../../my-work/hooks/useMyWork'
import type { MyWorkAttention, MyWorkItem } from '../../my-work/types/myWork'
import { usePersonalWorkspace } from '../../personal-workspace/hooks/usePersonalWorkspace'
import { getPersonalWorkspaceTarget } from '../../personal-workspace/navigation/personalWorkspaceTarget'
import type { PersonalWorkspaceItem } from '../../personal-workspace/types/personalWorkspace'

export interface DashboardQuickAction {
    label: string
    path: string
    icon: ReactNode
}

interface DashboardWorkspaceOverviewProps {
    tenantId: string
    quickActions: readonly DashboardQuickAction[]
}

const attentionLabels: Record<MyWorkAttention, string> = {
    OVERDUE: 'Overdue',
    BLOCKED: 'Blocked',
    DUE_SOON: 'Due soon',
    IN_PROGRESS: 'In progress',
    ASSIGNED: 'Assigned',
}

function formatDueDate(value: string | null): string {
    if (!value) return 'No due date'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return 'Due date unavailable'
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

function WorkItemRow({ item }: { item: MyWorkItem }) {
    return (
        <Paper variant="outlined" sx={{ padding: 1.5 }}>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
            >
                <Box sx={{ minWidth: 0 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', flexWrap: 'wrap' }}>
                        <Typography noWrap sx={{ fontWeight: 700 }}>
                            {item.title}
                        </Typography>
                        <Chip label={attentionLabels[item.attention]} size="small" />
                    </Stack>
                    <Typography color="text.secondary" noWrap variant="body2">
                        {item.projectName} · {formatDueDate(item.dueAt)}
                    </Typography>
                </Box>
                <Button component={Link} size="small" to={item.targetUrl}>
                    Open
                </Button>
            </Stack>
        </Paper>
    )
}

function PersonalItemRow({ item }: { item: PersonalWorkspaceItem }) {
    return (
        <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', justifyContent: 'space-between', minWidth: 0 }}
        >
            <Box sx={{ minWidth: 0 }}>
                <Typography noWrap variant="body2" sx={{ fontWeight: 700 }}>
                    {item.title}
                </Typography>
                <Typography color="text.secondary" noWrap variant="caption">
                    {item.subtitle || (item.type === 'PROJECT' ? 'Project' : 'Task')}
                </Typography>
            </Box>
            <Button component={Link} size="small" to={getPersonalWorkspaceTarget(item)}>
                Open
            </Button>
        </Stack>
    )
}

function WidgetSkeleton() {
    return (
        <Stack spacing={1}>
            <Skeleton height={32} width="45%" />
            <Skeleton height={56} variant="rounded" />
            <Skeleton height={56} variant="rounded" />
        </Stack>
    )
}

export function DashboardWorkspaceOverview({
    tenantId,
    quickActions,
}: DashboardWorkspaceOverviewProps) {
    const myWorkQuery = useMyWork(tenantId, 8)
    const personalWorkspaceQuery = usePersonalWorkspace(tenantId, 6)

    const work = myWorkQuery.data
    const favorites = personalWorkspaceQuery.data?.favorites.slice(0, 3) ?? []
    const recent = personalWorkspaceQuery.data?.recent.slice(0, 3) ?? []

    return (
        <Box sx={{ marginTop: 3 }}>
            <Stack spacing={0.5} sx={{ marginBottom: 2 }}>
                <Typography component="h2" variant="h5" sx={{ fontWeight: 800 }}>
                    Your workspace
                </Typography>
                <Typography color="text.secondary" variant="body2">
                    Personal attention, shortcuts, favorites and recent context from the access you
                    have right now.
                </Typography>
            </Stack>

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: { xs: '1fr', xl: 'minmax(0, 2fr) minmax(320px, 1fr)' },
                }}
            >
                <Card variant="outlined">
                    <CardContent>
                        <Stack
                            direction="row"
                            spacing={1}
                            sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}
                        >
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                <AssignmentRoundedIcon />
                                <Typography component="h3" variant="h6" sx={{ fontWeight: 750 }}>
                                    My Work
                                </Typography>
                            </Stack>
                            <Button component={Link} size="small" to="/my-work">
                                View all
                            </Button>
                        </Stack>

                        {myWorkQuery.isPending ? (
                            <WidgetSkeleton />
                        ) : myWorkQuery.isError ? (
                            <Alert severity="warning">
                                Your personal attention queue could not be loaded. The rest of the
                                dashboard remains available.
                            </Alert>
                        ) : (
                            <>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gap: 1,
                                        gridTemplateColumns: {
                                            xs: 'repeat(2, minmax(0, 1fr))',
                                            sm: 'repeat(5, minmax(0, 1fr))',
                                        },
                                        mb: 2,
                                    }}
                                >
                                    {[
                                        ['Open', work?.summary.totalOpen ?? 0],
                                        ['Overdue', work?.summary.overdue ?? 0],
                                        ['Due soon', work?.summary.dueSoon ?? 0],
                                        ['Blocked', work?.summary.blocked ?? 0],
                                        ['In progress', work?.summary.inProgress ?? 0],
                                    ].map(([label, value]) => (
                                        <Paper key={label} variant="outlined" sx={{ p: 1.25 }}>
                                            <Typography color="text.secondary" variant="caption">
                                                {label}
                                            </Typography>
                                            <Typography variant="h6">{value}</Typography>
                                        </Paper>
                                    ))}
                                </Box>

                                <Stack spacing={1}>
                                    {(work?.items ?? []).slice(0, 5).map((item) => (
                                        <WorkItemRow key={item.taskId} item={item} />
                                    ))}
                                    {(work?.items.length ?? 0) === 0 && (
                                        <Typography color="text.secondary" variant="body2">
                                            No assigned open tasks need your attention right now.
                                        </Typography>
                                    )}
                                </Stack>
                            </>
                        )}
                    </CardContent>
                </Card>

                <Stack spacing={2}>
                    <Card variant="outlined">
                        <CardContent>
                            <Typography component="h3" variant="h6" sx={{ fontWeight: 750, mb: 1 }}>
                                Quick actions
                            </Typography>
                            <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                                {quickActions.slice(0, 6).map((action) => (
                                    <Button
                                        component={Link}
                                        key={action.path}
                                        startIcon={action.icon}
                                        to={action.path}
                                        variant="outlined"
                                    >
                                        {action.label}
                                    </Button>
                                ))}
                            </Stack>
                        </CardContent>
                    </Card>

                    <Card variant="outlined">
                        <CardContent>
                            {personalWorkspaceQuery.isPending ? (
                                <WidgetSkeleton />
                            ) : personalWorkspaceQuery.isError ? (
                                <Alert severity="warning">
                                    Favorites and recent items could not be loaded.
                                </Alert>
                            ) : (
                                <Stack spacing={2}>
                                    <Box>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: 'center', mb: 1 }}
                                        >
                                            <StarRoundedIcon fontSize="small" />
                                            <Typography
                                                component="h3"
                                                variant="subtitle1"
                                                sx={{ fontWeight: 750 }}
                                            >
                                                Favorites
                                            </Typography>
                                        </Stack>
                                        <Stack spacing={1}>
                                            {favorites.map((item) => (
                                                <PersonalItemRow
                                                    key={`${item.type}:${item.resourceId}`}
                                                    item={item}
                                                />
                                            ))}
                                            {favorites.length === 0 && (
                                                <Typography
                                                    color="text.secondary"
                                                    variant="body2"
                                                >
                                                    Pin projects or tasks to keep them close.
                                                </Typography>
                                            )}
                                        </Stack>
                                    </Box>

                                    <Box>
                                        <Stack
                                            direction="row"
                                            spacing={1}
                                            sx={{ alignItems: 'center', mb: 1 }}
                                        >
                                            <HistoryRoundedIcon fontSize="small" />
                                            <Typography
                                                component="h3"
                                                variant="subtitle1"
                                                sx={{ fontWeight: 750 }}
                                            >
                                                Recently viewed
                                            </Typography>
                                        </Stack>
                                        <Stack spacing={1}>
                                            {recent.map((item) => (
                                                <PersonalItemRow
                                                    key={`${item.type}:${item.resourceId}`}
                                                    item={item}
                                                />
                                            ))}
                                            {recent.length === 0 && (
                                                <Typography
                                                    color="text.secondary"
                                                    variant="body2"
                                                >
                                                    Recently opened projects and tasks will appear
                                                    here.
                                                </Typography>
                                            )}
                                        </Stack>
                                    </Box>

                                    <Button component={Link} size="small" to="/personal">
                                        Manage favorites and recent
                                    </Button>
                                </Stack>
                            )}
                        </CardContent>
                    </Card>
                </Stack>
            </Box>
        </Box>
    )
}
