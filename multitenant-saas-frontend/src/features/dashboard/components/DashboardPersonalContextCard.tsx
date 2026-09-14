import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import StarRoundedIcon from '@mui/icons-material/StarRounded'
import { Alert, Box, Button, Card, CardContent, Skeleton, Stack, Typography } from '@mui/material'
import { Link } from 'react-router'

import { usePersonalWorkspace } from '../../personal-workspace/hooks/usePersonalWorkspace'
import { getPersonalWorkspaceTarget } from '../../personal-workspace/navigation/personalWorkspaceTarget'
import type { PersonalWorkspaceItem } from '../../personal-workspace/types/personalWorkspace'

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

function PersonalContextSkeleton() {
    return (
        <Stack spacing={1}>
            <Skeleton height={32} width="45%" />
            <Skeleton height={56} variant="rounded" />
            <Skeleton height={56} variant="rounded" />
        </Stack>
    )
}

export function DashboardPersonalContextCard({ tenantId }: { tenantId: string }) {
    const personalWorkspaceQuery = usePersonalWorkspace(tenantId, 6)
    const favorites = personalWorkspaceQuery.data?.favorites.slice(0, 3) ?? []
    const recent = personalWorkspaceQuery.data?.recent.slice(0, 3) ?? []

    return (
        <Card variant="outlined">
            <CardContent>
                {personalWorkspaceQuery.isPending ? (
                    <PersonalContextSkeleton />
                ) : personalWorkspaceQuery.isError ? (
                    <Alert severity="warning">
                        Favorites and recent items could not be loaded.
                    </Alert>
                ) : (
                    <Stack spacing={2}>
                        <Box>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
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
                                    <Typography color="text.secondary" variant="body2">
                                        Pin projects or tasks to keep them close.
                                    </Typography>
                                )}
                            </Stack>
                        </Box>

                        <Box>
                            <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1 }}>
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
                                    <Typography color="text.secondary" variant="body2">
                                        Recently opened projects and tasks will appear here.
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
    )
}
