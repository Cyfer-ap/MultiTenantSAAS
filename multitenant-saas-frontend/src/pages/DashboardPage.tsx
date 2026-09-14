import FolderRoundedIcon from '@mui/icons-material/FolderRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import TaskAltRoundedIcon from '@mui/icons-material/TaskAltRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    LinearProgress,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material'
import type { ReactNode } from 'react'

import { useCurrentAuthorization } from '../features/authorization/hooks/useCurrentAuthorization'
import {
    DashboardWorkspaceOverview,
    type DashboardQuickAction,
} from '../features/dashboard/components/DashboardWorkspaceOverview'
import { useDashboardSummary } from '../features/dashboard/hooks/useDashboardSummary'
import type { TenantDashboardSummary } from '../features/dashboard/types/dashboard'
import { SubscriptionEndingSoonAlert } from '../features/subscriptions/components/SubscriptionEndingSoonAlert'
import { getAvailableWorkspaceNavigationItems } from '../layouts/workspaceNavigation'

interface MetricCardProps {
    label: string
    value: number
    detail: string
    icon: ReactNode
    tone: 'primary' | 'success' | 'warning' | 'error'
}

interface BreakdownItemProps {
    label: string
    value: number
}

function MetricCard({ label, value, detail, icon, tone }: MetricCardProps) {
    return (
        <Card variant="outlined">
            <CardContent>
                <Stack
                    direction="row"
                    spacing={2}
                    sx={{
                        alignItems: 'flex-start',
                        justifyContent: 'space-between',
                    }}
                >
                    <Box>
                        <Typography color="text.secondary" variant="body2">
                            {label}
                        </Typography>

                        <Typography component="p" variant="h4" sx={{ marginTop: 0.5 }}>
                            {value.toLocaleString()}
                        </Typography>
                    </Box>

                    <Box
                        sx={{
                            alignItems: 'center',
                            backgroundColor: `${tone}.main`,
                            borderRadius: 2,
                            color: `${tone}.contrastText`,
                            display: 'flex',
                            height: 44,
                            justifyContent: 'center',
                            width: 44,
                        }}
                    >
                        {icon}
                    </Box>
                </Stack>

                <Typography
                    color="text.secondary"
                    variant="caption"
                    sx={{ display: 'block', marginTop: 1.5 }}
                >
                    {detail}
                </Typography>
            </CardContent>
        </Card>
    )
}

function BreakdownItem({ label, value }: BreakdownItemProps) {
    return (
        <Stack
            direction="row"
            spacing={2}
            sx={{
                alignItems: 'center',
                justifyContent: 'space-between',
                paddingY: 0.75,
            }}
        >
            <Typography color="text.secondary" variant="body2">
                {label}
            </Typography>

            <Typography variant="body2" sx={{ fontWeight: 700 }}>
                {value.toLocaleString()}
            </Typography>
        </Stack>
    )
}

function DashboardSkeleton() {
    return (
        <Box aria-label="Loading dashboard" role="status">
            <Skeleton width="45%" height={44} />
            <Skeleton width="65%" />

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: {
                        xs: '1fr',
                        sm: 'repeat(2, minmax(0, 1fr))',
                        xl: 'repeat(4, minmax(0, 1fr))',
                    },
                    marginTop: 3,
                }}
            >
                {[0, 1, 2, 3].map((item) => (
                    <Skeleton key={item} height={148} variant="rounded" />
                ))}
            </Box>
        </Box>
    )
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'The dashboard could not be loaded.'
}

