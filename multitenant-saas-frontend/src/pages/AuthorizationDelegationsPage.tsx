import AddRoundedIcon from '@mui/icons-material/AddRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Paper,
    Snackbar,
    Stack,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useAuthorizationDelegationReferenceData,
    useAuthorizationDelegations,
    useCreateAuthorizationDelegation,
    useRevokeAuthorizationDelegation,
} from '../features/authorization/hooks/useAuthorizationDelegation'
import type {
    AuthorizationDelegation,
    AuthorizationDelegationParentAssignmentOption,
    AuthorizationDelegationReferenceData,
    AuthorizationScopeType,
} from '../features/authorization/types/authorization'

const scopeLabels: Record<AuthorizationScopeType, string> = {
    TENANT: 'Entire tenant',
    ORGANIZATIONAL_UNIT: 'Organizational unit',
    ORGANIZATIONAL_SUBTREE: 'Organizational subtree',
    DIRECT_REPORTS: 'Direct reports',
    PROJECT: 'Project',
    SELF: 'Delegate self',
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function toDateTimeLocal(date: Date): string {
    const pad = (value: number) => value.toString().padStart(2, '0')

    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(
        date.getHours(),
    )}:${pad(date.getMinutes())}`
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

function scopeRequiresTarget(scopeType: AuthorizationScopeType): boolean {
    return (
        scopeType === 'PROJECT' ||
        scopeType === 'ORGANIZATIONAL_UNIT' ||
        scopeType === 'ORGANIZATIONAL_SUBTREE'
    )
}

function getAllowedScopeTypes(
    parent: AuthorizationDelegationParentAssignmentOption | undefined,
): AuthorizationScopeType[] {
    if (!parent) {
        return []
    }

    switch (parent.scopeType) {
        case 'TENANT':
            return ['TENANT', 'ORGANIZATIONAL_UNIT', 'ORGANIZATIONAL_SUBTREE', 'PROJECT', 'SELF']
        case 'PROJECT':
            return ['PROJECT']
        case 'ORGANIZATIONAL_UNIT':
            return ['ORGANIZATIONAL_UNIT']
        case 'ORGANIZATIONAL_SUBTREE':
            return ['ORGANIZATIONAL_UNIT', 'ORGANIZATIONAL_SUBTREE']
        case 'SELF':
        case 'DIRECT_REPORTS':
            return []
    }
}

function getScopeTargetLabel(
    delegation: AuthorizationDelegation,
    referenceData: AuthorizationDelegationReferenceData | undefined,
): string | null {
    if (!delegation.scopeTargetId || !referenceData) {
        return null
    }

    const options =
        delegation.scopeType === 'PROJECT'
            ? referenceData.projects
            : delegation.scopeType === 'ORGANIZATIONAL_UNIT' ||
                delegation.scopeType === 'ORGANIZATIONAL_SUBTREE'
              ? referenceData.organizationalUnits
              : []

    return options.find((option) => option.id === delegation.scopeTargetId)?.label ?? null
}

interface CreateDelegationDialogProps {
    open: boolean
    tenantId: string
    referenceData: AuthorizationDelegationReferenceData
    onClose: () => void
    onCreated: () => void
}

function CreateDelegationDialog({
    open,
    tenantId,
    referenceData,
    onClose,
    onCreated,
}: CreateDelegationDialogProps) {
    const createMutation = useCreateAuthorizationDelegation(tenantId)
    const [delegateUserId, setDelegateUserId] = useState('')
    const [parentAssignmentId, setParentAssignmentId] = useState('')
    const [roleId, setRoleId] = useState('')
    const [scopeType, setScopeType] = useState<AuthorizationScopeType | ''>('')
    const [scopeTargetId, setScopeTargetId] = useState('')
    const [validUntil, setValidUntil] = useState(() =>
        toDateTimeLocal(new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)),
    )
    const [validationError, setValidationError] = useState<string | null>(null)

    const parentAssignment = referenceData.parentAssignments.find(
        (assignment) => assignment.id === parentAssignmentId,
    )
    const allowedScopeTypes = getAllowedScopeTypes(parentAssignment)
    const eligibleRoles = parentAssignment
        ? referenceData.roles.filter((role) =>
              role.permissions.every((permission) =>
                  parentAssignment.permissionCodes.includes(permission.code),
              ),
          )
        : []

    const targetOptions =
        scopeType === 'PROJECT'
            ? referenceData.projects
            : scopeType === 'ORGANIZATIONAL_UNIT' || scopeType === 'ORGANIZATIONAL_SUBTREE'
              ? referenceData.organizationalUnits
              : []
    const availableTargetOptions =
        parentAssignment?.scopeType === 'TENANT'
            ? targetOptions
            : targetOptions.filter((option) => option.id === parentAssignment?.scopeTargetId)

    const handleParentAssignmentChange = (nextParentAssignmentId: string): void => {
        const nextParentAssignment = referenceData.parentAssignments.find(
            (assignment) => assignment.id === nextParentAssignmentId,
        )
        const nextScope = getAllowedScopeTypes(nextParentAssignment)[0] ?? ''

        setParentAssignmentId(nextParentAssignmentId)
        setRoleId('')
        setScopeType(nextScope)
        setScopeTargetId(
            nextScope &&
                scopeRequiresTarget(nextScope) &&
                nextParentAssignment?.scopeType !== 'TENANT'
                ? (nextParentAssignment?.scopeTargetId ?? '')
                : '',
        )
        setValidationError(null)
    }

    const handleScopeChange = (nextScope: AuthorizationScopeType): void => {
        setScopeType(nextScope)
        setScopeTargetId(
            scopeRequiresTarget(nextScope) && parentAssignment?.scopeType !== 'TENANT'
                ? (parentAssignment?.scopeTargetId ?? '')
                : '',
        )
    }

    const submit = (): void => {
        setValidationError(null)

        if (!delegateUserId || !parentAssignment || !roleId || !scopeType) {
            setValidationError('Choose a delegate, parent authority, role, and scope.')
            return
        }
        if (scopeRequiresTarget(scopeType) && !scopeTargetId) {
            setValidationError('Choose a target for the selected scope.')
            return
        }

        const validUntilDate = new Date(validUntil)
        if (Number.isNaN(validUntilDate.getTime()) || validUntilDate.getTime() <= Date.now()) {
            setValidationError('Delegation expiry must be in the future.')
            return
        }
        if (
            parentAssignment.validUntil &&
            validUntilDate.getTime() > new Date(parentAssignment.validUntil).getTime()
        ) {
            setValidationError('Delegation cannot outlive its parent authority assignment.')
            return
        }

        createMutation.mutate(
            {
                delegateUserId,
                parentAssignmentId: parentAssignment.id,
                roleId,
                scopeType,
                scopeTargetId: scopeRequiresTarget(scopeType) ? scopeTargetId : null,
                validFrom: null,
                validUntil: validUntilDate.toISOString(),
            },
            {
                onSuccess: () => {
                    onCreated()
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={onClose} open={open}>
            <DialogTitle>Create delegation</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ paddingTop: 1 }}>
                    <Alert severity="info">
                        A delegation can only narrow permissions, scope, and validity from one of
                        your direct active assignments.
                    </Alert>
                    <TextField
                        label="Delegate"
                        onChange={(event) => {
                            setDelegateUserId(event.target.value)
                        }}
                        select
                        value={delegateUserId}
                    >
                        {referenceData.users.map((user) => (
                            <MenuItem key={user.id} value={user.id}>
                                {user.fullName} ({user.email})
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        helperText="Delegated authority cannot be used as a new parent."
                        label="Parent authority"
                        onChange={(event) => {
                            handleParentAssignmentChange(event.target.value)
                        }}
                        select
                        value={parentAssignmentId}
                    >
                        {referenceData.parentAssignments.map((assignment) => (
                            <MenuItem key={assignment.id} value={assignment.id}>
                                {assignment.roleName} · {scopeLabels[assignment.scopeType]}
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        disabled={!parentAssignment}
                        helperText={
                            parentAssignment && eligibleRoles.length === 0
                                ? 'No active role is a safe permission subset of this parent.'
                                : 'Only roles fully contained by the parent permission set are shown.'
                        }
                        label="Delegated role"
                        onChange={(event) => {
                            setRoleId(event.target.value)
                        }}
                        select
                        value={roleId}
                    >
                        {eligibleRoles.map((role) => (
                            <MenuItem key={role.id} value={role.id}>
                                {role.name} ({role.permissions.length} permissions)
                            </MenuItem>
                        ))}
                    </TextField>
                    <TextField
                        disabled={!parentAssignment}
                        label="Scope"
                        onChange={(event) => {
                            handleScopeChange(event.target.value as AuthorizationScopeType)
                        }}
                        select
                        value={scopeType}
                    >
                        {allowedScopeTypes.map((scope) => (
                            <MenuItem key={scope} value={scope}>
                                {scopeLabels[scope]}
                            </MenuItem>
                        ))}
                    </TextField>
                    {scopeType && scopeRequiresTarget(scopeType) && (
                        <TextField
                            disabled={availableTargetOptions.length <= 1}
                            helperText={
                                parentAssignment?.scopeType === 'ORGANIZATIONAL_SUBTREE'
                                    ? 'The UI defaults to the parent subtree root; the API still enforces subtree containment.'
                                    : undefined
                            }
                            label="Scope target"
                            onChange={(event) => {
                                setScopeTargetId(event.target.value)
                            }}
                            select
                            value={scopeTargetId}
                        >
                            {availableTargetOptions.map((option) => (
                                <MenuItem key={option.id} value={option.id}>
                                    {option.label}
                                </MenuItem>
                            ))}
                        </TextField>
                    )}
                    <TextField
                        helperText={
                            parentAssignment?.validUntil
                                ? `Parent expires ${formatDateTime(parentAssignment.validUntil)}.`
                                : 'An explicit expiry is required for every delegation.'
                        }
                        label="Valid until"
                        onChange={(event) => {
                            setValidUntil(event.target.value)
                        }}
                        slotProps={{ inputLabel: { shrink: true } }}
                        type="datetime-local"
                        value={validUntil}
                    />
                    {(validationError || createMutation.isError) && (
                        <Alert severity="error">
                            {validationError ??
                                getErrorMessage(
                                    createMutation.error,
                                    'Authorization delegation could not be created.',
                                )}
                        </Alert>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={createMutation.isPending} onClick={onClose}>
                    Cancel
                </Button>
                <Button disabled={createMutation.isPending} onClick={submit} variant="contained">
                    {createMutation.isPending ? 'Creating…' : 'Create delegation'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function AuthorizationDelegationsPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const referenceDataQuery = useAuthorizationDelegationReferenceData(tenantId)
    const delegationsQuery = useAuthorizationDelegations(tenantId)
    const revokeMutation = useRevokeAuthorizationDelegation(tenantId)
    const [createOpen, setCreateOpen] = useState(false)
    const [revokeTarget, setRevokeTarget] = useState<AuthorizationDelegation | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const refresh = async (): Promise<void> => {
        await Promise.all([referenceDataQuery.refetch(), delegationsQuery.refetch()])
    }

    const isRefreshing = referenceDataQuery.isFetching || delegationsQuery.isFetching
    const dataError = referenceDataQuery.error ?? delegationsQuery.error
    const delegationsEvaluatedAt = delegationsQuery.dataUpdatedAt

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Authorization delegations
                    </Typography>
                    <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                        Delegate bounded authority with explicit provenance, scope, and expiry.
                    </Typography>
                </Box>
                <Stack direction="row" spacing={1}>
                    <Button
                        disabled={isRefreshing}
                        onClick={() => {
                            void refresh()
                        }}
                        startIcon={<RefreshRoundedIcon />}
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                    <Button
                        disabled={
                            !referenceDataQuery.data ||
                            referenceDataQuery.data.parentAssignments.length === 0
                        }
                        onClick={() => {
                            setCreateOpen(true)
                        }}
                        startIcon={<AddRoundedIcon />}
                        variant="contained"
                    >
                        Create delegation
                    </Button>
                </Stack>
            </Stack>

            {dataError && (
                <Alert severity="error" sx={{ marginTop: 2 }}>
                    {getErrorMessage(
                        dataError,
                        'Authorization delegation data could not be loaded.',
                    )}
                </Alert>
            )}

            {!dataError && delegationsQuery.isPending && (
                <Paper role="status" sx={{ marginTop: 2, padding: 4 }} variant="outlined">
                    <CircularProgress aria-label="Loading authorization delegations" />
                </Paper>
            )}

            {!dataError && delegationsQuery.isSuccess && (
                <Paper sx={{ marginTop: 2 }} variant="outlined">
                    <TableContainer>
                        <Table aria-label="Authorization delegations">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Delegator</TableCell>
                                    <TableCell>Delegate</TableCell>
                                    <TableCell>Role</TableCell>
                                    <TableCell>Scope</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Validity</TableCell>
                                    <TableCell align="right">Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {delegationsQuery.data.map((delegation) => {
                                    const expired =
                                        delegation.status === 'ACTIVE' &&
                                        new Date(delegation.validUntil).getTime() <=
                                            delegationsEvaluatedAt
                                    const statusLabel =
                                        delegation.status === 'REVOKED'
                                            ? 'Revoked'
                                            : expired
                                              ? 'Expired'
                                              : 'Active'
                                    const targetLabel = getScopeTargetLabel(
                                        delegation,
                                        referenceDataQuery.data,
                                    )

                                    return (
                                        <TableRow key={delegation.id}>
                                            <TableCell>{delegation.delegatorEmail}</TableCell>
                                            <TableCell>{delegation.delegateEmail}</TableCell>
                                            <TableCell>
                                                <Typography
                                                    sx={{ fontWeight: 700 }}
                                                    variant="body2"
                                                >
                                                    {delegation.roleName}
                                                </Typography>
                                                <Typography
                                                    color="text.secondary"
                                                    variant="caption"
                                                >
                                                    {delegation.roleCode}
                                                </Typography>
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2">
                                                    {scopeLabels[delegation.scopeType]}
                                                </Typography>
                                                {targetLabel && (
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {targetLabel}
                                                    </Typography>
                                                )}
                                            </TableCell>
                                            <TableCell>
                                                <Chip
                                                    color={
                                                        statusLabel === 'Active'
                                                            ? 'success'
                                                            : 'default'
                                                    }
                                                    label={statusLabel}
                                                    size="small"
                                                />
                                            </TableCell>
                                            <TableCell>
                                                <Typography variant="body2">
                                                    {formatDateTime(delegation.validFrom)}
                                                </Typography>
                                                <Typography
                                                    color="text.secondary"
                                                    variant="caption"
                                                >
                                                    to {formatDateTime(delegation.validUntil)}
                                                </Typography>
                                            </TableCell>
                                            <TableCell align="right">
                                                {delegation.status === 'ACTIVE' && !expired && (
                                                    <Button
                                                        color="error"
                                                        onClick={() => {
                                                            setRevokeTarget(delegation)
                                                        }}
                                                        size="small"
                                                    >
                                                        Revoke
                                                    </Button>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    )
                                })}
                                {delegationsQuery.data.length === 0 && (
                                    <TableRow>
                                        <TableCell colSpan={7}>
                                            <Typography color="text.secondary">
                                                No authorization delegations have been created yet.
                                            </Typography>
                                        </TableCell>
                                    </TableRow>
                                )}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Paper>
            )}

            {referenceDataQuery.data && createOpen && (
                <CreateDelegationDialog
                    onClose={() => {
                        setCreateOpen(false)
                    }}
                    onCreated={() => {
                        setFeedback('Authorization delegation created successfully.')
                    }}
                    open={createOpen}
                    referenceData={referenceDataQuery.data}
                    tenantId={tenantId}
                />
            )}

            <Dialog
                onClose={() => {
                    setRevokeTarget(null)
                }}
                open={Boolean(revokeTarget)}
            >
                <DialogTitle>Revoke delegation?</DialogTitle>
                <DialogContent>
                    <Typography>
                        This immediately deactivates the delegated assignment for{' '}
                        {revokeTarget?.delegateEmail}.
                    </Typography>
                    {revokeMutation.isError && (
                        <Alert severity="error" sx={{ marginTop: 2 }}>
                            {getErrorMessage(
                                revokeMutation.error,
                                'Authorization delegation could not be revoked.',
                            )}
                        </Alert>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={revokeMutation.isPending}
                        onClick={() => {
                            setRevokeTarget(null)
                        }}
                    >
                        Cancel
                    </Button>
                    <Button
                        color="error"
                        disabled={!revokeTarget || revokeMutation.isPending}
                        onClick={() => {
                            if (!revokeTarget) {
                                return
                            }
                            revokeMutation.mutate(revokeTarget.id, {
                                onSuccess: () => {
                                    setFeedback('Authorization delegation revoked successfully.')
                                    setRevokeTarget(null)
                                },
                            })
                        }}
                        variant="contained"
                    >
                        {revokeMutation.isPending ? 'Revoking…' : 'Revoke'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Snackbar
                autoHideDuration={4000}
                message={feedback ?? ''}
                onClose={() => {
                    setFeedback(null)
                }}
                open={Boolean(feedback)}
            />
        </Box>
    )
}
