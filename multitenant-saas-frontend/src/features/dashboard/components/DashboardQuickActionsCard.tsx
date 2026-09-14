import { Button, Card, CardContent, Stack, Typography } from '@mui/material'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

export interface DashboardQuickAction {
    label: string
    path: string
    icon: ReactNode
}

const dashboardQuickActionPriority: Readonly<Record<string, number>> = {
    '/my-work': 0,
    '/projects': 1,
    '/calendar': 2,
    '/task-planning': 3,
    '/work-automation': 4,
    '/users': 5,
    '/personal': 6,
}

export function DashboardQuickActionsCard({
    actions,
}: {
    actions: readonly DashboardQuickAction[]
}) {
    const visibleActions = [...actions]
        .sort(
            (left, right) =>
                (dashboardQuickActionPriority[left.path] ?? Number.MAX_SAFE_INTEGER) -
                (dashboardQuickActionPriority[right.path] ?? Number.MAX_SAFE_INTEGER),
        )
        .slice(0, 6)

    return (
        <Card variant="outlined">
            <CardContent>
                <Typography component="h3" variant="h6" sx={{ fontWeight: 750, mb: 1 }}>
                    Quick actions
                </Typography>
                {visibleActions.length > 0 ? (
                    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap', gap: 1 }}>
                        {visibleActions.map((action) => (
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
