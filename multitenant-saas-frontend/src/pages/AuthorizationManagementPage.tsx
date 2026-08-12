import AddRoundedIcon from '@mui/icons-material/AddRounded'
import BlockRoundedIcon from '@mui/icons-material/BlockRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import {
    Alert,
    Autocomplete,
    Box,
    Button,
    Chip,
    CircularProgress,
    IconButton,
    LinearProgress,
    Paper,
    Snackbar,
    Stack,
    Tab,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tabs,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    CreateAuthorizationAssignmentDialog,
    CreateAuthorizationRoleDialog,
    DeactivateAuthorizationAssignmentDialog,
    DeactivateAuthorizationRoleDialog,
    EditAuthorizationRolePermissionsDialog,
} from '../features/authorization/components/AuthorizationManagementDialogs'
import {
    useAuthorizationAssignmentReferenceData,
    useAuthorizationPermissions,
    useAuthorizationRoles,
    useInitializeDefaultRoles,
    useUserAuthorizationAssignments,
} from '../features/authorization/hooks/useAuthorizationManagement'
import type {
    AuthorizationAssignmentReferenceData,
    AuthorizationAssignmentUserOption,
    AuthorizationRole,
    AuthorizationScopeType,
    AuthorizationUserRoleAssignment,
} from '../features/authorization/types/authorization'

type AuthorizationTab = 'roles' | 'assignments' | 'permissions'

type RoleDialog = 'create' | 'permissions' | 'deactivate' | null

const scopeLabels: Record<AuthorizationScopeType, string> = {
    TENANT: 'Entire tenant',
    ORGANIZATIONAL_UNIT: 'Organizational unit',
    ORGANIZATIONAL_SUBTREE: 'Organizational subtree',
    DIRECT_REPORTS: 'Direct reports',
    PROJECT: 'Project',
    SELF: 'Self',
}

