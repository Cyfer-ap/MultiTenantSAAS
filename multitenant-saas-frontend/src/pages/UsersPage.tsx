import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import LockResetOutlinedIcon from '@mui/icons-material/LockResetOutlined'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import ToggleOnOutlinedIcon from '@mui/icons-material/ToggleOnOutlined'
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
    ListItemIcon,
    Menu,
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
import type {
    FormEvent,
    MouseEvent,
} from 'react'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import type {
    TenantRole,
    UserStatus,
} from '../features/auth/types/auth'
import {
    hasTenantPermission,
} from '../features/authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../features/authorization/hooks/useCurrentAuthorization'
import {
    authorizationPermissionCodes,
} from '../features/authorization/types/authorization'
import { useWorkspaceSubscriptionAccessContext } from '../features/subscriptions/context/WorkspaceSubscriptionAccessContext'
import {
    ChangeUserRoleDialog,
    ChangeUserStatusDialog,
    CreateUserDialog,
    EditUserDialog,
    UnlockUserDialog,
} from '../features/users/components/UserManagementDialogs'
import { useTenantUsers } from '../features/users/hooks/useTenantUsers'
import type {
    SortDirection,
    TenantUser,
    TenantUsersQueryParams,
    UserSortField,
} from '../features/users/types/users'

