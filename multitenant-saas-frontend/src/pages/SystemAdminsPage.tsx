import AddModeratorRoundedIcon from '@mui/icons-material/AddModeratorRounded'
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import LockOpenRoundedIcon from '@mui/icons-material/LockOpenRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Chip,
    CircularProgress,
    FormControl,
    InputAdornment,
    InputLabel,
    LinearProgress,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Snackbar,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import {
    ChangeSystemAdminStatusDialog,
    CreateSystemAdminDialog,
    UnlockSystemAdminDialog,
} from '../features/system-admin/components/SystemAdminDialogs'
import { useSystemAdmin } from '../features/system-admin/hooks/useSystemAdmin'
import { useSystemAdmins } from '../features/system-admin/hooks/useSystemAdmins'
import type {
    SortDirection,
    SystemAdminRecord,
    SystemAdminSortField,
    SystemAdminsQueryParams,
    SystemAdminStatus,
} from '../features/system-admin/types/systemAdmin'

type StatusFilter = SystemAdminStatus | 'ALL'

const statusLabels: Record<SystemAdminStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
}

function statusColor(status: SystemAdminStatus): 'success' | 'default' | 'warning' {
    if (status === 'ACTIVE') return 'success'
    if (status === 'SUSPENDED') return 'warning'
    return 'default'
}

function formatDate(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function isLoginRestricted(systemAdmin: SystemAdminRecord): boolean {
    if (systemAdmin.failedLoginAttempts > 0) return true
    if (!systemAdmin.lockedUntil) return false

    const lockedUntil = new Date(systemAdmin.lockedUntil)
    return !Number.isNaN(lockedUntil.getTime()) && lockedUntil > new Date()
}

function AdminTableSkeleton() {
    return (
        <Box aria-label="Loading system administrators" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack direction="row" key={row} spacing={2} sx={{ p: 2 }}>
                    <Skeleton width="35%" />
                    <Skeleton width="15%" />
                    <Skeleton width="20%" />
                    <Skeleton width="15%" />
                    <Skeleton width="15%" />
                </Stack>
            ))}
        </Box>
    )
}

