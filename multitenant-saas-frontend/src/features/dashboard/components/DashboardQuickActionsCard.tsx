import { Button, Card, CardContent, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

export interface DashboardQuickAction {
    label: string
    path: string
    icon: ReactNode
}

export function DashboardQuickActionsCard({
    actions,
}: {
    actions: readonly DashboardQuickAction[]
}) {
    return (
        <Card variant="outlined">
            <CardContent>
                <Typography component="h3" variant="h6" sx={{ fontWeight: 750, mb: 1 }}>
                    Quick actions
                </Typography>
                {actions.length > 0 ? (
                    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                        {actions.slice(0, 6).map((action) => (
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
                ) : (
                    <Typography color="text.secondary" variant="body2">
                        Workspace actions are loading.
                    </Typography>
                )}
            </CardContent>
        </Card>
    )
}
