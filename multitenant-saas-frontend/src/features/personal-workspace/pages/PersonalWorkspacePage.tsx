import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined'
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import StarBorderRoundedIcon from '@mui/icons-material/StarBorderRounded'
import StarRoundedIcon from '@mui/icons-material/StarRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    InputAdornment,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent, ReactNode } from 'react'
import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { useGlobalSearch } from '../../search/hooks/useGlobalSearch'
import type { GlobalSearchResult } from '../../search/types/search'
import {
    useFavoritePersonalResource,
    usePersonalWorkspace,
    useUnfavoritePersonalResource,
} from '../hooks/usePersonalWorkspace'
import type { PersonalResourceType, PersonalWorkspaceItem } from '../types/personalWorkspace'

function resourceIcon(type: PersonalResourceType): ReactNode {
    return type === 'PROJECT' ? <FolderOutlinedIcon /> : <AssignmentOutlinedIcon />
}

function targetFor(item: Pick<PersonalWorkspaceItem, 'type' | 'resourceId' | 'parentId'>): string {
    if (item.type === 'PROJECT') {
        return `/projects/${item.resourceId}`
    }
    return `/projects/${item.parentId}?task=${item.resourceId}`
}

function formatTime(value: string | null): string {
    if (!value) {
        return '—'
    }
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
        return '—'
    }
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'The personal workspace could not be updated.'
}

function ResourceCard({
    item,
    action,
}: {
    item: PersonalWorkspaceItem
    action?: ReactNode
}) {
    const navigate = useNavigate()
    return (
        <Paper variant="outlined" sx={{ p: 2 }}>
            <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                <Box sx={{ color: 'text.secondary', display: 'flex' }}>{resourceIcon(item.type)}</Box>
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Typography noWrap sx={{ fontWeight: 700 }}>
                            {item.title}
                        </Typography>
                        <Chip label={item.type === 'PROJECT' ? 'Project' : 'Task'} size="small" />
                    </Stack>
                    <Typography color="text.secondary" noWrap variant="body2">
                        {item.subtitle || 'Workspace item'}
                    </Typography>
                </Box>
                {action}
                <Button onClick={() => navigate(targetFor(item))} size="small">
                    Open
                </Button>
            </Stack>
        </Paper>
    )
}

