import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AssignmentOutlinedIcon from '@mui/icons-material/AssignmentOutlined'
import FolderOutlinedIcon from '@mui/icons-material/FolderOutlined'
import PersonAddAltRoundedIcon from '@mui/icons-material/PersonAddAltRounded'
import PersonOutlineRoundedIcon from '@mui/icons-material/PersonOutlineRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined'
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
import type { KeyboardEvent as ReactKeyboardEvent, ReactNode } from 'react'
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'

import { useGlobalSearch } from '../hooks/useGlobalSearch'
import type { GlobalSearchResult, GlobalSearchResultType } from '../types/search'

export interface CommandPaletteNavigationItem {
    label: string
    path: string
    icon: ReactNode
}

interface CommandPaletteProps {
    tenantId: string
    navigationItems: readonly CommandPaletteNavigationItem[]
    canCreateProject: boolean
    canInviteUser: boolean
}

interface PaletteEntry {
    key: string
    label: string
    description: string
    icon: ReactNode
    target: string
    badge: string
    keywords: readonly string[]
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

function matchesEntry(entry: PaletteEntry, query: string): boolean {
    if (!query) {
        return true
    }

    const haystack = [entry.label, entry.description, ...entry.keywords].join(' ').toLowerCase()
    return query
        .toLowerCase()
        .split(/\s+/)
        .filter(Boolean)
        .every((token) => haystack.includes(token))
}

function getShortcutLabel(): string {
    if (typeof navigator === 'undefined') {
        return 'Ctrl K'
    }

    return /Mac|iPhone|iPad|iPod/i.test(navigator.userAgent) ? '⌘K' : 'Ctrl K'
}

export function CommandPalette({
    tenantId,
    navigationItems,
    canCreateProject,
    canInviteUser,
}: CommandPaletteProps) {
    const navigate = useNavigate()
    const theme = useTheme()
    const compact = useMediaQuery(theme.breakpoints.down('sm'))
    const shortcutLabel = useMemo(getShortcutLabel, [])
    const [open, setOpen] = useState(false)
    const [query, setQuery] = useState('')
    const [debouncedQuery, setDebouncedQuery] = useState('')
    const [activeIndex, setActiveIndex] = useState(0)

    useEffect(() => {
        const timeout = window.setTimeout(() => setDebouncedQuery(query.trim()), 250)
        return () => window.clearTimeout(timeout)
    }, [query])

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            const commandShortcut =
                event.key.toLowerCase() === 'k' && (event.metaKey || event.ctrlKey) && !event.altKey

            if (commandShortcut) {
                event.preventDefault()
                setOpen(true)
                return
            }

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

    const quickActions = useMemo<PaletteEntry[]>(() => {
        const entries: PaletteEntry[] = []

        if (canCreateProject) {
            entries.push({
                key: 'create-project',
                label: 'Create project',
                description: 'Start a new workspace project',
                icon: <AddRoundedIcon fontSize="small" />,
                target: '/projects?action=create',
                badge: 'Action',
                keywords: ['new', 'project', 'create'],
            })
        }

        if (canInviteUser) {
            entries.push({
                key: 'invite-user',
                label: 'Invite user',
                description: 'Invite a person to this workspace',
                icon: <PersonAddAltRoundedIcon fontSize="small" />,
                target: '/invitations?action=create',
                badge: 'Action',
                keywords: ['new', 'member', 'person', 'invite'],
            })
        }

        entries.push({
            key: 'account-settings',
            label: 'Account settings',
            description: 'Open your account and security settings',
            icon: <SettingsOutlinedIcon fontSize="small" />,
            target: '/account',
            badge: 'Navigate',
            keywords: ['profile', 'password', 'settings', 'account'],
        })

        return entries
    }, [canCreateProject, canInviteUser])

    const navigationCommands = useMemo<PaletteEntry[]>(
        () =>
            navigationItems.map((item) => ({
                key: `navigate-${item.path}`,
                label: `Open ${item.label}`,
                description: `Go to ${item.label}`,
                icon: item.icon,
                target: item.path,
                badge: 'Navigate',
                keywords: ['open', 'go', 'navigate', item.label],
            })),
        [navigationItems],
    )

    const filteredQuickActions = useMemo(
        () => quickActions.filter((entry) => matchesEntry(entry, query.trim())),
        [quickActions, query],
    )
    const filteredNavigation = useMemo(
        () => navigationCommands.filter((entry) => matchesEntry(entry, query.trim())),
        [navigationCommands, query],
    )
    const resultEntries = useMemo<PaletteEntry[]>(
        () =>
            (searchQuery.data?.results ?? []).map((result) => ({
                key: `result-${result.type}-${result.id}`,
                label: result.title,
                description: result.subtitle ?? resultTypeLabels[result.type],
                icon: resultIcon(result.type),
                target: resultTarget(result),
                badge: resultTypeLabels[result.type],
                keywords: [result.type, result.title, result.subtitle ?? ''],
            })),
        [searchQuery.data],
    )

    const entries = useMemo(
        () => [...filteredQuickActions, ...filteredNavigation, ...resultEntries],
        [filteredNavigation, filteredQuickActions, resultEntries],
    )
    const entryIndexes = useMemo(
        () => new Map(entries.map((entry, index) => [entry.key, index])),
        [entries],
    )

    useEffect(() => {
        setActiveIndex(0)
    }, [open, query])

    useEffect(() => {
        if (activeIndex >= entries.length) {
            setActiveIndex(Math.max(0, entries.length - 1))
        }
    }, [activeIndex, entries.length])

    const closePalette = () => {
        setOpen(false)
        setQuery('')
        setDebouncedQuery('')
        setActiveIndex(0)
    }

    const executeEntry = (entry: PaletteEntry) => {
        navigate(entry.target)
        closePalette()
    }

    const handleInputKeyDown = (event: ReactKeyboardEvent<HTMLInputElement>) => {
        if (entries.length === 0) {
            return
        }

        if (event.key === 'ArrowDown') {
            event.preventDefault()
            setActiveIndex((current) => (current + 1) % entries.length)
            return
        }

        if (event.key === 'ArrowUp') {
            event.preventDefault()
            setActiveIndex((current) => (current - 1 + entries.length) % entries.length)
            return
        }

        if (event.key === 'Enter') {
            event.preventDefault()
            const activeEntry = entries[activeIndex]
            if (activeEntry) {
                executeEntry(activeEntry)
            }
        }
    }

    const renderEntries = (label: string, sectionEntries: readonly PaletteEntry[]) => {
        if (sectionEntries.length === 0) {
            return null
        }

        return (
            <Box component="section" aria-label={label}>
                <Typography
                    color="text.secondary"
                    variant="overline"
                    sx={{ display: 'block', px: 2, pt: 1.25, pb: 0.5 }}
                >
                    {label}
                </Typography>
                <List disablePadding>
                    {sectionEntries.map((entry) => {
                        const index = entryIndexes.get(entry.key) ?? -1
                        return (
                            <ListItemButton
                                key={entry.key}
                                selected={index === activeIndex}
                                onClick={() => executeEntry(entry)}
                                onMouseEnter={() => setActiveIndex(index)}
                                sx={{ px: 2, py: 1.05 }}
                            >
                                <ListItemIcon sx={{ minWidth: 40 }}>{entry.icon}</ListItemIcon>
                                <ListItemText primary={entry.label} secondary={entry.description} />
                                <Chip
                                    label={entry.badge}
                                    size="small"
                                    variant="outlined"
                                    sx={{ ml: 1 }}
                                />
                            </ListItemButton>
                        )
                    })}
                </List>
            </Box>
        )
    }

    const hasLocalMatches = filteredQuickActions.length > 0 || filteredNavigation.length > 0
    const showEmptyState =
        query.trim().length >= 2 &&
        !searchQuery.isFetching &&
        !searchQuery.isError &&
        !hasLocalMatches &&
        resultEntries.length === 0

    return (
        <>
            {compact ? (
                <Tooltip title={`Command palette (${shortcutLabel})`}>
                    <IconButton aria-label="Open command palette" onClick={() => setOpen(true)}>
                        <SearchRoundedIcon />
                    </IconButton>
                </Tooltip>
            ) : (
                <Button
                    aria-label="Open command palette"
                    color="inherit"
                    onClick={() => setOpen(true)}
                    startIcon={<SearchRoundedIcon />}
                    variant="outlined"
                    sx={{
                        borderColor: 'divider',
                        color: 'text.secondary',
                        justifyContent: 'space-between',
                        minWidth: { sm: 230, lg: 340 },
                        px: 1.5,
                        textTransform: 'none',
                    }}
                >
                    <Box component="span" sx={{ flexGrow: 1, textAlign: 'left' }}>
                        Search or jump to…
                    </Box>
                    <Chip label={shortcutLabel} size="small" sx={{ ml: 1, height: 22 }} />
                </Button>
            )}

            <Dialog
                fullWidth
                maxWidth="sm"
                open={open}
                onClose={closePalette}
                aria-labelledby="command-palette-title"
            >
                <DialogContent sx={{ p: 0 }}>
                    <Box sx={{ p: 2, pb: 1.25 }}>
                        <Stack direction="row" sx={{ alignItems: 'center', mb: 1.25 }}>
                            <Box sx={{ flexGrow: 1 }}>
                                <Typography id="command-palette-title" sx={{ fontWeight: 750 }}>
                                    Command palette
                                </Typography>
                                <Typography color="text.secondary" variant="caption">
                                    Search accessible work or run a workspace command.
                                </Typography>
                            </Box>
                            <Chip label="↑↓ Enter" size="small" variant="outlined" />
                        </Stack>
                        <TextField
                            autoFocus
                            fullWidth
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            onKeyDown={handleInputKeyDown}
                            placeholder="Search or type a command"
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

                    <Box
                        sx={{
                            borderTop: 1,
                            borderColor: 'divider',
                            maxHeight: 'min(62vh, 520px)',
                            minHeight: 260,
                            overflowY: 'auto',
                            pb: 1,
                        }}
                    >
                        {renderEntries('Quick actions', filteredQuickActions)}
                        {renderEntries('Navigate', filteredNavigation)}

                        {searchQuery.isError && (
                            <Alert severity="error" sx={{ m: 2 }}>
                                Workspace search could not be completed. Commands are still
                                available.
                            </Alert>
                        )}

                        {renderEntries('Search results', resultEntries)}

                        {query.trim().length === 1 && !hasLocalMatches && (
                            <Stack
                                sx={{
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    minHeight: 180,
                                    px: 3,
                                }}
                            >
                                <Typography color="text.secondary" sx={{ textAlign: 'center' }}>
                                    Keep typing to search projects, tasks, and people.
                                </Typography>
                            </Stack>
                        )}

                        {showEmptyState && (
                            <Stack
                                sx={{
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    minHeight: 180,
                                    px: 3,
                                }}
                            >
                                <Typography sx={{ fontWeight: 650 }}>No results</Typography>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                    sx={{ textAlign: 'center' }}
                                >
                                    No accessible work or command matches this query.
                                </Typography>
                            </Stack>
                        )}
                    </Box>
                </DialogContent>
            </Dialog>
        </>
    )
}
