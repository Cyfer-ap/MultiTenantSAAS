import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined'
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogContent,
    IconButton,
    InputAdornment,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Stack,
    TextField,
    Tooltip,
    Typography,
    useMediaQuery,
    useTheme,
} from '@mui/material'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { useGlobalSearch } from '../hooks/useGlobalSearch'
import type { GlobalSearchResult, GlobalSearchResultType } from '../types/search'

interface GlobalSearchProps {
    tenantId: string
}

const resultTypeLabels: Record<GlobalSearchResultType, string> = {
    PROJECT: 'Project',
    TASK: 'Task',
    USER: 'Person',
}

function resultIcon(type: GlobalSearchResultType) {
    if (type === 'PROJECT') {
        return <FolderOutlinedIcon fontSize="small" />
    }
    if (type === 'TASK') {
        return <AssignmentOutlinedIcon fontSize="small" />
    }
    return <PersonOutlineRoundedIcon fontSize="small" />
}

function resultTarget(result: GlobalSearchResult): string {
    if (result.type === 'PROJECT') {
        return `/projects/${result.id}`
    }
    if (result.type === 'TASK' && result.parentId) {
        return `/projects/${result.parentId}?task=${result.id}`
    }
    return `/users?search=${encodeURIComponent(result.subtitle ?? result.title)}`
}

function isEditableTarget(target: EventTarget | null): boolean {
    if (!(target instanceof HTMLElement)) {
        return false
    }

    return (
        target.isContentEditable ||
        target.tagName === 'INPUT' ||
        target.tagName === 'TEXTAREA' ||
        target.tagName === 'SELECT'
    )
}

export function GlobalSearch({ tenantId }: GlobalSearchProps) {
    const navigate = useNavigate()
    const theme = useTheme()
    const compact = useMediaQuery(theme.breakpoints.down('sm'))
    const [open, setOpen] = useState(false)
    const [query, setQuery] = useState('')
    const [debouncedQuery, setDebouncedQuery] = useState('')

    useEffect(() => {
        const timeout = window.setTimeout(() => setDebouncedQuery(query.trim()), 250)
        return () => window.clearTimeout(timeout)
    }, [query])

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === '/' && !event.metaKey && !event.ctrlKey && !event.altKey) {
                if (isEditableTarget(event.target)) {
                    return
                }
                event.preventDefault()
                setOpen(true)
            }
        }

        document.addEventListener('keydown', handleKeyDown)
        return () => document.removeEventListener('keydown', handleKeyDown)
    }, [])

    const normalizedQuery = debouncedQuery.trim()
    const searchQuery = useGlobalSearch(tenantId, normalizedQuery, {
        enabled: open && normalizedQuery.length >= 2,
        limit: 12,
    })

    const results = useMemo(() => searchQuery.data?.results ?? [], [searchQuery.data])

    const openResult = (result: GlobalSearchResult) => {
        navigate(resultTarget(result))
        setOpen(false)
        setQuery('')
        setDebouncedQuery('')
    }

    return (
        <>
            {compact ? (
                <Tooltip title="Search workspace (/)">
                    <IconButton aria-label="Search workspace" onClick={() => setOpen(true)}>
                        <SearchRoundedIcon />
                    </IconButton>
                </Tooltip>
            ) : (
                <Button
                    aria-label="Search workspace"
                    color="inherit"
                    onClick={() => setOpen(true)}
                    startIcon={<SearchRoundedIcon />}
                    variant="outlined"
                    sx={{
                        borderColor: 'divider',
                        color: 'text.secondary',
                        justifyContent: 'space-between',
                        minWidth: { sm: 220, lg: 320 },
                        px: 1.5,
                        textTransform: 'none',
                    }}
                >
                    <Box component="span" sx={{ flexGrow: 1, textAlign: 'left' }}>
                        Search workspace
                    </Box>
                    <Chip label="/" size="small" sx={{ ml: 1, height: 22 }} />
                </Button>
            )}

            <Dialog
                fullWidth
                maxWidth="sm"
                open={open}
                onClose={() => setOpen(false)}
                aria-labelledby="global-search-title"
            >
                <DialogContent sx={{ p: 0 }}>
                    <Box sx={{ p: 2, pb: 1.25 }}>
                        <Typography id="global-search-title" sx={{ fontWeight: 750, mb: 1.25 }}>
                            Search workspace
                        </Typography>
                        <TextField
                            autoFocus
                            fullWidth
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            placeholder="Search projects, tasks, and people"
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchRoundedIcon color="action" />
                                        </InputAdornment>
                                    ),
                                    endAdornment: searchQuery.isFetching ? (
                                        <InputAdornment position="end">
                                            <CircularProgress size={18} />
                                        </InputAdornment>
                                    ) : undefined,
                                },
                            }}
                        />
                    </Box>

                    <Box sx={{ borderTop: 1, borderColor: 'divider', minHeight: 220 }}>
                        {query.trim().length < 2 ? (
                            <Stack
                                sx={{
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    minHeight: 220,
                                    px: 3,
                                }}
                            >
                                <SearchRoundedIcon color="disabled" sx={{ fontSize: 38, mb: 1 }} />
                                <Typography color="text.secondary" sx={{ textAlign: 'center' }}>
                                    Type at least two characters to search the workspace.
                                </Typography>
                            </Stack>
                        ) : searchQuery.isError ? (
                            <Alert severity="error" sx={{ m: 2 }}>
                                Search could not be completed. Try again.
                            </Alert>
                        ) : !searchQuery.isFetching && results.length === 0 ? (
                            <Stack
                                sx={{
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    minHeight: 220,
                                    px: 3,
                                }}
                            >
                                <Typography sx={{ fontWeight: 650 }}>No results</Typography>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                    sx={{ textAlign: 'center' }}
                                >
                                    No accessible projects, tasks, or people match this search.
                                </Typography>
                            </Stack>
                        ) : (
                            <List disablePadding aria-label="Search results">
                                {results.map((result) => (
                                    <ListItemButton
                                        key={`${result.type}-${result.id}`}
                                        onClick={() => openResult(result)}
                                        sx={{ px: 2, py: 1.15 }}
                                    >
                                        <ListItemIcon sx={{ minWidth: 40 }}>
                                            {resultIcon(result.type)}
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={result.title}
                                            secondary={
                                                result.subtitle ?? resultTypeLabels[result.type]
                                            }
                                        />
                                        <Chip
                                            label={resultTypeLabels[result.type]}
                                            size="small"
                                            variant="outlined"
                                            sx={{ ml: 1 }}
                                        />
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                    </Box>
                </DialogContent>
            </Dialog>
        </>
    )
}