export function PersonalWorkspacePage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')

    const workspaceQuery = usePersonalWorkspace(tenantId, 16)
    const searchQuery = useGlobalSearch(tenantId, search, {
        enabled: search.length >= 2,
        limit: 12,
    })
    const favoriteMutation = useFavoritePersonalResource(tenantId)
    const unfavoriteMutation = useUnfavoritePersonalResource(tenantId)

    const favoriteKeys = useMemo(
        () =>
            new Set(
                (workspaceQuery.data?.favorites ?? []).map(
                    (item) => `${item.type}:${item.resourceId}`,
                ),
            ),
        [workspaceQuery.data?.favorites],
    )

    const searchableResults = (searchQuery.data?.results ?? []).filter(
        (result): result is GlobalSearchResult & { type: PersonalResourceType } =>
            result.type === 'PROJECT' || result.type === 'TASK',
    )

    const submitSearch = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        setSearch(searchDraft.trim())
    }

    if (workspaceQuery.isPending) {
        return (
            <Stack sx={{ alignItems: 'center', minHeight: 320, justifyContent: 'center' }}>
                <CircularProgress />
            </Stack>
        )
    }

    if (workspaceQuery.isError) {
        return (
            <Alert severity="error">
                {getErrorMessage(workspaceQuery.error)}
            </Alert>
        )
    }

    const favorites = workspaceQuery.data?.favorites ?? []
    const recent = workspaceQuery.data?.recent ?? []
    const mutationError = favoriteMutation.error ?? unfavoriteMutation.error

    return (
        <Box>
            <Stack spacing={0.5} sx={{ mb: 3 }}>
                <Typography component="h1" variant="h4" sx={{ fontWeight: 800 }}>
                    Favorites & recent
                </Typography>
                <Typography color="text.secondary">
                    Pin the work you return to most, and jump back into recently opened projects and tasks.
                </Typography>
            </Stack>

            {mutationError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                    {getErrorMessage(mutationError)}
                </Alert>
            )}

            <Paper variant="outlined" sx={{ p: 2.5, mb: 3 }}>
                <Typography component="h2" variant="h6" sx={{ mb: 0.5, fontWeight: 750 }}>
                    Add a favorite
                </Typography>
                <Typography color="text.secondary" variant="body2" sx={{ mb: 2 }}>
                    Search only the projects and tasks you can currently access.
                </Typography>
                <Box component="form" onSubmit={submitSearch}>
                    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                        <TextField
                            fullWidth
                            value={searchDraft}
                            onChange={(event) => setSearchDraft(event.target.value)}
                            placeholder="Search projects or tasks"
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchRoundedIcon />
                                        </InputAdornment>
                                    ),
                                },
                            }}
                        />
                        <Button type="submit" variant="contained" disabled={searchDraft.trim().length < 2}>
                            Search
                        </Button>
                    </Stack>
                </Box>

                {search.length >= 2 && (
                    <Stack spacing={1} sx={{ mt: 2 }}>
                        {searchQuery.isFetching && <CircularProgress size={22} />}
                        {searchQuery.isError && (
                            <Alert severity="error">Search could not be completed.</Alert>
                        )}
                        {!searchQuery.isFetching &&
                            !searchQuery.isError &&
                            searchableResults.length === 0 && (
                                <Typography color="text.secondary" variant="body2">
                                    No accessible projects or tasks match this search.
                                </Typography>
                            )}
                        {searchableResults.map((result) => {
                            const key = `${result.type}:${result.id}`
                            const isFavorite = favoriteKeys.has(key)
                            return (
                                <Paper key={key} variant="outlined" sx={{ p: 1.5 }}>
                                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                                        <Box sx={{ color: 'text.secondary', display: 'flex' }}>
                                            {resourceIcon(result.type)}
                                        </Box>
                                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                            <Typography noWrap sx={{ fontWeight: 650 }}>
                                                {result.title}
                                            </Typography>
                                            <Typography color="text.secondary" noWrap variant="body2">
                                                {result.subtitle || (result.type === 'PROJECT' ? 'Project' : 'Task')}
                                            </Typography>
                                        </Box>
                                        <Button
                                            disabled={favoriteMutation.isPending || unfavoriteMutation.isPending}
                                            onClick={() => {
                                                if (isFavorite) {
                                                    unfavoriteMutation.mutate({
                                                        type: result.type,
                                                        resourceId: result.id,
                                                    })
                                                } else {
                                                    favoriteMutation.mutate({
                                                        type: result.type,
                                                        resourceId: result.id,
                                                    })
                                                }
                                            }}
                                            startIcon={isFavorite ? <StarRoundedIcon /> : <StarBorderRoundedIcon />}
                                            size="small"
                                        >
                                            {isFavorite ? 'Favorited' : 'Favorite'}
                                        </Button>
                                    </Stack>
                                </Paper>
                            )
                        })}
                    </Stack>
                )}
            </Paper>

            <Stack direction={{ xs: 'column', lg: 'row' }} spacing={3}>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1.5 }}>
                        <StarRoundedIcon />
                        <Typography component="h2" variant="h6" sx={{ fontWeight: 750 }}>
                            Favorites
                        </Typography>
                    </Stack>
                    <Stack spacing={1}>
                        {favorites.length === 0 ? (
                            <Paper variant="outlined" sx={{ p: 3 }}>
                                <Typography color="text.secondary">
                                    No favorites yet. Search above to pin a project or task.
                                </Typography>
                            </Paper>
                        ) : (
                            favorites.map((item) => (
                                <ResourceCard
                                    key={`${item.type}:${item.resourceId}`}
                                    item={item}
                                    action={
                                        <Button
                                            aria-label={`Remove ${item.title} from favorites`}
                                            disabled={unfavoriteMutation.isPending}
                                            onClick={() =>
                                                unfavoriteMutation.mutate({
                                                    type: item.type,
                                                    resourceId: item.resourceId,
                                                })
                                            }
                                            size="small"
                                        >
                                            Unpin
                                        </Button>
                                    }
                                />
                            ))
                        )}
                    </Stack>
                </Box>

                <Box sx={{ flex: 1, minWidth: 0 }}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center', mb: 1.5 }}>
                        <HistoryRoundedIcon />
                        <Typography component="h2" variant="h6" sx={{ fontWeight: 750 }}>
                            Recently viewed
                        </Typography>
                    </Stack>
                    <Stack spacing={1}>
                        {recent.length === 0 ? (
                            <Paper variant="outlined" sx={{ p: 3 }}>
                                <Typography color="text.secondary">
                                    Open a project or task and it will appear here.
                                </Typography>
                            </Paper>
                        ) : (
                            recent.map((item) => (
                                <Box key={`${item.type}:${item.resourceId}`}>
                                    <ResourceCard item={item} />
                                    <Typography color="text.secondary" variant="caption" sx={{ display: 'block', mt: 0.25, ml: 1 }}>
                                        Viewed {formatTime(item.lastViewedAt)}
                                    </Typography>
                                </Box>
                            ))
                        )}
                    </Stack>
                </Box>
            </Stack>
        </Box>
    )
}
