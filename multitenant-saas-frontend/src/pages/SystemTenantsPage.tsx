import AddBusinessRoundedIcon from '@mui/icons-material/AddBusinessRounded'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
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

import type { TenantStatus } from '../features/onboarding/types/onboarding'
import {
    ChangeSystemTenantStatusDialog,
    OnboardSystemTenantDialog,
} from '../features/system-admin/components/SystemTenantDialogs'
import { useSystemTenants } from '../features/system-admin/hooks/useSystemTenants'
import type {
    SortDirection,
    SystemTenant,
    SystemTenantSortField,
    SystemTenantsQueryParams,
} from '../features/system-admin/types/systemAdmin'

type StatusFilter = TenantStatus | 'ALL'

const statusLabels: Record<TenantStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
}

function statusColor(status: TenantStatus): 'success' | 'default' | 'warning' {
    if (status === 'ACTIVE') return 'success'
    if (status === 'SUSPENDED') return 'warning'
    return 'default'
}

function formatDate(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

function TenantTableSkeleton() {
    return (
        <Box aria-label="Loading tenants" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack direction="row" key={row} spacing={2} sx={{ p: 2 }}>
                    <Skeleton width="38%" />
                    <Skeleton width="16%" />
                    <Skeleton width="18%" />
                    <Skeleton width="18%" />
                </Stack>
            ))}
        </Box>
    )
}

export function SystemTenantsPage() {
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] = useState<StatusFilter>('ALL')
    const [sortBy, setSortBy] = useState<SystemTenantSortField>('createdAt')
    const [sortDir, setSortDir] = useState<SortDirection>('desc')
    const [onboardingOpen, setOnboardingOpen] = useState(false)
    const [selectedTenant, setSelectedTenant] = useState<SystemTenant | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const queryParams: SystemTenantsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(search ? { search } : {}),
    }
    const query = useSystemTenants(queryParams)
    const hasFilters = search.length > 0 || status !== 'ALL'

    const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const changeSort = (nextSortBy: SystemTenantSortField): void => {
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
        setStatus('ALL')
    }

    return (
        <Box>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}>
                <Box>
                    <Typography component="h1" variant="h4">Tenants</Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Search, onboard, and control tenant access across the platform.
                    </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                    <Button onClick={() => { setOnboardingOpen(true) }} startIcon={<AddBusinessRoundedIcon />} variant="contained">
                        Onboard tenant
                    </Button>
                    <Button disabled={query.isFetching} onClick={() => { void query.refetch() }} startIcon={query.isFetching ? <CircularProgress color="inherit" size={16} /> : <RefreshRoundedIcon />} variant="outlined">
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Paper component="form" onSubmit={submitSearch} sx={{ mt: 3, p: 2 }} variant="outlined">
                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: { md: 'center' } }}>
                    <TextField
                        label="Search tenants"
                        onChange={(event) => { setSearchDraft(event.target.value) }}
                        placeholder="Workspace name or slug"
                        size="small"
                        slotProps={{ input: { startAdornment: <InputAdornment position="start"><SearchRoundedIcon fontSize="small" /></InputAdornment> } }}
                        sx={{ flexGrow: 1 }}
                        value={searchDraft}
                    />
                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="tenant-status-filter-label">Status</InputLabel>
                        <Select
                            label="Status"
                            labelId="tenant-status-filter-label"
                            onChange={(event) => { setPage(0); setStatus(event.target.value as StatusFilter) }}
                            value={status}
                        >
                            <MenuItem value="ALL">All statuses</MenuItem>
                            <MenuItem value="ACTIVE">Active</MenuItem>
                            <MenuItem value="INACTIVE">Inactive</MenuItem>
                            <MenuItem value="SUSPENDED">Suspended</MenuItem>
                        </Select>
                    </FormControl>
                    <Button type="submit" variant="contained">Search</Button>
                    <Button disabled={!hasFilters && !searchDraft} onClick={clearFilters} type="button">Clear</Button>
                </Stack>
            </Paper>

            <Paper sx={{ mt: 2 }} variant="outlined">
                {query.isLoading && <TenantTableSkeleton />}
                {query.isError && (
                    <Alert action={<Button color="inherit" onClick={() => { void query.refetch() }}>Retry</Button>} severity="error" sx={{ m: 2 }}>
                        {query.error instanceof Error ? query.error.message : 'The tenant directory could not be loaded.'}
                    </Alert>
                )}
                {query.data && (
                    <>
                        <TableContainer>
                            <Table aria-label="Tenant directory">
                                <TableHead>
                                    <TableRow>
                                        <TableCell>
                                            <TableSortLabel active={sortBy === 'name'} direction={sortBy === 'name' ? sortDir : 'asc'} onClick={() => { changeSort('name') }}>
                                                Tenant
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>
                                            <TableSortLabel active={sortBy === 'status'} direction={sortBy === 'status' ? sortDir : 'asc'} onClick={() => { changeSort('status') }}>
                                                Status
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>
                                            <TableSortLabel active={sortBy === 'createdAt'} direction={sortBy === 'createdAt' ? sortDir : 'asc'} onClick={() => { changeSort('createdAt') }}>
                                                Created
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Updated</TableCell>
                                        <TableCell align="right">Actions</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {query.data.content.map((tenant) => (
                                        <TableRow hover key={tenant.id}>
                                            <TableCell sx={{ minWidth: 280 }}>
                                                <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                                                    <Avatar sx={{ bgcolor: 'primary.50', color: 'primary.main', height: 40, width: 40 }}><BusinessRoundedIcon fontSize="small" /></Avatar>
                                                    <Box sx={{ minWidth: 0 }}>
                                                        <Typography noWrap sx={{ fontWeight: 700 }}>{tenant.name}</Typography>
                                                        <Typography color="text.secondary" noWrap variant="body2">{tenant.slug}</Typography>
                                                    </Box>
                                                </Stack>
                                            </TableCell>
                                            <TableCell><Chip color={statusColor(tenant.status)} label={statusLabels[tenant.status]} size="small" variant="outlined" /></TableCell>
                                            <TableCell>{formatDate(tenant.createdAt)}</TableCell>
                                            <TableCell>{formatDate(tenant.updatedAt)}</TableCell>
                                            <TableCell align="right">
                                                <Button onClick={() => { setSelectedTenant(tenant) }} size="small" startIcon={<EditOutlinedIcon />} variant="text">
                                                    Change status
                                                </Button>
                                            </TableCell>
                                        </TableRow>
                                    ))}
                                    {query.data.content.length === 0 && (
                                        <TableRow><TableCell colSpan={5} sx={{ py: 8, textAlign: 'center' }}><Typography color="text.secondary">No tenants match the current filters.</Typography></TableCell></TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
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

            {onboardingOpen && (
                <OnboardSystemTenantDialog
                    onClose={() => { setOnboardingOpen(false) }}
                    onSuccess={setFeedback}
                    open
                />
            )}
            {selectedTenant && (
                <ChangeSystemTenantStatusDialog
                    onClose={() => { setSelectedTenant(null) }}
                    onSuccess={setFeedback}
                    open
                    tenant={selectedTenant}
                />
            )}
            <Snackbar autoHideDuration={4000} message={feedback} onClose={() => { setFeedback(null) }} open={Boolean(feedback)} />
        </Box>
    )
}
