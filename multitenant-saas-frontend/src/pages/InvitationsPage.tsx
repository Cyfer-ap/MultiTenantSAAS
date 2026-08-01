import AddRoundedIcon from '@mui/icons-material/AddRounded'
import BlockRoundedIcon from '@mui/icons-material/BlockRounded'
import MailOutlineRoundedIcon from '@mui/icons-material/MailOutlineRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    FormControl,
    IconButton,
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
    Tooltip,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import type { TenantRole } from '../features/auth/types/auth'
import {
    CreateInvitationDialog,
    RevokeInvitationDialog,
} from '../features/invitations/components/InvitationDialogs'
import { useTenantInvitations } from '../features/invitations/hooks/useTenantInvitations'
import type {
    InvitationSortField,
    InvitationStatus,
    SortDirection,
    TenantInvitation,
    TenantInvitationsQueryParams,
} from '../features/invitations/types/invitations'

type StatusFilter = InvitationStatus | 'ALL'
type RoleFilter = TenantRole | 'ALL'

const roleLabels: Record<TenantRole, string> = {
    TENANT_ADMIN: 'Administrator',
    TENANT_MANAGER: 'Manager',
    TENANT_USER: 'User',
}

const statusLabels: Record<InvitationStatus, string> = {
    PENDING: 'Pending',
    ACCEPTED: 'Accepted',
    REVOKED: 'Revoked',
}

function formatTimestamp(value: string): string {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function getStatusLabel(
    invitation: TenantInvitation,
): string {
    if (
        invitation.status === 'PENDING' &&
        invitation.expired
    ) {
        return 'Expired'
    }

    return statusLabels[invitation.status]
}

function getStatusColor(
    invitation: TenantInvitation,
): 'success' | 'warning' | 'error' | 'default' {
    if (invitation.status === 'ACCEPTED') {
        return 'success'
    }

    if (invitation.status === 'REVOKED') {
        return 'default'
    }

    return invitation.expired ? 'error' : 'warning'
}

function getInviter(invitation: TenantInvitation): string {
    return invitation.invitedByUserEmail ??
        invitation.invitedBySystemAdminEmail ??
        'System'
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'Tenant invitations could not be loaded.'
}

function InvitationsTableSkeleton() {
    return (
        <Box aria-label="Loading invitations" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack
                    direction="row"
                    key={row}
                    spacing={2}
                    sx={{ padding: 2 }}
                >
                    <Skeleton width="32%" />
                    <Skeleton width="15%" />
                    <Skeleton width="15%" />
                    <Skeleton width="20%" />
                </Stack>
            ))}
        </Box>
    )
}