export function SystemAdminsPage() {
    const { session } = useSystemAdmin()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] = useState<StatusFilter>('ALL')
    const [sortBy, setSortBy] = useState<SystemAdminSortField>('createdAt')
    const [sortDir, setSortDir] = useState<SortDirection>('desc')
    const [createOpen, setCreateOpen] = useState(false)
    const [statusTarget, setStatusTarget] = useState<SystemAdminRecord | null>(null)
    const [unlockTarget, setUnlockTarget] = useState<SystemAdminRecord | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const queryParams: SystemAdminsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(search ? { search } : {}),
    }
    const query = useSystemAdmins(queryParams)
    const hasFilters = search.length > 0 || status !== 'ALL'

    const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const changeSort = (nextSortBy: SystemAdminSortField): void => {
        setPage(0)
        if (nextSortBy === sortBy) {
            setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
            return
        }
        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setStatus('ALL')
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { sm: 'flex-start' },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        System administrators
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Manage access to the SaaS owner control plane.
                    </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                    <Button
                        onClick={() => {
                            setCreateOpen(true)
                        }}
                        startIcon={<AddModeratorRoundedIcon />}
                        variant="contained"
                    >
                        Add administrator
                    </Button>
                    <Button
                        disabled={query.isFetching}
                        onClick={() => {
                            void query.refetch()
                        }}
                        startIcon={
                            query.isFetching ? (
                                <CircularProgress color="inherit" size={16} />
                            ) : (
                                <RefreshRoundedIcon />
                            )
                        }
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Paper component="form" onSubmit={submitSearch} sx={{ mt: 3, p: 2 }} variant="outlined">
                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { md: 'center' } }}
                >
                    <TextField
                        label="Search administrators"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
                        placeholder="Full name or email"
                        size="small"
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchRoundedIcon fontSize="small" />
                                    </InputAdornment>
                                ),
                            },
                        }}
                        sx={{ flexGrow: 1 }}
                        value={searchDraft}
                    />
                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="system-admin-status-filter-label">Status</InputLabel>
                        <Select
                            label="Status"
                            labelId="system-admin-status-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setStatus(event.target.value as StatusFilter)
                            }}
                            value={status}
                        >
                            <MenuItem value="ALL">All statuses</MenuItem>
                            <MenuItem value="ACTIVE">Active</MenuItem>
                            <MenuItem value="INACTIVE">Inactive</MenuItem>
                            <MenuItem value="SUSPENDED">Suspended</MenuItem>
                        </Select>
                    </FormControl>
                    <Button type="submit" variant="contained">
                        Search
                    </Button>
                    <Button
                        disabled={!hasFilters && !searchDraft}
                        onClick={clearFilters}
                        type="button"
                    >
                        Clear
                    </Button>
                </Stack>
            </Paper>

            <Paper sx={{ mt: 2, overflow: 'hidden' }} variant="outlined">
                {query.isFetching && !query.isPending && (
                    <LinearProgress aria-label="Updating system administrators" />
                )}
                {query.isPending && <AdminTableSkeleton />}
                {query.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void query.refetch()
                                }}
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ m: 2 }}
                    >
                        {query.error instanceof Error
                            ? query.error.message
                            : 'The system administrator directory could not be loaded.'}
                    </Alert>
                )}
                {query.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="System administrator directory">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>
                                            <TableSortLabel
                                                active={sortBy === 'fullName'}
                                                direction={sortBy === 'fullName' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('fullName')
                                                }}
                                            >
                                                Administrator
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>
                                            <TableSortLabel
                                                active={sortBy === 'status'}
                                                direction={sortBy === 'status' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('status')
                                                }}
                                            >
                                                Status
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Login security</TableCell>
                                        <TableCell>
                                            <TableSortLabel
                                                active={sortBy === 'createdAt'}
                                                direction={
                                                    sortBy === 'createdAt' ? sortDir : 'desc'
                                                }
                                                onClick={() => {
                                                    changeSort('createdAt')
                                                }}
                                            >
                                                Created
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell align="right">Actions</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {query.data.content.map((systemAdmin) => {
                                        const isCurrent = systemAdmin.id === session?.systemAdminId
                                        const restricted = isLoginRestricted(systemAdmin)

                                        return (
                                            <TableRow hover key={systemAdmin.id}>
                                                <TableCell sx={{ minWidth: 280 }}>
                                                    <Stack
                                                        direction="row"
                                                        spacing={1.5}
                                                        sx={{ alignItems: 'center' }}
                                                    >
                                                        <Avatar
                                                            sx={{
                                                                bgcolor: 'primary.50',
                                                                color: 'primary.main',
                                                                height: 40,
                                                                width: 40,
                                                            }}
                                                        >
                                                            <AdminPanelSettingsRoundedIcon fontSize="small" />
                                                        </Avatar>
                                                        <Box sx={{ minWidth: 0 }}>
                                                            <Stack
                                                                direction="row"
                                                                spacing={1}
                                                                sx={{ alignItems: 'center' }}
                                                            >
                                                                <Typography
                                                                    noWrap
                                                                    sx={{ fontWeight: 700 }}
                                                                >
                                                                    {systemAdmin.fullName}
                                                                </Typography>
                                                                {isCurrent && (
                                                                    <Chip
                                                                        label="You"
                                                                        size="small"
                                                                    />
                                                                )}
                                                            </Stack>
                                                            <Typography
                                                                color="text.secondary"
                                                                noWrap
                                                                variant="body2"
                                                            >
                                                                {systemAdmin.email}
                                                            </Typography>
                                                        </Box>
                                                    </Stack>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={statusColor(systemAdmin.status)}
                                                        label={statusLabels[systemAdmin.status]}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {restricted ? (
                                                        <Stack spacing={0.25}>
                                                            <Chip
                                                                color="error"
                                                                label="Restricted"
                                                                size="small"
                                                                variant="outlined"
                                                            />
                                                            <Typography
                                                                color="text.secondary"
                                                                variant="caption"
                                                            >
                                                                {systemAdmin.failedLoginAttempts}{' '}
                                                                failed attempt
                                                                {systemAdmin.failedLoginAttempts ===
                                                                1
                                                                    ? ''
                                                                    : 's'}
                                                            </Typography>
                                                        </Stack>
                                                    ) : (
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="body2"
                                                        >
                                                            No lock
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    {formatDate(systemAdmin.createdAt)}
                                                </TableCell>
                                                <TableCell align="right">
                                                    <Stack
                                                        direction="row"
                                                        spacing={0.5}
                                                        sx={{ justifyContent: 'flex-end' }}
                                                    >
                                                        {restricted && (
                                                            <Button
                                                                onClick={() => {
                                                                    setUnlockTarget(systemAdmin)
                                                                }}
                                                                size="small"
                                                                startIcon={<LockOpenRoundedIcon />}
                                                            >
                                                                Unlock
                                                            </Button>
                                                        )}
                                                        <Button
                                                            onClick={() => {
                                                                setStatusTarget(systemAdmin)
                                                            }}
                                                            size="small"
                                                            startIcon={<EditOutlinedIcon />}
                                                        >
                                                            Change status
                                                        </Button>
                                                    </Stack>
                                                </TableCell>
                                            </TableRow>
                                        )
                                    })}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        {query.data.content.length === 0 && (
                            <Box sx={{ p: 5, textAlign: 'center' }}>
                                <Typography variant="h6">No administrators found</Typography>
                                <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'Create another system administrator to share platform operations.'}
                                </Typography>
                            </Box>
                        )}
                        <TablePagination
                            component="div"
                            count={query.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={Math.min(page, Math.max(query.data.totalPages - 1, 0))}
                            rowsPerPage={size}
                            rowsPerPageOptions={[5, 10, 25, 50]}
                        />
                    </>
                )}
            </Paper>

            {createOpen && (
                <CreateSystemAdminDialog
                    onClose={() => {
                        setCreateOpen(false)
                    }}
                    onSuccess={setFeedback}
                    open
                />
            )}
            {statusTarget && session && (
                <ChangeSystemAdminStatusDialog
                    currentSystemAdminId={session.systemAdminId}
                    onClose={() => {
                        setStatusTarget(null)
                    }}
                    onSuccess={setFeedback}
                    open
                    systemAdmin={statusTarget}
                />
            )}
            {unlockTarget && (
                <UnlockSystemAdminDialog
                    onClose={() => {
                        setUnlockTarget(null)
                    }}
                    onSuccess={setFeedback}
                    open
                    systemAdmin={unlockTarget}
                />
            )}
            <Snackbar
                autoHideDuration={4000}
                message={feedback}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
