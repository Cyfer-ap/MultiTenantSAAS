import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import GroupsRoundedIcon from '@mui/icons-material/GroupsRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import VerifiedRoundedIcon from '@mui/icons-material/VerifiedRounded'
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    CircularProgress,
    LinearProgress,
    Skeleton,
    Stack,
    Typography,
} from '@mui/material'
import type { ReactNode } from 'react'

import { useSystemDashboard } from '../features/system-admin/hooks/useSystemDashboard'
import type { SystemDashboardSummary } from '../features/system-admin/types/systemAdmin'

interface MetricCardProps {
    detail: string
    icon: ReactNode
    label: string
    tone: 'primary' | 'success' | 'warning' | 'info'
    value: number
}

function MetricCard({ detail, icon, label, tone, value }: MetricCardProps) {
    return (
        <Card variant="outlined">
            <CardContent>
                <Stack direction="row" sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}>
                    <Box>
                        <Typography color="text.secondary" variant="body2">{label}</Typography>
                        <Typography component="p" sx={{ mt: 0.5 }} variant="h4">
                            {value.toLocaleString()}
                        </Typography>
                    </Box>
                    <Box sx={{ alignItems: 'center', bgcolor: tone + '.main', borderRadius: 2, color: tone + '.contrastText', display: 'flex', height: 44, justifyContent: 'center', width: 44 }}>
                        {icon}
                    </Box>
                </Stack>
                <Typography color="text.secondary" sx={{ display: 'block', mt: 1.5 }} variant="caption">
                    {detail}
                </Typography>
            </CardContent>
        </Card>
    )
}

function Breakdown({ active, inactive, suspended, total }: { active: number; inactive: number; suspended: number; total: number }) {
    const activePercent = total > 0 ? (active / total) * 100 : 0

    return (
        <Stack spacing={1.5}>
            <LinearProgress aria-label="Active percentage" value={activePercent} variant="determinate" />
            {[
                ['Active', active],
                ['Inactive', inactive],
                ['Suspended', suspended],
            ].map(([label, value]) => (
                <Stack direction="row" key={String(label)} sx={{ justifyContent: 'space-between' }}>
                    <Typography color="text.secondary" variant="body2">{label}</Typography>
                    <Typography sx={{ fontWeight: 700 }} variant="body2">{Number(value).toLocaleString()}</Typography>
                </Stack>
            ))}
        </Stack>
    )
}

function DashboardContent({ summary }: { summary: SystemDashboardSummary }) {
    return (
        <>
            <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(4, minmax(0, 1fr))' }, mt: 3 }}>
                <MetricCard detail={summary.activeTenants.toLocaleString() + ' active workspaces'} icon={<BusinessRoundedIcon />} label="Total tenants" tone="primary" value={summary.totalTenants} />
                <MetricCard detail={(summary.inactiveTenants + summary.suspendedTenants).toLocaleString() + ' require attention'} icon={<VerifiedRoundedIcon />} label="Active tenants" tone="success" value={summary.activeTenants} />
                <MetricCard detail={summary.activeUsers.toLocaleString() + ' active accounts'} icon={<GroupsRoundedIcon />} label="Total tenant users" tone="info" value={summary.totalUsers} />
                <MetricCard detail={(summary.inactiveUsers + summary.suspendedUsers).toLocaleString() + ' inactive or suspended'} icon={<WarningAmberRoundedIcon />} label="Active tenant users" tone="warning" value={summary.activeUsers} />
            </Box>

            <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' }, mt: 2 }}>
                <Card variant="outlined"><CardContent><Typography sx={{ mb: 2, fontWeight: 700 }}>Tenant health</Typography><Breakdown active={summary.activeTenants} inactive={summary.inactiveTenants} suspended={summary.suspendedTenants} total={summary.totalTenants} /></CardContent></Card>
                <Card variant="outlined"><CardContent><Typography sx={{ mb: 2, fontWeight: 700 }}>User account health</Typography><Breakdown active={summary.activeUsers} inactive={summary.inactiveUsers} suspended={summary.suspendedUsers} total={summary.totalUsers} /></CardContent></Card>
            </Box>
        </>
    )
}

export function SystemDashboardPage() {
    const query = useSystemDashboard()

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}>
                <Box>
                    <Typography component="h1" variant="h4">Global dashboard</Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>Platform-wide tenant and user health.</Typography>
                </Box>
                <Button disabled={query.isFetching} onClick={() => { void query.refetch() }} startIcon={query.isFetching ? <CircularProgress color="inherit" size={16} /> : <RefreshRoundedIcon />} variant="outlined">
                    Refresh
                </Button>
            </Stack>

            {query.isLoading && (
                <Box aria-label="Loading global dashboard" role="status" sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', xl: 'repeat(4, 1fr)' }, mt: 3 }}>
                    {[0, 1, 2, 3].map((item) => <Skeleton height={148} key={item} variant="rounded" />)}
                </Box>
            )}
            {query.isError && (
                <Alert action={<Button color="inherit" onClick={() => { void query.refetch() }}>Retry</Button>} severity="error" sx={{ mt: 3 }}>
                    {query.error instanceof Error ? query.error.message : 'The global dashboard could not be loaded.'}
                </Alert>
            )}
            {query.data && <DashboardContent summary={query.data} />}
        </Box>
    )
}
