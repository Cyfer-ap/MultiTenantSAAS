import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
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

import { usePlatformAuditLogs } from '../features/system-admin/hooks/usePlatformAuditLogs'
import {
    platformAuditActions,
} from '../features/system-admin/types/systemAdmin'
import type {
    PlatformAuditAction,
    PlatformAuditLogsQueryParams,
    PlatformAuditSortField,
    SortDirection,
} from '../features/system-admin/types/systemAdmin'

type ActionFilter = PlatformAuditAction | 'ALL'
type OutcomeFilter = 'ALL' | 'SUCCESS' | 'FAILURE'

function formatLabel(value: string): string {
    const normalized = value.toLowerCase().replaceAll('_', ' ')
    return normalized.charAt(0).toUpperCase() + normalized.slice(1)
}

function formatTimestamp(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function AuditTableSkeleton() {
    return (
        <Box aria-label="Loading platform audit logs" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack direction="row" key={row} spacing={2} sx={{ p: 2 }}>
                    <Skeleton width="38%" />
                    <Skeleton width="20%" />
                    <Skeleton width="20%" />
                    <Skeleton width="10%" />
                    <Skeleton width="12%" />
                </Stack>
            ))}
        </Box>
    )
}

export function PlatformAuditLogsPage() {
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [action, setAction] = useState<ActionFilter>('ALL')
    const [outcome, setOutcome] = useState<OutcomeFilter>('ALL')
    const [sortBy, setSortBy] =
        useState<PlatformAuditSortField>('createdAt')
    const [sortDir, setSortDir] = useState<SortDirection>('desc')

    const queryParams: PlatformAuditLogsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(action === 'ALL' ? {} : { action }),
        ...(outcome === 'ALL'
            ? {}
            : { success: outcome === 'SUCCESS' }),
        ...(search ? { search } : {}),
    }
    const query = usePlatformAuditLogs(queryParams)
    const hasFilters =
        search.length > 0 || action !== 'ALL' || outcome !== 'ALL'

    const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const changeSort = (nextSortBy: PlatformAuditSortField): void => {
        setPage(0)
        if (nextSortBy === sortBy) {
            setSortDir((current) => current === 'asc' ? 'desc' : 'asc')
            return
        }
        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setAction('ALL')
        setOutcome('ALL')
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
                        Platform audit
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Review system-administrator account operations.
                    </Typography>
                </Box>
                <Button
                    disabled={query.isFetching}
                    onClick={() => { void query.refetch() }}
                    startIcon={
                        query.isFetching
                            ? <CircularProgress color="inherit" size={16} />
                            : <RefreshRoundedIcon />
                    }
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            <Paper
                component="form"
                onSubmit={submitSearch}
                sx={{ mt: 3, p: 2 }}
                variant="outlined"
            >
                <Stack
                    direction={{ xs: 'column', lg: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { lg: 'center' } }}
                >
                    <TextField
                        label="Search platform events"
                        onChange={(event) => { setSearchDraft(event.target.value) }}
                        placeholder="Actor, target, or message"
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
                        sx={{ flexGrow: 1, minWidth: 260 }}
                        value={searchDraft}
                    />
                    <FormControl size="small" sx={{ minWidth: 250 }}>
                        <InputLabel id="platform-action-filter-label">Action</InputLabel>
                        <Select
                            label="Action"
                            labelId="platform-action-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setAction(event.target.value as ActionFilter)
                            }}
                            value={action}
                        >
                            <MenuItem value="ALL">All actions</MenuItem>
                            {platformAuditActions.map((value) => (
                                <MenuItem key={value} value={value}>
                                    {formatLabel(value)}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="platform-outcome-filter-label">Outcome</InputLabel>
                        <Select
                            label="Outcome"
                            labelId="platform-outcome-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setOutcome(event.target.value as OutcomeFilter)
                            }}
                            value={outcome}
                        >
                            <MenuItem value="ALL">All outcomes</MenuItem>
                            <MenuItem value="SUCCESS">Successful</MenuItem>
                            <MenuItem value="FAILURE">Failed</MenuItem>
                        </Select>
                    </FormControl>
                    <Button type="submit" variant="contained">Search</Button>
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
                    <LinearProgress aria-label="Updating platform audit logs" />
                )}
                {query.isPending && <AuditTableSkeleton />}
                {query.isError && (
                    <Alert
                        action={(
                            <Button
                                color="inherit"
                                onClick={() => { void query.refetch() }}
                            >
                                Retry
                            </Button>
                        )}
                        severity="error"
                        sx={{ m: 2 }}
                    >
                        {query.error instanceof Error
                            ? query.error.message
                            : 'The platform audit logs could not be loaded.'}
                    </Alert>
                )}
                {query.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Platform audit logs">
                                <TableHead>
                                    <TableRow>
                                        <TableCell sx={{ minWidth: 310 }}>
                                            <TableSortLabel
                                                active={sortBy === 'action'}
                                                direction={sortBy === 'action' ? sortDir : 'asc'}
                                                onClick={() => { changeSort('action') }}
                                            >
                                                Event
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 210 }}>Actor</TableCell>
                                        <TableCell sx={{ minWidth: 210 }}>Target</TableCell>
                                        <TableCell>
                                            <TableSortLabel
                                                active={sortBy === 'success'}
                                                direction={sortBy === 'success' ? sortDir : 'asc'}
                                                onClick={() => { changeSort('success') }}
                                            >
                                                Outcome
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 180 }}>
                                            <TableSortLabel
                                                active={sortBy === 'createdAt'}
                                                direction={sortBy === 'createdAt' ? sortDir : 'desc'}
                                                onClick={() => { changeSort('createdAt') }}
                                            >
                                                Occurred
                                            </TableSortLabel>
                                        </TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {query.data.content.map((log) => (
                                        <TableRow hover key={log.id}>
                                            <TableCell>
                                                <Typography sx={{ fontWeight: 600 }} variant="body2">
                                                    {formatLabel(log.action)}
                                                </Typography>
                                                <Typography
                                                    color="text.secondary"
                                                    sx={{ mt: 0.5, maxWidth: 460, overflowWrap: 'anywhere' }}
                                                    variant="caption"
                                                >
                                                    {log.message}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography sx={{ overflowWrap: 'anywhere' }} variant="body2">
                                                    {log.actorSystemAdminEmail ?? 'System'}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography color={log.targetSystemAdminEmail ? 'text.primary' : 'text.secondary'} sx={{ overflowWrap: 'anywhere' }} variant="body2">
                                                    {log.targetSystemAdminEmail ?? '—'}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Chip
                                                    color={log.success ? 'success' : 'error'}
                                                    label={log.success ? 'Successful' : 'Failed'}
                                                    size="small"
                                                    variant="outlined"
                                                />
                                            </TableCell>
                                            <TableCell>{formatTimestamp(log.createdAt)}</TableCell>
                                        </TableRow>
                                    ))}
                                </TableBody>
                            </Table>
                        </TableContainer>
                        {query.data.content.length === 0 && (
                            <Box sx={{ p: 5, textAlign: 'center' }}>
                                <Typography variant="h6">No platform events found</Typography>
                                <Typography color="text.secondary" sx={{ mt: 0.5 }} variant="body2">
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'System-administrator operations will appear here.'}
                                </Typography>
                            </Box>
                        )}
                        <TablePagination
                            component="div"
                            count={query.data.totalElements}
                            onPageChange={(_event, nextPage) => { setPage(nextPage) }}
                            onRowsPerPageChange={(event) => { setPage(0); setSize(Number(event.target.value)) }}
                            page={Math.min(page, Math.max(query.data.totalPages - 1, 0))}
                            rowsPerPage={size}
                            rowsPerPageOptions={[5, 10, 25, 50]}
                        />
                    </>
                )}
            </Paper>
        </Box>
    )
}
