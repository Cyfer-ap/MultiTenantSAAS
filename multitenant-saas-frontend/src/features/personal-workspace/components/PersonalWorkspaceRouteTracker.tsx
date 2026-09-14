import StarBorderRoundedIcon from '@mui/icons-material/StarBorderRounded'
import StarRoundedIcon from '@mui/icons-material/StarRounded'
import { CircularProgress, Fab, Tooltip } from '@mui/material'
import { useEffect, useMemo } from 'react'
import { useLocation } from 'react-router'

import {
    useFavoritePersonalResource,
    usePersonalWorkspace,
    useRecordRecentPersonalResource,
    useUnfavoritePersonalResource,
} from '../hooks/usePersonalWorkspace'
import type { PersonalResourceType } from '../types/personalWorkspace'

interface CurrentPersonalResource {
    type: PersonalResourceType
    resourceId: string
}

function resolveCurrentResource(pathname: string, search: string): CurrentPersonalResource | null {
    const projectMatch = /^\/projects\/([^/]+)$/.exec(pathname)
    if (!projectMatch) {
        return null
    }

    const taskId = new URLSearchParams(search).get('task')?.trim()
    if (taskId) {
        return { type: 'TASK', resourceId: taskId }
    }

    return { type: 'PROJECT', resourceId: projectMatch[1] }
}

export function PersonalWorkspaceRouteTracker({ tenantId }: { tenantId: string }) {
    const location = useLocation()
    const currentResource = useMemo(
        () => resolveCurrentResource(location.pathname, location.search),
        [location.pathname, location.search],
    )
    const workspaceQuery = usePersonalWorkspace(tenantId, 50)
    const favoriteMutation = useFavoritePersonalResource(tenantId)
    const unfavoriteMutation = useUnfavoritePersonalResource(tenantId)
    const { mutate: recordRecent } = useRecordRecentPersonalResource(tenantId)

    useEffect(() => {
        if (!tenantId || !currentResource) {
            return
        }

        recordRecent(currentResource)
    }, [currentResource, recordRecent, tenantId])

    if (!currentResource) {
        return null
    }

    const isFavorite = Boolean(
        workspaceQuery.data?.favorites.some(
            (item) =>
                item.type === currentResource.type &&
                item.resourceId === currentResource.resourceId,
        ),
    )
    const pending = favoriteMutation.isPending || unfavoriteMutation.isPending
    const resourceLabel = currentResource.type === 'TASK' ? 'task' : 'project'
    const actionLabel = isFavorite
        ? `Remove ${resourceLabel} from favorites`
        : `Add ${resourceLabel} to favorites`

    const toggleFavorite = () => {
        if (pending) {
            return
        }

        if (isFavorite) {
            unfavoriteMutation.mutate(currentResource)
            return
        }

        favoriteMutation.mutate(currentResource)
    }

    return (
        <Tooltip title={actionLabel}>
            <Fab
                aria-label={actionLabel}
                color={isFavorite ? 'warning' : 'primary'}
                disabled={workspaceQuery.isPending || pending}
                onClick={toggleFavorite}
                size="medium"
                variant="extended"
                sx={(theme) => ({
                    bottom: 24,
                    position: 'fixed',
                    right: {
                        xs: 16,
                        sm: currentResource.type === 'TASK' ? 604 : 24,
                    },
                    zIndex: theme.zIndex.drawer + 1,
                })}
            >
                {pending ? (
                    <CircularProgress color="inherit" size={18} sx={{ mr: 1 }} />
                ) : isFavorite ? (
                    <StarRoundedIcon sx={{ mr: 1 }} />
                ) : (
                    <StarBorderRoundedIcon sx={{ mr: 1 }} />
                )}
                {isFavorite ? 'Favorited' : 'Favorite'}
            </Fab>
        </Tooltip>
    )
}
