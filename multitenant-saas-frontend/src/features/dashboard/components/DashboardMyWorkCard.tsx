import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded'
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
import { Link } from 'react-router'

import { useMyWork } from '../../my-work/hooks/useMyWork'
import type { MyWorkAttention, MyWorkItem } from '../../my-work/types/myWork'

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
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                    >
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

function MyWorkSkeleton() {
    return (
        <Stack spacing={1}>
            <Skeleton height={32} width="45%" />
            <Skeleton height={56} variant="rounded" />
            <Skeleton height={56} variant="rounded" />
        </Stack>
    )
}

export function DashboardMyWorkCard({ tenantId }: { tenantId: string }) {
    const myWorkQuery = useMyWork(tenantId, 8)
    const work = myWorkQuery.data

    return (
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
                    <MyWorkSkeleton />
                ) : myWorkQuery.isError ? (
                    <Alert severity="warning">
                        Your personal attention queue could not be loaded. The rest of the dashboard
                        remains available.
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
    )
}
