import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import BlockRoundedIcon from '@mui/icons-material/BlockRounded'
import DriveFileMoveRoundedIcon from '@mui/icons-material/DriveFileMoveRounded'
import EditRoundedIcon from '@mui/icons-material/EditRounded'
import PersonAddRoundedIcon from '@mui/icons-material/PersonAddRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SyncAltRoundedIcon from '@mui/icons-material/SyncAltRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    IconButton,
    List,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Paper,
    Skeleton,
    Snackbar,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Tooltip,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    hasTenantPermission,
} from '../features/authorization/access/authorizationAccess'
import {
    useCurrentAuthorization,
} from '../features/authorization/hooks/useCurrentAuthorization'
import {
    authorizationPermissionCodes,
} from '../features/authorization/types/authorization'
import {
    flattenOrganizationTree,
    hasOrganizationUnitPermission,
    isOrganizationUnitDescendant,
} from '../features/organization/access/organizationAccess'
import {
    CreateAssignmentDialog,
    DeactivateAssignmentDialog,
    MoveUnitDialog,
    UnitEditorDialog,
    UnitStatusDialog,
} from '../features/organization/components/OrganizationDialogs'
import {
    useOrganizationTree,
    useOrganizationUnitAssignments,
} from '../features/organization/hooks/useOrganization'
import type {
    FlatOrganizationalUnit,
    OrganizationAssignment,
} from '../features/organization/types/organization'

type DialogState =
    | 'create-root'
    | 'create-child'
    | 'edit'
    | 'move'
    | 'status'
    | 'create-assignment'
    | null

function formatDate(value: string | null): string {
    if (!value) {
        return 'No end date'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
    }).format(date)
}

function getErrorMessage(
    error: unknown,
    fallback: string,
): string {
    return error instanceof Error
        ? error.message
        : fallback
}