function DashboardContent({
    summary,
    quickActions,
}: {
    summary: TenantDashboardSummary
    quickActions: readonly DashboardQuickAction[]
}) {
    const completionPercentage = Math.min(100, Math.max(0, summary.taskCompletionPercentage))

    return (
        <>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1}
                sx={{
                    alignItems: {
                        xs: 'flex-start',
                        sm: 'center',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        {summary.tenantName}
                    </Typography>

                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Your workspace overview and current priorities
                    </Typography>
                </Box>

                <Chip
                    color={summary.tenantStatus === 'ACTIVE' ? 'success' : 'default'}
                    label={summary.tenantStatus.replace('_', ' ')}
                    size="small"
                    variant="outlined"
                />
            </Stack>

            <DashboardWorkspaceOverview quickActions={quickActions} tenantId={summary.tenantId} />

            <Typography component="h2" variant="h5" sx={{ fontWeight: 800, marginTop: 4 }}>
                Workspace health
            </Typography>
            <Typography color="text.secondary" variant="body2" sx={{ marginTop: 0.5 }}>
                Tenant-wide operational totals available to your dashboard access.
            </Typography>

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: {
                        xs: '1fr',
                        sm: 'repeat(2, minmax(0, 1fr))',
                        xl: 'repeat(4, minmax(0, 1fr))',
                    },
                    marginTop: 2,
                }}
            >
                <MetricCard
                    label="Total users"
                    value={summary.totalUsers}
                    detail={`${summary.activeUsers.toLocaleString()} active users`}
                    icon={<GroupsRoundedIcon />}
                    tone="primary"
                />

                <MetricCard
                    label="Active projects"
                    value={summary.activeProjects}
                    detail={`${summary.totalProjects.toLocaleString()} projects in total`}
                    icon={<FolderRoundedIcon />}
                    tone="success"
                />

                <MetricCard
                    label="Open tasks"
                    value={summary.todoTasks + summary.inProgressTasks + summary.blockedTasks}
                    detail={`${summary.totalTasks.toLocaleString()} tasks in total`}
                    icon={<TaskAltRoundedIcon />}
                    tone="warning"
                />

                <MetricCard
                    label="Overdue tasks"
                    value={summary.overdueTasks}
                    detail="Excludes completed and cancelled tasks"
                    icon={<WarningAmberRoundedIcon />}
                    tone="error"
                />
            </Box>

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: {
                        xs: '1fr',
                        lg: 'repeat(3, minmax(0, 1fr))',
                    },
                    marginTop: 2,
                }}
            >
                <Card variant="outlined">
                    <CardContent>
                        <Typography component="h3" variant="h6">
                            Users
                        </Typography>

                        <Typography
                            color="text.secondary"
                            variant="body2"
                            sx={{ marginBottom: 1, marginTop: 0.5 }}
                        >
                            Current account status distribution
                        </Typography>

                        <BreakdownItem label="Active" value={summary.activeUsers} />
                        <BreakdownItem label="Inactive" value={summary.inactiveUsers} />
                        <BreakdownItem label="Suspended" value={summary.suspendedUsers} />
                    </CardContent>
                </Card>

                <Card variant="outlined">
                    <CardContent>
                        <Typography component="h3" variant="h6">
                            Projects
                        </Typography>

                        <Typography
                            color="text.secondary"
                            variant="body2"
                            sx={{ marginBottom: 1, marginTop: 0.5 }}
                        >
                            {summary.totalProjectMemberships.toLocaleString()} project memberships
                        </Typography>

                        <BreakdownItem label="Planning" value={summary.planningProjects} />
                        <BreakdownItem label="Active" value={summary.activeProjects} />
                        <BreakdownItem label="On hold" value={summary.onHoldProjects} />
                        <BreakdownItem label="Completed" value={summary.completedProjects} />
                        <BreakdownItem label="Archived" value={summary.archivedProjects} />
                    </CardContent>
                </Card>

                <Card variant="outlined">
                    <CardContent>
                        <Stack
                            direction="row"
                            spacing={2}
                            sx={{
                                alignItems: 'baseline',
                                justifyContent: 'space-between',
                            }}
                        >
                            <Typography component="h3" variant="h6">
                                Tasks
                            </Typography>

                            <Typography color="primary.main" variant="h6">
                                {completionPercentage.toFixed(1)}%
                            </Typography>
                        </Stack>

                        <Typography color="text.secondary" variant="body2" sx={{ marginTop: 0.5 }}>
                            Completion excludes cancelled tasks
                        </Typography>

                        <LinearProgress
                            aria-label="Task completion"
                            value={completionPercentage}
                            variant="determinate"
                            sx={{ height: 8, marginY: 2, borderRadius: 4 }}
                        />

                        <BreakdownItem label="To do" value={summary.todoTasks} />
                        <BreakdownItem label="In progress" value={summary.inProgressTasks} />
                        <BreakdownItem label="Blocked" value={summary.blockedTasks} />
                        <BreakdownItem label="Completed" value={summary.completedTasks} />
                        <BreakdownItem label="Cancelled" value={summary.cancelledTasks} />
                    </CardContent>
                </Card>
            </Box>
        </>
    )
}

export function DashboardPage() {
    const dashboardQuery = useDashboardSummary()
    const authorizationQuery = useCurrentAuthorization()

    if (dashboardQuery.isPending) {
        return <DashboardSkeleton />
    }

    if (dashboardQuery.isError) {
        return (
            <Box>
                <Typography component="h1" variant="h4">
                    Dashboard
                </Typography>

                <Alert
                    severity="error"
                    sx={{ marginTop: 3 }}
                    action={
                        <Button
                            color="inherit"
                            size="small"
                            onClick={() => {
                                void dashboardQuery.refetch()
                            }}
                        >
                            Retry
                        </Button>
                    }
                >
                    {getErrorMessage(dashboardQuery.error)}
                </Alert>
            </Box>
        )
    }

    const quickActions: DashboardQuickAction[] = authorizationQuery.data
        ? getAvailableWorkspaceNavigationItems(authorizationQuery.data)
              .filter((item) => item.path !== '/dashboard')
              .map((item) => ({ label: item.label, path: item.path, icon: item.icon }))
        : []

    return (
        <Box>
            <Stack
                direction="row"
                sx={{
                    justifyContent: 'flex-end',
                    marginBottom: 1,
                }}
            >
                <Button
                    startIcon={
                        dashboardQuery.isFetching ? (
                            <CircularProgress color="inherit" size={16} />
                        ) : (
                            <RefreshRoundedIcon />
                        )
                    }
                    disabled={dashboardQuery.isFetching}
                    onClick={() => {
                        void dashboardQuery.refetch()
                    }}
                    size="small"
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            <SubscriptionEndingSoonAlert />

            <DashboardContent quickActions={quickActions} summary={dashboardQuery.data} />
        </Box>
    )
}
