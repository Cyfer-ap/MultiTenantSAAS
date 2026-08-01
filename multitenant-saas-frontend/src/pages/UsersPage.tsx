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

import { useAuth } from '../features/auth/hooks/useAuth'
import type {
    TenantRole,
    UserStatus,
} from '../features/auth/types/auth'
import { useTenantUsers } from '../features/users/hooks/useTenantUsers'
import type {
    SortDirection,
    TenantUsersQueryParams,
    UserSortField,
} from '../features/users/types/users'

type RoleFilter = TenantRole | 'ALL'
type StatusFilter = UserStatus | 'ALL'

const roleLabels: Record<TenantRole, string> = {
    TENANT_ADMIN: 'Administrator',
    TENANT_MANAGER: 'Manager',
    TENANT_USER: 'User',
}

const statusLabels: Record<UserStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
}

function getStatusColor(
    status: UserStatus,
): 'success' | 'default' | 'warning' {
    if (status === 'ACTIVE') {
        return 'success'
    }

    if (status === 'SUSPENDED') {
        return 'warning'
    }

    return 'default'
}

function formatDate(value: string): string {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The tenant users could not be loaded.'
}

function UsersTableSkeleton() {
    return (
        <Box aria-label="Loading users" role="status">
            {[0, 1, 2, 3, 4].map((row) => (
                <Stack
                    direction="row"
                    key={row}
                    spacing={2}
                    sx={{ padding: 2 }}
                >
                    <Skeleton width="35%" />
                    <Skeleton width="20%" />
                    <Skeleton width="15%" />
                    <Skeleton width="15%" />
                </Stack>
            ))}
        </Box>
    )
}

export function UsersPage() {
    const { session } = useAuth()
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [role, setRole] =
        useState<RoleFilter>('ALL')
    const [status, setStatus] =
        useState<StatusFilter>('ALL')
    const [sortBy, setSortBy] =
        useState<UserSortField>('createdAt')
    const [sortDir, setSortDir] =
        useState<SortDirection>('desc')

    const queryParams: TenantUsersQueryParams = {
        page,
        size,
        sortBy,
        sortDir,
        ...(role === 'ALL' ? {} : { role }),
        ...(status === 'ALL' ? {} : { status }),
        ...(search ? { search } : {}),
    }

    const usersQuery = useTenantUsers(
        session?.tenantId ?? '',
        queryParams,
    )

    const hasFilters =
        search.length > 0 ||
        role !== 'ALL' ||
        status !== 'ALL'

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
        setRole('ALL')
        setStatus('ALL')
    }

    const changeSort = (
        nextSortBy: UserSortField,
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
                        Users
                    </Typography>

                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Browse tenant users, roles, and account statuses.
                    </Typography>
                </Box>

                <Button
                    startIcon={
                        usersQuery.isFetching
                            ? (
                                <CircularProgress
                                    color="inherit"
                                    size={16}
                                />
                            )
                            : <RefreshRoundedIcon />
                    }
                    disabled={usersQuery.isFetching}
                    onClick={() => {
                        void usersQuery.refetch()
                    }}
                    size="small"
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            <Paper
                component="form"
                onSubmit={submitSearch}
                variant="outlined"
                sx={{ marginTop: 3, padding: 2 }}
            >
                <Stack
                    direction={{ xs: 'column', md: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { md: 'center' } }}
                >
                    <TextField
                        label="Search users"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
                        placeholder="Name or email"
                        size="small"
                        value={searchDraft}
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
                    />

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="user-role-filter-label">
                            Role
                        </InputLabel>
                        <Select
                            label="Role"
                            labelId="user-role-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setRole(
                                    event.target.value as RoleFilter,
                                )
                            }}
                            value={role}
                        >
                            <MenuItem value="ALL">All roles</MenuItem>
                            <MenuItem value="TENANT_ADMIN">
                                Administrator
                            </MenuItem>
                            <MenuItem value="TENANT_MANAGER">
                                Manager
                            </MenuItem>
                            <MenuItem value="TENANT_USER">
                                User
                            </MenuItem>
                        </Select>
                    </FormControl>

                    <FormControl size="small" sx={{ minWidth: 170 }}>
                        <InputLabel id="user-status-filter-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="user-status-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setStatus(
                                    event.target.value as StatusFilter,
                                )
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

                    {hasFilters && (
                        <Button onClick={clearFilters}>
                            Clear filters
                        </Button>
                    )}
                </Stack>
            </Paper>

            <Paper
                variant="outlined"
                sx={{ marginTop: 2, overflow: 'hidden' }}
            >
                {usersQuery.isFetching && !usersQuery.isPending && (
                    <LinearProgress aria-label="Updating users" />
                )}

                {usersQuery.isPending && <UsersTableSkeleton />}

                {usersQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void usersQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(usersQuery.error)}
                    </Alert>
                )}

                {usersQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Tenant users">
                                <TableHead>
                                    <TableRow>
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'fullName'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={
                                                    sortBy === 'fullName'
                                                }
                                                direction={
                                                    sortBy === 'fullName'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('fullName')
                                                }}
                                            >
                                                User
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
                                        <TableCell
                                            sortDirection={
                                                sortBy === 'createdAt'
                                                    ? sortDir
                                                    : false
                                            }
                                        >
                                            <TableSortLabel
                                                active={
                                                    sortBy === 'createdAt'
                                                }
                                                direction={
                                                    sortBy === 'createdAt'
                                                        ? sortDir
                                                        : 'asc'
                                                }
                                                onClick={() => {
                                                    changeSort('createdAt')
                                                }}
                                            >
                                                Created
                                            </TableSortLabel>
                                        </TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {usersQuery.data.content.map(
                                        (user) => (
                                            <TableRow key={user.id}>
                                                <TableCell>
                                                    <Typography
                                                        variant="body2"
                                                        sx={{ fontWeight: 600 }}
                                                    >
                                                        {user.fullName}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {user.email}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    {roleLabels[user.role]}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={getStatusColor(
                                                            user.status,
                                                        )}
                                                        label={
                                                            statusLabels[
                                                                user.status
                                                            ]
                                                        }
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    {formatDate(
                                                        user.createdAt,
                                                    )}
                                                </TableCell>
                                            </TableRow>
                                        ),
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>

                        {usersQuery.data.content.length === 0 && (
                            <Box
                                sx={{
                                    padding: 4,
                                    textAlign: 'center',
                                }}
                            >
                                <Typography variant="h6">
                                    No users found
                                </Typography>
                                <Typography
                                    color="text.secondary"
                                    variant="body2"
                                    sx={{ marginTop: 0.5 }}
                                >
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'This tenant does not have any users yet.'}
                                </Typography>
                            </Box>
                        )}

                        <TablePagination
                            component="div"
                            count={usersQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[10, 25, 50]}
                        />
                    </>
                )}
            </Paper>
        </Box>
    )
}
