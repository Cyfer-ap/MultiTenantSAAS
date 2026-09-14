import { Box, Stack, Typography } from '@mui/material'

import { DashboardMyWorkCard } from './DashboardMyWorkCard'
import { DashboardPersonalContextCard } from './DashboardPersonalContextCard'
import {
    DashboardQuickActionsCard,
    type DashboardQuickAction,
} from './DashboardQuickActionsCard'

interface DashboardWorkspaceOverviewProps {
    tenantId: string
    quickActions: readonly DashboardQuickAction[]
}

export function DashboardWorkspaceOverview({
    tenantId,
    quickActions,
}: DashboardWorkspaceOverviewProps) {
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
                <DashboardMyWorkCard tenantId={tenantId} />

                <Stack spacing={2}>
                    <DashboardQuickActionsCard actions={quickActions} />
                    <DashboardPersonalContextCard tenantId={tenantId} />
                </Stack>
            </Box>
        </Box>
    )
}