export function InvitationsPage() {
    const { session } = useAuth()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] =
        useState<StatusFilter>('ALL')
    const [role, setRole] =
        useState<RoleFilter>('ALL')
    const [sortBy, setSortBy] =
        useState<InvitationSortField>('createdAt')
    const [sortDir, setSortDir] =
        useState<SortDirection>('desc')
    const [createDialogOpen, setCreateDialogOpen] =
        useState(false)
    const [revokeTarget, setRevokeTarget] =
        useState<TenantInvitation | null>(null)
    const [feedback, setFeedback] =
        useState<string | null>(null)

    const tenantId = session?.tenantId ?? ''
    const queryParams: TenantInvitationsQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(role === 'ALL' ? {} : { role }),
        ...(search ? { search } : {}),
    }
    const invitationsQuery = useTenantInvitations(
        tenantId,
        queryParams,
    )
    const hasFilters =
        search.length > 0 ||
        status !== 'ALL' ||
        role !== 'ALL'

    const submitSearch = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setStatus('ALL')
        setRole('ALL')
    }

    const changeSort = (
        nextSortBy: InvitationSortField,
    ): void => {
        setPage(0)

        if (nextSortBy === sortBy) {
            setSortDir((current) =>
                current === 'asc' ? 'desc' : 'asc',
            )
            return
        }

        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: {
                        xs: 'stretch',
                        sm: 'flex-start',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Invitations
                    </Typography>

                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Invite users and monitor tenant onboarding.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1}>
                    <Button
                        onClick={() => {
                            setCreateDialogOpen(true)
                        }}
                        startIcon={<AddRoundedIcon />}
                        variant="contained"
                    >
                        Invite user
                    </Button>

                    <Button
                        disabled={invitationsQuery.isFetching}
                        onClick={() => {
                            void invitationsQuery.refetch()
                        }}
                        size="small"
                        startIcon={
                            invitationsQuery.isFetching
                                ? (
                                    <CircularProgress
                                        color="inherit"
                                        size={16}
                                    />
                                )
                                : <RefreshRoundedIcon />
                        }
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Paper
                component="form"
                onSubmit={submitSearch}
                sx={{ marginTop: 3, padding: 2 }}
                variant="outlined"
            >
                <Stack
                    direction={{ xs: 'column', lg: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { lg: 'center' } }}
                >
                    <TextField
                        fullWidth
                        label="Search invitations"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
                        size="small"
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchRoundedIcon />
                                    </InputAdornment>
                                ),
                            },
                        }}
                        value={searchDraft}
                    />

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="invitation-status-filter-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="invitation-status-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setStatus(
                                    event.target.value as StatusFilter,
                                )
                            }}
                            value={status}
                        >
                            <MenuItem value="ALL">
                                All statuses
                            </MenuItem>
                            {Object.entries(statusLabels).map(
                                ([value, label]) => (
                                    <MenuItem key={value} value={value}>
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="invitation-role-filter-label">
                            Role
                        </InputLabel>
                        <Select
                            label="Role"
                            labelId="invitation-role-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setRole(
                                    event.target.value as RoleFilter,
                                )
                            }}
                            value={role}
                        >
                            <MenuItem value="ALL">
                                All roles
                            </MenuItem>
                            {Object.entries(roleLabels).map(
                                ([value, label]) => (
                                    <MenuItem key={value} value={value}>
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>

                    <Button type="submit" variant="outlined">
                        Search
                    </Button>

                    {hasFilters && (
                        <Button onClick={clearFilters}>
                            Clear filters
                        </Button>
                    )}
                </Stack>
            </Paper>

            <Paper
                sx={{ marginTop: 2, overflow: 'hidden' }}
                variant="outlined"
            >
                {invitationsQuery.isFetching &&
                    !invitationsQuery.isPending && (
                        <LinearProgress aria-label="Updating invitations" />
                    )}

                {invitationsQuery.isPending && (
                    <InvitationsTableSkeleton />
                )}

                {invitationsQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void invitationsQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(invitationsQuery.error)}
                    </Alert>
                )}

                {invitationsQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Tenant invitations">
                                <TableHead>
                                    <TableRow>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'fullName'
                                                    ? sortDir
                                                    : false
                                            }
                                            sx={{ minWidth: 280 }}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'fullName'}
                                                direction={
                                                    sortBy === 'fullName'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('fullName')
                                                }}
                                            >
                                                Invitee
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'role'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'role'}
                                                direction={
                                                    sortBy === 'role'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('role')
                                                }}
                                            >
                                                Role
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'status'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'status'}
                                                direction={
                                                    sortBy === 'status'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('status')
                                                }}
                                            >
                                                Status
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell sx={{ minWidth: 190 }}>
                                            Invited by
                                        </TableCell>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'expiresAt'
                                                    ? sortDir
                                                    : false
                                            }
                                            sx={{ minWidth: 180 }}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'expiresAt'}
                                                direction={
                                                    sortBy === 'expiresAt'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('expiresAt')
                                                }}
                                            >
                                                Expires
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell align="right">
                                            Actions
                                        </TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {invitationsQuery.data.content.map(
                                        (invitation) => (
                                            <TableRow
                                                hover
                                                key={invitation.invitationId}
                                                sx={{
                                                    '&:last-child td': {
                                                        borderBottom: 0,
                                                    },
                                                }}
                                            >
                                                <TableCell>
                                                    <Stack
                                                        direction="row"
                                                        spacing={1.5}
                                                        sx={{
                                                            alignItems: 'center',
                                                        }}
                                                    >
                                                        <MailOutlineRoundedIcon
                                                            color="action"
                                                        />
                                                        <Box sx={{ minWidth: 0 }}>
                                                            <Typography
                                                                noWrap
                                                                sx={{
                                                                    fontWeight: 600,
                                                                }}
                                                                variant="body2"
                                                            >
                                                                {invitation.fullName}
                                                            </Typography>
                                                            <Typography
                                                                color="text.secondary"
                                                                noWrap
                                                                variant="caption"
                                                            >
                                                                {invitation.email}
                                                            </Typography>
                                                        </Box>
                                                    </Stack>
                                                </TableCell>
                                                <TableCell>
                                                    {roleLabels[invitation.role]}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={getStatusColor(
                                                            invitation,
                                                        )}
                                                        label={getStatusLabel(
                                                            invitation,
                                                        )}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {getInviter(invitation)}
                                                </TableCell>
                                                <TableCell>
                                                    {formatTimestamp(
                                                        invitation.expiresAt,
                                                    )}
                                                </TableCell>
                                                <TableCell align="right">
                                                    {invitation.status ===
                                                        'PENDING' && (
                                                        <Tooltip title="Revoke invitation">
                                                            <IconButton
                                                                aria-label={`Revoke invitation for ${invitation.fullName}`}
                                                                color="error"
                                                                onClick={() => {
                                                                    setRevokeTarget(
                                                                        invitation,
                                                                    )
                                                                }}
                                                                size="small"
                                                            >
                                                                <BlockRoundedIcon />
                                                            </IconButton>
                                                        </Tooltip>
                                                    )}
                                                </TableCell>
                                            </TableRow>
                                        ),
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>

                        {invitationsQuery.data.content.length === 0 && (
                            <Box
                                sx={{
                                    padding: 5,
                                    textAlign: 'center',
                                }}
                            >
                                <MailOutlineRoundedIcon
                                    color="disabled"
                                    sx={{ fontSize: 44 }}
                                />
                                <Typography
                                    sx={{ marginTop: 1 }}
                                    variant="h6"
                                >
                                    No invitations found
                                </Typography>
                                <Typography color="text.secondary">
                                    {hasFilters
                                        ? 'Change or clear the current filters.'
                                        : 'Invite a user to begin tenant onboarding.'}
                                </Typography>
                            </Box>
                        )}

                        <TablePagination
                            component="div"
                            count={invitationsQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[5, 10, 25, 50]}
                        />
                    </>
                )}
            </Paper>

            {createDialogOpen && (
                <CreateInvitationDialog
                    onClose={() => {
                        setCreateDialogOpen(false)
                    }}
                    open
                    tenantId={tenantId}
                />
            )}

            {revokeTarget && (
                <RevokeInvitationDialog
                    invitation={revokeTarget}
                    onClose={() => {
                        setRevokeTarget(null)
                    }}
                    onSuccess={setFeedback}
                    open
                    tenantId={tenantId}
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