type RoleFilter = TenantRole | 'ALL'
type StatusFilter = UserStatus | 'ALL'
type UserDialog =
    | 'edit'
    | 'role'
    | 'status'
    | 'unlock'
    | null

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
    const authorization = useCurrentAuthorization()
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
    const [createDialogOpen, setCreateDialogOpen] =
        useState(false)
    const [menuAnchor, setMenuAnchor] =
        useState<HTMLElement | null>(null)
    const [selectedUser, setSelectedUser] =
        useState<TenantUser | null>(null)
    const [activeDialog, setActiveDialog] =
        useState<UserDialog>(null)
    const [feedback, setFeedback] =
        useState<string | null>(null)

    const tenantId = session?.tenantId ?? ''
    const authorizationContext = authorization.data
    const subscriptionAccess =
        useWorkspaceSubscriptionAccessContext()
    const userCreationAllowed =
        subscriptionAccess?.userCreationAllowed ?? true
    const userCreationRestrictionMessage =
        subscriptionAccess?.mutationsAllowed === false
            ? 'The current subscription does not allow creating or reactivating users.'
            : 'The active-user limit has been reached for the current plan. Deactivate a user or change the plan before adding or reactivating another user.'

    const canCreateUser = hasTenantPermission(
        authorizationContext,
        authorizationPermissionCodes.USER_CREATE,
    )

    const canUpdateUsers = hasTenantPermission(
        authorizationContext,
        authorizationPermissionCodes.USER_UPDATE,
    )

    const canManageUserRoles = hasTenantPermission(
        authorizationContext,
        authorizationPermissionCodes.AUTHORIZATION_MANAGE,
    )

    const canUpdateUserStatus = hasTenantPermission(
        authorizationContext,
        authorizationPermissionCodes.USER_STATUS_UPDATE,
    )

    const canManageAnyUser =
        canUpdateUsers ||
        canManageUserRoles ||
        canUpdateUserStatus

    const canManageUser = (userId: string): boolean =>
        canUpdateUsers ||
        canUpdateUserStatus ||
        (
            canManageUserRoles &&
            userId !== session?.userId
        )

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
        tenantId,
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

    const openUserMenu = (
        event: MouseEvent<HTMLButtonElement>,
        user: TenantUser,
    ): void => {
        setSelectedUser(user)
        setMenuAnchor(event.currentTarget)
    }

    const closeUserMenu = (): void => {
        setMenuAnchor(null)
    }

    const openUserDialog = (
        dialog: Exclude<UserDialog, null>,
    ): void => {
        setActiveDialog(dialog)
        closeUserMenu()
    }

    const closeUserDialog = (): void => {
        setActiveDialog(null)
        setSelectedUser(null)
    }

    const showSuccess = (message: string): void => {
        setFeedback(message)
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

                <Stack direction="row" spacing={1}>
                    {canCreateUser && (
                        <Button
                            disabled={!userCreationAllowed}
                            onClick={() => {
                                setCreateDialogOpen(true)
                            }}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Add user
                        </Button>
                    )}

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
            </Stack>

            {canCreateUser && !userCreationAllowed && (
                <Alert
                    severity="warning"
                    sx={{ marginTop: 2 }}
                >
                    {userCreationRestrictionMessage}
                </Alert>
            )}

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
                                        {canManageAnyUser && (
                                            <TableCell align="right">
                                                Actions
                                            </TableCell>
                                        )}
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
                                                {canManageUser(user.id) && (
                                                    <TableCell align="right">
                                                        <IconButton
                                                            aria-label={`Manage ${user.fullName}`}
                                                            onClick={(event) => {
                                                                openUserMenu(
                                                                    event,
                                                                    user,
                                                                )
                                                            }}
                                                            size="small"
                                                        >
                                                            <MoreVertRoundedIcon />
                                                        </IconButton>
                                                    </TableCell>
                                                )}
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

            {(canCreateUser || canManageAnyUser) && (
                <>
                    {canManageAnyUser && (
                        <Menu
                            anchorEl={menuAnchor}
                            onClose={closeUserMenu}
                            open={Boolean(menuAnchor)}
                        >
                            {canUpdateUsers && (
                                <MenuItem
                                    onClick={() => {
                                        openUserDialog('edit')
                                    }}
                                >
                                    <ListItemIcon>
                                        <EditOutlinedIcon fontSize="small" />
                                    </ListItemIcon>
                                    Edit profile
                                </MenuItem>
                            )}
                            {canManageUserRoles && (
                                <MenuItem
                                    disabled={
                                        selectedUser?.id === session?.userId
                                    }
                                    onClick={() => {
                                        openUserDialog('role')
                                    }}
                                >
                                    <ListItemIcon>
                                        <AdminPanelSettingsOutlinedIcon fontSize="small" />
                                    </ListItemIcon>
                                    Change role
                                </MenuItem>
                            )}
                            {canUpdateUserStatus && (
                                <MenuItem
                                    disabled={
                                        selectedUser?.id === session?.userId
                                    }
                                    onClick={() => {
                                        openUserDialog('status')
                                    }}
                                >
                                    <ListItemIcon>
                                        <ToggleOnOutlinedIcon fontSize="small" />
                                    </ListItemIcon>
                                    Change status
                                </MenuItem>
                            )}
                            {canUpdateUserStatus && (
                                <MenuItem
                                    onClick={() => {
                                        openUserDialog('unlock')
                                    }}
                                >
                                    <ListItemIcon>
                                        <LockResetOutlinedIcon fontSize="small" />
                                    </ListItemIcon>
                                    Unlock login
                                </MenuItem>
                            )}
                        </Menu>
                    )}

                    {canCreateUser && createDialogOpen && (
                        <CreateUserDialog
                            onClose={() => {
                                setCreateDialogOpen(false)
                            }}
                            onSuccess={showSuccess}
                            open
                            tenantId={tenantId}
                        />
                    )}
                    {canUpdateUsers &&
                        activeDialog === 'edit' && (
                        <EditUserDialog
                            onClose={closeUserDialog}
                            onSuccess={showSuccess}
                            open
                            tenantId={tenantId}
                            user={selectedUser}
                        />
                    )}
                    {canManageUserRoles &&
                        activeDialog === 'role' && (
                        <ChangeUserRoleDialog
                            onClose={closeUserDialog}
                            onSuccess={showSuccess}
                            open
                            tenantId={tenantId}
                            user={selectedUser}
                        />
                    )}
                    {canUpdateUserStatus &&
                        activeDialog === 'status' && (
                        <ChangeUserStatusDialog
                            activationAllowed={
                                userCreationAllowed
                            }
                            activationRestrictionMessage={
                                userCreationRestrictionMessage
                            }
                            onClose={closeUserDialog}
                            onSuccess={showSuccess}
                            open
                            tenantId={tenantId}
                            user={selectedUser}
                        />
                    )}
                    {canUpdateUserStatus &&
                        activeDialog === 'unlock' && (
                        <UnlockUserDialog
                            onClose={closeUserDialog}
                            onSuccess={showSuccess}
                            open
                            tenantId={tenantId}
                            user={selectedUser}
                        />
                    )}
                </>
            )}

            <Snackbar
                autoHideDuration={6_000}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            >
                <Alert
                    onClose={() => {
                        setFeedback(null)
                    }}
                    severity="success"
                    variant="filled"
                >
                    {feedback}
                </Alert>
            </Snackbar>
        </Box>
    )
}