function formatDateTime(value: string | null): string {
    if (!value) {
        return 'No expiry'
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

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function getScopeTargetOption(
    assignment: AuthorizationUserRoleAssignment,
    referenceData: AuthorizationAssignmentReferenceData | undefined,
) {
    if (!assignment.scopeTargetId || !referenceData) {
        return null
    }

    const options =
        assignment.scopeType === 'PROJECT'
            ? referenceData.projects
            : assignment.scopeType === 'DIRECT_REPORTS'
              ? referenceData.directReportsAnchors
              : assignment.scopeType === 'ORGANIZATIONAL_UNIT' ||
                  assignment.scopeType === 'ORGANIZATIONAL_SUBTREE'
                ? referenceData.organizationalUnits
                : []

    return options.find((option) => option.id === assignment.scopeTargetId) ?? null
}

export function AuthorizationManagementPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const [tab, setTab] = useState<AuthorizationTab>('roles')
    const [roleDialog, setRoleDialog] = useState<RoleDialog>(null)
    const [selectedRole, setSelectedRole] = useState<AuthorizationRole | null>(null)
    const [selectedUser, setSelectedUser] = useState<AuthorizationAssignmentUserOption | null>(null)
    const [assignmentDialogOpen, setAssignmentDialogOpen] = useState(false)
    const [deactivateAssignment, setDeactivateAssignment] =
        useState<AuthorizationUserRoleAssignment | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const permissionsQuery = useAuthorizationPermissions(tenantId)
    const rolesQuery = useAuthorizationRoles(tenantId)
    const assignmentReferenceDataQuery = useAuthorizationAssignmentReferenceData(tenantId)
    const selectedUserId = selectedUser?.id ?? ''
    const assignmentsQuery = useUserAuthorizationAssignments(tenantId, selectedUserId)
    const initializeRolesMutation = useInitializeDefaultRoles(tenantId)

    const permissions = permissionsQuery.data ?? []
    const roles = rolesQuery.data ?? []
    const activeRoles = roles.filter((role) => role.status === 'ACTIVE')
    const assignments = assignmentsQuery.data ?? []
    const selectedUserLabel = assignments[0]?.userFullName ?? selectedUser?.fullName ?? ''

    const openRoleDialog = (
        dialog: Exclude<RoleDialog, null>,
        role: AuthorizationRole | null = null,
    ): void => {
        setSelectedRole(role)
        setRoleDialog(dialog)
    }

    const closeRoleDialog = (): void => {
        setSelectedRole(null)
        setRoleDialog(null)
    }

    const refresh = async (): Promise<void> => {
        const requests: Promise<unknown>[] = [
            permissionsQuery.refetch(),
            rolesQuery.refetch(),
            assignmentReferenceDataQuery.refetch(),
        ]

        if (selectedUserId) {
            requests.push(assignmentsQuery.refetch())
        }

        await Promise.all(requests)
    }

    const isRefreshing =
        permissionsQuery.isFetching ||
        rolesQuery.isFetching ||
        assignmentsQuery.isFetching ||
        assignmentReferenceDataQuery.isFetching
    const roleDataError = permissionsQuery.isError || rolesQuery.isError

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
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <SecurityRoundedIcon color="primary" />
                        <Typography component="h1" variant="h4">
                            Authorization
                        </Typography>
                    </Stack>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Manage tenant roles, scoped grants, and the permission catalog.
                    </Typography>
                </Box>

                <Button
                    disabled={isRefreshing}
                    onClick={() => {
                        void refresh()
                    }}
                    startIcon={
                        isRefreshing ? (
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

            <Paper variant="outlined" sx={{ marginTop: 3 }}>
                <Tabs
                    onChange={(_event, value: AuthorizationTab) => {
                        setTab(value)
                    }}
                    value={tab}
                    variant="scrollable"
                >
                    <Tab label="Roles" value="roles" />
                    <Tab label="Assignments" value="assignments" />
                    <Tab label="Permissions" value="permissions" />
                </Tabs>
            </Paper>

            {isRefreshing && (
                <LinearProgress aria-label="Updating authorization data" sx={{ marginTop: 1 }} />
            )}

            {tab === 'roles' && (
                <Box sx={{ marginTop: 2 }}>
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={1}
                        sx={{
                            justifyContent: 'flex-end',
                            marginBottom: 2,
                        }}
                    >
                        <Button
                            disabled={initializeRolesMutation.isPending}
                            onClick={() => {
                                initializeRolesMutation.mutate(undefined, {
                                    onSuccess: () => {
                                        setFeedback(
                                            'Default roles were initialized and synchronized.',
                                        )
                                    },
                                })
                            }}
                            variant="outlined"
                        >
                            Initialize defaults
                        </Button>
                        <Button
                            disabled={permissions.length === 0}
                            onClick={() => {
                                openRoleDialog('create')
                            }}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Create role
                        </Button>
                    </Stack>

                    {initializeRolesMutation.isError && (
                        <Alert severity="error" sx={{ marginBottom: 2 }}>
                            {getErrorMessage(
                                initializeRolesMutation.error,
                                'Default roles could not be initialized.',
                            )}
                        </Alert>
                    )}

                    {roleDataError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                rolesQuery.error ?? permissionsQuery.error,
                                'Authorization roles could not be loaded.',
                            )}
                        </Alert>
                    )}

                    {(rolesQuery.isPending || permissionsQuery.isPending) && (
                        <Paper
                            aria-label="Loading authorization roles"
                            role="status"
                            sx={{ padding: 4 }}
                            variant="outlined"
                        >
                            <CircularProgress />
                        </Paper>
                    )}

                    {!roleDataError && rolesQuery.isSuccess && permissionsQuery.isSuccess && (
                        <Paper variant="outlined">
                            <TableContainer>
                                <Table aria-label="Authorization roles">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Role</TableCell>
                                            <TableCell>Source</TableCell>
                                            <TableCell>Status</TableCell>
                                            <TableCell>Permissions</TableCell>
                                            <TableCell align="right">Actions</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {roles.map((role) => {
                                            const tenantRoleIsActive =
                                                role.source === 'TENANT' && role.status === 'ACTIVE'

                                            return (
                                                <TableRow key={role.id}>
                                                    <TableCell>
                                                        <Typography
                                                            sx={{
                                                                fontWeight: 700,
                                                            }}
                                                            variant="body2"
                                                        >
                                                            {role.name}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="caption"
                                                        >
                                                            {role.code}
                                                        </Typography>
                                                        {role.description && (
                                                            <Typography
                                                                color="text.secondary"
                                                                sx={{
                                                                    display: 'block',
                                                                    marginTop: 0.5,
                                                                }}
                                                                variant="caption"
                                                            >
                                                                {role.description}
                                                            </Typography>
                                                        )}
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip
                                                            label={
                                                                role.source === 'SYSTEM'
                                                                    ? 'System'
                                                                    : 'Tenant'
                                                            }
                                                            size="small"
                                                            variant="outlined"
                                                        />
                                                    </TableCell>
                                                    <TableCell>
                                                        <Chip
                                                            color={
                                                                role.status === 'ACTIVE'
                                                                    ? 'success'
                                                                    : 'default'
                                                            }
                                                            label={
                                                                role.status === 'ACTIVE'
                                                                    ? 'Active'
                                                                    : 'Inactive'
                                                            }
                                                            size="small"
                                                        />
                                                    </TableCell>
                                                    <TableCell>{role.permissions.length}</TableCell>
                                                    <TableCell align="right">
                                                        {tenantRoleIsActive && (
                                                            <Stack
                                                                direction="row"
                                                                spacing={0.5}
                                                                sx={{
                                                                    justifyContent: 'flex-end',
                                                                }}
                                                            >
                                                                <Tooltip title="Edit permissions">
                                                                    <IconButton
                                                                        aria-label={`Edit permissions for ${role.name}`}
                                                                        onClick={() => {
                                                                            openRoleDialog(
                                                                                'permissions',
                                                                                role,
                                                                            )
                                                                        }}
                                                                        size="small"
                                                                    >
                                                                        <EditOutlinedIcon />
                                                                    </IconButton>
                                                                </Tooltip>
                                                                <Tooltip title="Deactivate role">
                                                                    <IconButton
                                                                        aria-label={`Deactivate ${role.name}`}
                                                                        color="error"
                                                                        onClick={() => {
                                                                            openRoleDialog(
                                                                                'deactivate',
                                                                                role,
                                                                            )
                                                                        }}
                                                                        size="small"
                                                                    >
                                                                        <BlockRoundedIcon />
                                                                    </IconButton>
                                                                </Tooltip>
                                                            </Stack>
                                                        )}
                                                    </TableCell>
                                                </TableRow>
                                            )
                                        })}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Paper>
                    )}
                </Box>
            )}

            {tab === 'assignments' && (
                <Box sx={{ marginTop: 2 }}>
                    <Paper sx={{ padding: 2 }} variant="outlined">
                        <Autocomplete
                            autoHighlight
                            getOptionLabel={(option) => `${option.fullName} (${option.email})`}
                            isOptionEqualToValue={(option, value) => option.id === value.id}
                            loading={assignmentReferenceDataQuery.isFetching}
                            onChange={(_event, option) => {
                                setSelectedUser(option)
                                setAssignmentDialogOpen(false)
                            }}
                            options={assignmentReferenceDataQuery.data?.users ?? []}
                            renderInput={(params) => (
                                <TextField
                                    {...params}
                                    helperText="Search active tenant users by name or email."
                                    label="User"
                                    placeholder="Start typing a name or email"
                                />
                            )}
                            value={selectedUser}
                        />
                    </Paper>

                    {assignmentReferenceDataQuery.isError && (
                        <Alert severity="error" sx={{ marginTop: 2 }}>
                            {getErrorMessage(
                                assignmentReferenceDataQuery.error,
                                'Assignment selector data could not be loaded.',
                            )}
                        </Alert>
                    )}

                    {selectedUserId && (
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={2}
                            sx={{
                                alignItems: { sm: 'center' },
                                justifyContent: 'space-between',
                                marginTop: 2,
                            }}
                        >
                            <Box>
                                <Typography component="h2" variant="h6">
                                    Assignments for {selectedUserLabel}
                                </Typography>
                                <Typography color="text.secondary" variant="caption">
                                    {selectedUser?.email}
                                </Typography>
                            </Box>
                            <Button
                                disabled={activeRoles.length === 0}
                                onClick={() => {
                                    setAssignmentDialogOpen(true)
                                }}
                                startIcon={<AddRoundedIcon />}
                                variant="contained"
                            >
                                Assign role
                            </Button>
                        </Stack>
                    )}

                    {assignmentsQuery.isPending && selectedUserId && (
                        <Paper
                            aria-label="Loading authorization assignments"
                            role="status"
                            sx={{ marginTop: 2, padding: 4 }}
                            variant="outlined"
                        >
                            <CircularProgress />
                        </Paper>
                    )}

                    {assignmentsQuery.isError && (
                        <Alert severity="error" sx={{ marginTop: 2 }}>
                            {getErrorMessage(
                                assignmentsQuery.error,
                                'Authorization assignments could not be loaded.',
                            )}
                        </Alert>
                    )}

                    {assignmentsQuery.isSuccess && selectedUserId && (
                        <Paper sx={{ marginTop: 2 }} variant="outlined">
                            <TableContainer>
                                <Table aria-label="Authorization assignments">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Role</TableCell>
                                            <TableCell>Scope</TableCell>
                                            <TableCell>Status</TableCell>
                                            <TableCell>Validity</TableCell>
                                            <TableCell align="right">Actions</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {assignments.map((assignment) => (
                                            <TableRow key={assignment.id}>
                                                <TableCell>
                                                    <Typography
                                                        sx={{ fontWeight: 700 }}
                                                        variant="body2"
                                                    >
                                                        {assignment.roleName}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {assignment.roleCode}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">
                                                        {scopeLabels[assignment.scopeType]}
                                                    </Typography>
                                                    {assignment.scopeTargetId && (
                                                        <Box>
                                                            <Typography variant="body2">
                                                                {getScopeTargetOption(
                                                                    assignment,
                                                                    assignmentReferenceDataQuery.data,
                                                                )?.label ?? 'Unavailable target'}
                                                            </Typography>
                                                            {getScopeTargetOption(
                                                                assignment,
                                                                assignmentReferenceDataQuery.data,
                                                            )?.description && (
                                                                <Typography
                                                                    color="text.secondary"
                                                                    variant="caption"
                                                                >
                                                                    {
                                                                        getScopeTargetOption(
                                                                            assignment,
                                                                            assignmentReferenceDataQuery.data,
                                                                        )?.description
                                                                    }
                                                                </Typography>
                                                            )}
                                                        </Box>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={
                                                            assignment.status === 'ACTIVE'
                                                                ? 'success'
                                                                : 'default'
                                                        }
                                                        label={
                                                            assignment.status === 'ACTIVE'
                                                                ? 'Active'
                                                                : 'Inactive'
                                                        }
                                                        size="small"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">
                                                        From {formatDateTime(assignment.validFrom)}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        Until{' '}
                                                        {formatDateTime(assignment.validUntil)}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell align="right">
                                                    {assignment.status === 'ACTIVE' && (
                                                        <Tooltip title="Deactivate assignment">
                                                            <IconButton
                                                                aria-label={`Deactivate ${assignment.roleName} assignment`}
                                                                color="error"
                                                                onClick={() => {
                                                                    setDeactivateAssignment(
                                                                        assignment,
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
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>

                            {assignments.length === 0 && (
                                <Box
                                    sx={{
                                        padding: 4,
                                        textAlign: 'center',
                                    }}
                                >
                                    <Typography variant="h6">No assignments found</Typography>
                                    <Typography color="text.secondary">
                                        Assign a role to create the first scoped grant for this
                                        user.
                                    </Typography>
                                </Box>
                            )}
                        </Paper>
                    )}
                </Box>
            )}

            {tab === 'permissions' && (
                <Box sx={{ marginTop: 2 }}>
                    {permissionsQuery.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                permissionsQuery.error,
                                'The permission catalog could not be loaded.',
                            )}
                        </Alert>
                    )}

                    {permissionsQuery.isPending && (
                        <Paper
                            aria-label="Loading permission catalog"
                            role="status"
                            sx={{ padding: 4 }}
                            variant="outlined"
                        >
                            <CircularProgress />
                        </Paper>
                    )}

                    {permissionsQuery.isSuccess && (
                        <Paper variant="outlined">
                            <TableContainer>
                                <Table aria-label="Authorization permissions">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Permission</TableCell>
                                            <TableCell>Category</TableCell>
                                            <TableCell>Source</TableCell>
                                            <TableCell>Status</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {permissions.map((permission) => (
                                            <TableRow key={permission.id}>
                                                <TableCell>
                                                    <Typography
                                                        sx={{ fontWeight: 700 }}
                                                        variant="body2"
                                                    >
                                                        {permission.name}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {permission.code}
                                                    </Typography>
                                                    {permission.description && (
                                                        <Typography
                                                            color="text.secondary"
                                                            sx={{
                                                                display: 'block',
                                                                marginTop: 0.5,
                                                            }}
                                                            variant="caption"
                                                        >
                                                            {permission.description}
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>{permission.category}</TableCell>
                                                <TableCell>
                                                    {permission.source === 'SYSTEM'
                                                        ? 'System'
                                                        : 'Tenant'}
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={
                                                            permission.status === 'ACTIVE'
                                                                ? 'success'
                                                                : 'default'
                                                        }
                                                        label={
                                                            permission.status === 'ACTIVE'
                                                                ? 'Active'
                                                                : 'Inactive'
                                                        }
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                        </Paper>
                    )}
                </Box>
            )}

            {roleDialog === 'create' && (
                <CreateAuthorizationRoleDialog
                    onClose={closeRoleDialog}
                    onSuccess={setFeedback}
                    permissions={permissions}
                    tenantId={tenantId}
                />
            )}

            {roleDialog === 'permissions' && selectedRole && (
                <EditAuthorizationRolePermissionsDialog
                    onClose={closeRoleDialog}
                    onSuccess={setFeedback}
                    permissions={permissions}
                    role={selectedRole}
                    tenantId={tenantId}
                />
            )}

            {roleDialog === 'deactivate' && selectedRole && (
                <DeactivateAuthorizationRoleDialog
                    onClose={closeRoleDialog}
                    onSuccess={setFeedback}
                    role={selectedRole}
                    tenantId={tenantId}
                />
            )}

            {assignmentDialogOpen && selectedUserId && assignmentReferenceDataQuery.data && (
                <CreateAuthorizationAssignmentDialog
                    onClose={() => {
                        setAssignmentDialogOpen(false)
                    }}
                    onSuccess={setFeedback}
                    roles={activeRoles}
                    tenantId={tenantId}
                    userDisplayName={selectedUserLabel}
                    userId={selectedUserId}
                    referenceData={assignmentReferenceDataQuery.data}
                />
            )}

            {deactivateAssignment && (
                <DeactivateAuthorizationAssignmentDialog
                    assignment={deactivateAssignment}
                    onClose={() => {
                        setDeactivateAssignment(null)
                    }}
                    onSuccess={setFeedback}
                    tenantId={tenantId}
                />
            )}

            <Snackbar
                autoHideDuration={5000}
                message={feedback}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