export function OrganizationPage() {
    const { session } = useAuth()
    const authorizationQuery =
        useCurrentAuthorization()
    const tenantId = session?.tenantId ?? ''
    const treeQuery =
        useOrganizationTree(tenantId)
    const [selectedUnitId, setSelectedUnitId] =
        useState('')
    const [dialog, setDialog] =
        useState<DialogState>(null)
    const [
        deactivateTarget,
        setDeactivateTarget,
    ] = useState<OrganizationAssignment | null>(
        null,
    )
    const [feedback, setFeedback] =
        useState<string | null>(null)

    const units = useMemo(
        () =>
            flattenOrganizationTree(
                treeQuery.data ?? [],
            ),
        [treeQuery.data],
    )

    const selectedUnit =
        units.find(
            (unit) =>
                unit.id === selectedUnitId,
        ) ??
        units[0] ??
        null
    const resolvedSelectedUnitId =
        selectedUnit?.id ?? ''
    const authorization =
        authorizationQuery.data

    const canCreateRoot = hasTenantPermission(
        authorization,
        authorizationPermissionCodes
            .ORGANIZATION_UNIT_MANAGE,
    )

    const canManageSelected = Boolean(
        selectedUnit &&
        hasOrganizationUnitPermission(
            authorization,
            authorizationPermissionCodes
                .ORGANIZATION_UNIT_MANAGE,
            selectedUnit.id,
            units,
        ),
    )

    const canReadSelectedAssignments = Boolean(
        selectedUnit &&
        hasOrganizationUnitPermission(
            authorization,
            authorizationPermissionCodes
                .ORGANIZATION_ASSIGNMENT_READ,
            selectedUnit.id,
            units,
        ),
    )

    const canManageSelectedAssignments = Boolean(
        selectedUnit &&
        hasOrganizationUnitPermission(
            authorization,
            authorizationPermissionCodes
                .ORGANIZATION_ASSIGNMENT_MANAGE,
            selectedUnit.id,
            units,
        ),
    )

    const assignmentsQuery =
        useOrganizationUnitAssignments(
            tenantId,
            selectedUnit?.id ?? '',
            canReadSelectedAssignments,
        )

    const moveParentOptions = useMemo(() => {
        if (!selectedUnit) {
            return []
        }

        return units.filter((candidate) => {
            if (
                candidate.id === selectedUnit.id ||
                candidate.status !== 'ACTIVE' ||
                isOrganizationUnitDescendant(
                    units,
                    candidate.id,
                    selectedUnit.id,
                )
            ) {
                return false
            }

            return hasOrganizationUnitPermission(
                authorization,
                authorizationPermissionCodes
                    .ORGANIZATION_UNIT_MANAGE,
                candidate.id,
                units,
            )
        })
    }, [
        authorization,
        selectedUnit,
        units,
    ])

    if (
        treeQuery.isPending ||
        authorizationQuery.isPending
    ) {
        return (
            <Stack
                aria-label="Loading organization"
                role="status"
                spacing={2}
            >
                <Skeleton height={48} width="40%" />
                <Skeleton
                    height={500}
                    variant="rounded"
                />
            </Stack>
        )
    }

    if (
        treeQuery.isError ||
        authorizationQuery.isError
    ) {
        const error =
            treeQuery.error ??
            authorizationQuery.error

        return (
            <Alert
                action={
                    <Button
                        color="inherit"
                        onClick={() => {
                            void Promise.all([
                                treeQuery.refetch(),
                                authorizationQuery.refetch(),
                            ])
                        }}
                    >
                        Retry
                    </Button>
                }
                severity="error"
            >
                {getErrorMessage(
                    error,
                    'The organization could not be loaded.',
                )}
            </Alert>
        )
    }

    return (
        <Box>
            <Stack
                direction={{
                    xs: 'column',
                    md: 'row',
                }}
                spacing={2}
                sx={{
                    alignItems: {
                        md: 'flex-start',
                    },
                    justifyContent:
                        'space-between',
                }}
            >
                <Box>
                    <Typography
                        component="h1"
                        variant="h4"
                    >
                        Organization
                    </Typography>
                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 0.5 }}
                    >
                        Manage the tenant hierarchy,
                        reporting lines, and organizational
                        assignments.
                    </Typography>
                </Box>

                <Stack
                    direction="row"
                    spacing={1}
                >
                    {canCreateRoot && (
                        <Button
                            onClick={() => {
                                setDialog('create-root')
                            }}
                            startIcon={
                                <AddRoundedIcon />
                            }
                            variant="contained"
                        >
                            Add root unit
                        </Button>
                    )}
                    <Button
                        disabled={treeQuery.isFetching}
                        onClick={() => {
                            void treeQuery.refetch()
                        }}
                        startIcon={
                            treeQuery.isFetching
                                ? (
                                    <CircularProgress
                                        color="inherit"
                                        size={16}
                                    />
                                )
                                : (
                                    <RefreshRoundedIcon />
                                )
                        }
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Stack
                direction={{
                    xs: 'column',
                    lg: 'row',
                }}
                spacing={2}
                sx={{ marginTop: 3 }}
            >
                <Paper
                    sx={{
                        flexBasis: 380,
                        flexShrink: 0,
                        overflow: 'hidden',
                    }}
                    variant="outlined"
                >
                    <Box sx={{ padding: 2 }}>
                        <Typography variant="h6">
                            Unit hierarchy
                        </Typography>
                        <Typography
                            color="text.secondary"
                            variant="body2"
                        >
                            Select a unit to inspect its
                            assignments and available actions.
                        </Typography>
                    </Box>
                    <Divider />

                    {units.length === 0
                        ? (
                            <Box
                                sx={{
                                    padding: 4,
                                    textAlign: 'center',
                                }}
                            >
                                <AccountTreeRoundedIcon
                                    color="disabled"
                                    sx={{ fontSize: 44 }}
                                />
                                <Typography
                                    sx={{ marginTop: 1 }}
                                    variant="h6"
                                >
                                    No organizational units
                                </Typography>
                            </Box>
                        )
                        : (
                            <List
                                aria-label="Organizational units"
                                disablePadding
                            >
                                {units.map((unit) => (
                                    <ListItemButton
                                        key={unit.id}
                                        onClick={() => {
                                            setSelectedUnitId(
                                                unit.id,
                                            )
                                        }}
                                        selected={
                                            resolvedSelectedUnitId ===
                                            unit.id
                                        }
                                        sx={{
                                            paddingLeft:
                                                2 +
                                                unit.depth *
                                                    2.5,
                                        }}
                                    >
                                        <ListItemIcon
                                            sx={{
                                                minWidth: 36,
                                            }}
                                        >
                                            <AccountTreeRoundedIcon
                                                fontSize="small"
                                            />
                                        </ListItemIcon>
                                        <ListItemText
                                            primary={
                                                unit.name
                                            }
                                            secondary={
                                                unit.code ??
                                                unit.type
                                            }
                                        />
                                        <Chip
                                            label={
                                                unit.status ===
                                                'ACTIVE'
                                                    ? 'Active'
                                                    : 'Inactive'
                                            }
                                            size="small"
                                            variant="outlined"
                                        />
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                </Paper>

                <Stack
                    spacing={2}
                    sx={{
                        flexGrow: 1,
                        minWidth: 0,
                    }}
                >
                    {!selectedUnit
                        ? (
                            <Paper
                                sx={{
                                    padding: 5,
                                    textAlign: 'center',
                                }}
                                variant="outlined"
                            >
                                <Typography variant="h6">
                                    Select an organizational
                                    unit
                                </Typography>
                            </Paper>
                        )
                        : (
                            <>
                                <UnitSummary
                                    canManage={
                                        canManageSelected
                                    }
                                    onAction={
                                        setDialog
                                    }
                                    unit={
                                        selectedUnit
                                    }
                                />

                                <Paper
                                    variant="outlined"
                                >
                                    <Stack
                                        direction={{
                                            xs: 'column',
                                            sm: 'row',
                                        }}
                                        spacing={2}
                                        sx={{
                                            alignItems: {
                                                sm: 'center',
                                            },
                                            justifyContent:
                                                'space-between',
                                            padding: 2,
                                        }}
                                    >
                                        <Box>
                                            <Typography
                                                component="h2"
                                                variant="h6"
                                            >
                                                Assignments
                                            </Typography>
                                            <Typography
                                                color="text.secondary"
                                                variant="body2"
                                            >
                                                Users placed
                                                in{' '}
                                                {
                                                    selectedUnit.name
                                                }
                                                .
                                            </Typography>
                                        </Box>

                                        {canManageSelectedAssignments && (
                                            <Button
                                                disabled={
                                                    selectedUnit.status !==
                                                    'ACTIVE'
                                                }
                                                onClick={() => {
                                                    setDialog(
                                                        'create-assignment',
                                                    )
                                                }}
                                                startIcon={
                                                    <PersonAddRoundedIcon />
                                                }
                                                variant="contained"
                                            >
                                                Assign user
                                            </Button>
                                        )}
                                    </Stack>
                                    <Divider />

                                    {!canReadSelectedAssignments
                                        ? (
                                            <Alert
                                                severity="info"
                                                sx={{
                                                    margin: 2,
                                                }}
                                            >
                                                You do not
                                                have permission
                                                to read this
                                                unit&apos;s
                                                assignments.
                                            </Alert>
                                        )
                                        : (
                                            <AssignmentsTable
                                                assignments={
                                                    assignmentsQuery.data ??
                                                    []
                                                }
                                                canManage={
                                                    canManageSelectedAssignments
                                                }
                                                isError={
                                                    assignmentsQuery.isError
                                                }
                                                isPending={
                                                    assignmentsQuery.isPending
                                                }
                                                onDeactivate={
                                                    setDeactivateTarget
                                                }
                                                onRetry={() => {
                                                    void assignmentsQuery.refetch()
                                                }}
                                            />
                                        )}
                                </Paper>
                            </>
                        )}
                </Stack>
            </Stack>

            {dialog === 'create-root' && (
                <UnitEditorDialog
                    mode="create"
                    onClose={() => {
                        setDialog(null)
                    }}
                    onSuccess={setFeedback}
                    parentUnitId={null}
                    tenantId={tenantId}
                />
            )}

            {dialog === 'create-child' &&
                selectedUnit && (
                    <UnitEditorDialog
                        mode="create"
                        onClose={() => {
                            setDialog(null)
                        }}
                        onSuccess={setFeedback}
                        parentUnitId={
                            selectedUnit.id
                        }
                        tenantId={tenantId}
                    />
                )}

            {dialog === 'edit' &&
                selectedUnit && (
                    <UnitEditorDialog
                        mode="edit"
                        onClose={() => {
                            setDialog(null)
                        }}
                        onSuccess={setFeedback}
                        tenantId={tenantId}
                        unit={selectedUnit}
                    />
                )}

            {dialog === 'move' &&
                selectedUnit && (
                    <MoveUnitDialog
                        allowRoot={canCreateRoot}
                        onClose={() => {
                            setDialog(null)
                        }}
                        onSuccess={setFeedback}
                        parentOptions={
                            moveParentOptions
                        }
                        tenantId={tenantId}
                        unit={selectedUnit}
                    />
                )}

            {dialog === 'status' &&
                selectedUnit && (
                    <UnitStatusDialog
                        onClose={() => {
                            setDialog(null)
                        }}
                        onSuccess={setFeedback}
                        tenantId={tenantId}
                        unit={selectedUnit}
                    />
                )}

            {dialog === 'create-assignment' &&
                selectedUnit && (
                    <CreateAssignmentDialog
                        managerOptions={
                            assignmentsQuery.data ??
                            []
                        }
                        onClose={() => {
                            setDialog(null)
                        }}
                        onSuccess={setFeedback}
                        tenantId={tenantId}
                        unitId={selectedUnit.id}
                        unitName={
                            selectedUnit.name
                        }
                    />
                )}

            {deactivateTarget &&
                selectedUnit && (
                    <DeactivateAssignmentDialog
                        assignment={
                            deactivateTarget
                        }
                        onClose={() => {
                            setDeactivateTarget(
                                null,
                            )
                        }}
                        onSuccess={setFeedback}
                        tenantId={tenantId}
                        unitId={selectedUnit.id}
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

interface UnitSummaryProps {
    unit: FlatOrganizationalUnit
    canManage: boolean
    onAction: (action: DialogState) => void
}

function UnitSummary({
    unit,
    canManage,
    onAction,
}: UnitSummaryProps) {
    return (
        <Paper
            sx={{ padding: 3 }}
            variant="outlined"
        >
            <Stack
                direction={{
                    xs: 'column',
                    sm: 'row',
                }}
                spacing={2}
                sx={{
                    alignItems: {
                        sm: 'flex-start',
                    },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ alignItems: 'center' }}
                    >
                        <Typography
                            component="h2"
                            variant="h5"
                        >
                            {unit.name}
                        </Typography>
                        <Chip
                            label={unit.type}
                            size="small"
                        />
                        <Chip
                            color={
                                unit.status === 'ACTIVE'
                                    ? 'success'
                                    : 'default'
                            }
                            label={unit.status}
                            size="small"
                            variant="outlined"
                        />
                    </Stack>
                    <Typography
                        color="text.secondary"
                        sx={{ marginTop: 1 }}
                    >
                        Code: {unit.code ?? 'Not set'}
                    </Typography>
                </Box>

                {canManage && (
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{ flexWrap: 'wrap' }}
                    >
                        <Button
                            disabled={
                                unit.status !== 'ACTIVE'
                            }
                            onClick={() => {
                                onAction(
                                    'create-child',
                                )
                            }}
                            startIcon={
                                <AddRoundedIcon />
                            }
                            size="small"
                        >
                            Add child
                        </Button>
                        <Button
                            onClick={() => {
                                onAction('edit')
                            }}
                            startIcon={
                                <EditRoundedIcon />
                            }
                            size="small"
                        >
                            Edit
                        </Button>
                        <Button
                            onClick={() => {
                                onAction('move')
                            }}
                            startIcon={
                                <DriveFileMoveRoundedIcon />
                            }
                            size="small"
                        >
                            Move
                        </Button>
                        <Button
                            onClick={() => {
                                onAction('status')
                            }}
                            startIcon={
                                <SyncAltRoundedIcon />
                            }
                            size="small"
                        >
                            Status
                        </Button>
                    </Stack>
                )}
            </Stack>
        </Paper>
    )
}

interface AssignmentsTableProps {
    assignments: readonly OrganizationAssignment[]
    canManage: boolean
    isPending: boolean
    isError: boolean
    onDeactivate: (
        assignment: OrganizationAssignment,
    ) => void
    onRetry: () => void
}

function AssignmentsTable({
    assignments,
    canManage,
    isPending,
    isError,
    onDeactivate,
    onRetry,
}: AssignmentsTableProps) {
    if (isPending) {
        return (
            <Stack
                aria-label="Loading organization assignments"
                role="status"
                spacing={1}
                sx={{ padding: 2 }}
            >
                {[0, 1, 2].map((row) => (
                    <Skeleton
                        height={42}
                        key={row}
                    />
                ))}
            </Stack>
        )
    }

    if (isError) {
        return (
            <Alert
                action={
                    <Button
                        color="inherit"
                        onClick={onRetry}
                    >
                        Retry
                    </Button>
                }
                severity="error"
                sx={{ margin: 2 }}
            >
                Assignments could not be loaded.
            </Alert>
        )
    }

    return (
        <>
            <TableContainer>
                <Table aria-label="Organization assignments">
                    <TableHead>
                        <TableRow>
                            <TableCell>User</TableCell>
                            <TableCell>Position</TableCell>
                            <TableCell>Reports to</TableCell>
                            <TableCell>Validity</TableCell>
                            <TableCell>Status</TableCell>
                            {canManage && (
                                <TableCell align="right">
                                    Actions
                                </TableCell>
                            )}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {assignments.map(
                            (assignment) => (
                                <TableRow
                                    key={assignment.id}
                                >
                                    <TableCell>
                                        <Typography
                                            sx={{
                                                fontWeight: 600,
                                            }}
                                            variant="body2"
                                        >
                                            {
                                                assignment.userFullName
                                            }
                                        </Typography>
                                        <Typography
                                            color="text.secondary"
                                            variant="caption"
                                        >
                                            {assignment.primaryAssignment
                                                ? 'Primary assignment'
                                                : assignment.userId}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        {assignment.positionTitle ??
                                            '—'}
                                    </TableCell>
                                    <TableCell>
                                        {assignment.managerUserFullName ??
                                            'No manager'}
                                    </TableCell>
                                    <TableCell>
                                        <Typography variant="body2">
                                            From{' '}
                                            {formatDate(
                                                assignment.validFrom,
                                            )}
                                        </Typography>
                                        <Typography
                                            color="text.secondary"
                                            variant="caption"
                                        >
                                            Until{' '}
                                            {formatDate(
                                                assignment.validUntil,
                                            )}
                                        </Typography>
                                    </TableCell>
                                    <TableCell>
                                        <Chip
                                            color={
                                                assignment.status ===
                                                'ACTIVE'
                                                    ? 'success'
                                                    : 'default'
                                            }
                                            label={
                                                assignment.status
                                            }
                                            size="small"
                                            variant="outlined"
                                        />
                                    </TableCell>
                                    {canManage && (
                                        <TableCell align="right">
                                            {assignment.status ===
                                                'ACTIVE' && (
                                                <Tooltip title="Deactivate assignment">
                                                    <IconButton
                                                        aria-label={`Deactivate assignment for ${assignment.userFullName}`}
                                                        color="error"
                                                        onClick={() => {
                                                            onDeactivate(
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
                                    )}
                                </TableRow>
                            ),
                        )}
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
                    <Typography variant="h6">
                        No assignments
                    </Typography>
                    <Typography
                        color="text.secondary"
                        variant="body2"
                    >
                        No users are assigned to this
                        organizational unit.
                    </Typography>
                </Box>
            )}
        </>
    )
}
