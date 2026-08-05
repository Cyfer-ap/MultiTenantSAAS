import {
    Alert,
    Box,
    Button,
    Checkbox,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import {
    useCreateAuthorizationAssignment,
    useCreateAuthorizationRole,
    useDeactivateAuthorizationAssignment,
    useDeactivateAuthorizationRole,
    useReplaceAuthorizationRolePermissions,
} from '../hooks/useAuthorizationManagement'
import type {
    AuthorizationPermission,
    AuthorizationRole,
    AuthorizationScopeType,
    AuthorizationUserRoleAssignment,
} from '../types/authorization'

function getErrorMessage(
    error: unknown,
    fallback: string,
): string {
    return error instanceof Error ? error.message : fallback
}

function normalizeRoleCode(value: string): string {
    return value
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
}

function toIsoTimestamp(value: string): string | null {
    if (!value) {
        return null
    }

    const date = new Date(value)

    return Number.isNaN(date.getTime())
        ? null
        : date.toISOString()
}

interface PermissionSelectionProps {
    permissions: readonly AuthorizationPermission[]
    selectedIds: readonly string[]
    onToggle: (permissionId: string) => void
}

function PermissionSelection({
    permissions,
    selectedIds,
    onToggle,
}: PermissionSelectionProps) {
    const activePermissions = permissions.filter(
        (permission) => permission.status === 'ACTIVE',
    )
    const categories = Array.from(
        new Set(
            activePermissions.map(
                (permission) => permission.category,
            ),
        ),
    ).sort()

    return (
        <Box
            sx={{
                border: 1,
                borderColor: 'divider',
                borderRadius: 1,
                maxHeight: 330,
                overflowY: 'auto',
                padding: 2,
            }}
        >
            {categories.map((category) => (
                <Box key={category} sx={{ marginBottom: 2 }}>
                    <Typography
                        color="text.secondary"
                        sx={{
                            fontWeight: 700,
                            textTransform: 'uppercase',
                        }}
                        variant="caption"
                    >
                        {category}
                    </Typography>
                    <FormGroup>
                        {activePermissions
                            .filter(
                                (permission) =>
                                    permission.category === category,
                            )
                            .map((permission) => (
                                <FormControlLabel
                                    control={
                                        <Checkbox
                                            checked={selectedIds.includes(
                                                permission.id,
                                            )}
                                            onChange={() => {
                                                onToggle(permission.id)
                                            }}
                                        />
                                    }
                                    key={permission.id}
                                    label={
                                        <Box>
                                            <Typography variant="body2">
                                                {permission.name}
                                            </Typography>
                                            <Typography
                                                color="text.secondary"
                                                variant="caption"
                                            >
                                                {permission.code}
                                            </Typography>
                                        </Box>
                                    }
                                />
                            ))}
                    </FormGroup>
                </Box>
            ))}
        </Box>
    )
}

interface CreateAuthorizationRoleDialogProps {
    tenantId: string
    permissions: readonly AuthorizationPermission[]
    onClose: () => void
    onSuccess: (message: string) => void
}

export function CreateAuthorizationRoleDialog({
    tenantId,
    permissions,
    onClose,
    onSuccess,
}: CreateAuthorizationRoleDialogProps) {
    const [code, setCode] = useState('')
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [permissionIds, setPermissionIds] =
        useState<string[]>([])
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation = useCreateAuthorizationRole(tenantId)

    const togglePermission = (permissionId: string): void => {
        setPermissionIds((current) =>
            current.includes(permissionId)
                ? current.filter((id) => id !== permissionId)
                : [...current, permissionId],
        )
    }

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()

        const normalizedCode = normalizeRoleCode(code)
        const normalizedName = name.trim()

        if (!normalizedCode || !normalizedName) {
            setValidationError(
                'Role code and role name are required.',
            )
            return
        }

        setValidationError(null)
        mutation.mutate(
            {
                code: normalizedCode,
                name: normalizedName,
                description: description.trim() || null,
                permissionIds,
            },
            {
                onSuccess: (role) => {
                    onSuccess(
                        `${role.name} was created successfully.`,
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="md"
            onClose={onClose}
            open
        >
            <DialogTitle>Create authorization role</DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="create-authorization-role-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {(validationError || mutation.isError) && (
                        <Alert severity="error">
                            {validationError ??
                                getErrorMessage(
                                    mutation.error,
                                    'The role could not be created.',
                                )}
                        </Alert>
                    )}
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={2}
                    >
                        <TextField
                            fullWidth
                            label="Role code"
                            onChange={(event) => {
                                setCode(event.target.value)
                            }}
                            placeholder="PROJECT_COORDINATOR"
                            value={code}
                        />
                        <TextField
                            fullWidth
                            label="Role name"
                            onChange={(event) => {
                                setName(event.target.value)
                            }}
                            value={name}
                        />
                    </Stack>
                    <TextField
                        fullWidth
                        label="Description"
                        multiline
                        minRows={2}
                        onChange={(event) => {
                            setDescription(event.target.value)
                        }}
                        value={description}
                    />
                    <Box>
                        <Typography sx={{ marginBottom: 1 }}>
                            Permissions
                        </Typography>
                        <PermissionSelection
                            onToggle={togglePermission}
                            permissions={permissions}
                            selectedIds={permissionIds}
                        />
                    </Box>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="create-authorization-role-form"
                    type="submit"
                    variant="contained"
                >
                    Create role
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface EditAuthorizationRolePermissionsDialogProps {
    tenantId: string
    role: AuthorizationRole
    permissions: readonly AuthorizationPermission[]
    onClose: () => void
    onSuccess: (message: string) => void
}

export function EditAuthorizationRolePermissionsDialog({
    tenantId,
    role,
    permissions,
    onClose,
    onSuccess,
}: EditAuthorizationRolePermissionsDialogProps) {
    const [permissionIds, setPermissionIds] =
        useState<string[]>(
            role.permissions.map((permission) => permission.id),
        )
    const mutation =
        useReplaceAuthorizationRolePermissions(tenantId)

    const togglePermission = (permissionId: string): void => {
        setPermissionIds((current) =>
            current.includes(permissionId)
                ? current.filter((id) => id !== permissionId)
                : [...current, permissionId],
        )
    }

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()
        mutation.mutate(
            {
                roleId: role.id,
                input: { permissionIds },
            },
            {
                onSuccess: () => {
                    onSuccess(
                        `${role.name} permissions were updated.`,
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="md"
            onClose={onClose}
            open
        >
            <DialogTitle>
                Edit permissions for {role.name}
            </DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="edit-authorization-role-permissions-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The permissions could not be updated.',
                            )}
                        </Alert>
                    )}
                    <PermissionSelection
                        onToggle={togglePermission}
                        permissions={permissions}
                        selectedIds={permissionIds}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="edit-authorization-role-permissions-form"
                    type="submit"
                    variant="contained"
                >
                    Save permissions
                </Button>
            </DialogActions>
        </Dialog>
    )
}

const scopeLabels: Record<AuthorizationScopeType, string> = {
    TENANT: 'Entire tenant',
    ORGANIZATIONAL_UNIT: 'Organizational unit',
    ORGANIZATIONAL_SUBTREE: 'Organizational subtree',
    DIRECT_REPORTS: 'Direct reports',
    PROJECT: 'Project',
    SELF: 'User self-service',
}

interface CreateAuthorizationAssignmentDialogProps {
    tenantId: string
    userId: string
    userDisplayName: string
    roles: readonly AuthorizationRole[]
    onClose: () => void
    onSuccess: (message: string) => void
}

export function CreateAuthorizationAssignmentDialog({
    tenantId,
    userId,
    userDisplayName,
    roles,
    onClose,
    onSuccess,
}: CreateAuthorizationAssignmentDialogProps) {
    const activeRoles = roles.filter(
        (role) => role.status === 'ACTIVE',
    )
    const [roleId, setRoleId] = useState(
        activeRoles[0]?.id ?? '',
    )
    const [scopeType, setScopeType] =
        useState<AuthorizationScopeType>('TENANT')
    const [scopeTargetId, setScopeTargetId] = useState('')
    const [validFrom, setValidFrom] = useState('')
    const [validUntil, setValidUntil] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation =
        useCreateAuthorizationAssignment(tenantId)

    const scopeRequiresTarget =
        scopeType !== 'TENANT' && scopeType !== 'SELF'

    const submit = (
        event: FormEvent<HTMLFormElement>,
    ): void => {
        event.preventDefault()

        if (!roleId) {
            setValidationError('Select a role.')
            return
        }

        if (scopeRequiresTarget && !scopeTargetId.trim()) {
            setValidationError(
                'A scope target ID is required for this scope.',
            )
            return
        }

        const validFromIso = toIsoTimestamp(validFrom)
        const validUntilIso = toIsoTimestamp(validUntil)

        if (validFrom && !validFromIso) {
            setValidationError('Valid from is not a valid date.')
            return
        }

        if (validUntil && !validUntilIso) {
            setValidationError('Valid until is not a valid date.')
            return
        }

        if (
            validFromIso &&
            validUntilIso &&
            new Date(validUntilIso).getTime() <=
                new Date(validFromIso).getTime()
        ) {
            setValidationError(
                'Valid until must be later than valid from.',
            )
            return
        }

        setValidationError(null)
        mutation.mutate(
            {
                userId,
                roleId,
                scopeType,
                scopeTargetId: scopeRequiresTarget
                    ? scopeTargetId.trim()
                    : null,
                validFrom: validFromIso,
                validUntil: validUntilIso,
            },
            {
                onSuccess: (assignment) => {
                    onSuccess(
                        `${assignment.roleName} was assigned to ${userDisplayName}.`,
                    )
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <DialogTitle>
                Assign authorization role
            </DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="create-authorization-assignment-form"
                    onSubmit={submit}
                    spacing={2}
                    sx={{ paddingTop: 1 }}
                >
                    {(validationError || mutation.isError) && (
                        <Alert severity="error">
                            {validationError ??
                                getErrorMessage(
                                    mutation.error,
                                    'The role assignment could not be created.',
                                )}
                        </Alert>
                    )}
                    <Alert severity="info">
                        Assigning a role to {userDisplayName} ({userId}).
                    </Alert>
                    <FormControl fullWidth>
                        <InputLabel id="authorization-role-label">
                            Role
                        </InputLabel>
                        <Select
                            label="Role"
                            labelId="authorization-role-label"
                            onChange={(event) => {
                                setRoleId(event.target.value)
                            }}
                            value={roleId}
                        >
                            {activeRoles.map((role) => (
                                <MenuItem key={role.id} value={role.id}>
                                    {role.name} ({role.code})
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    <FormControl fullWidth>
                        <InputLabel id="authorization-scope-label">
                            Scope
                        </InputLabel>
                        <Select
                            label="Scope"
                            labelId="authorization-scope-label"
                            onChange={(event) => {
                                setScopeType(
                                    event.target
                                        .value as AuthorizationScopeType,
                                )
                                setScopeTargetId('')
                            }}
                            value={scopeType}
                        >
                            {Object.entries(scopeLabels).map(
                                ([value, label]) => (
                                    <MenuItem key={value} value={value}>
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                        <FormHelperText>
                            Tenant and self scopes do not require a target ID.
                        </FormHelperText>
                    </FormControl>
                    {scopeRequiresTarget && (
                        <TextField
                            fullWidth
                            helperText="Enter the project, organizational unit, manager assignment, or other target UUID represented by the selected scope."
                            label="Scope target ID"
                            onChange={(event) => {
                                setScopeTargetId(event.target.value)
                            }}
                            value={scopeTargetId}
                        />
                    )}
                    <Stack
                        direction={{ xs: 'column', sm: 'row' }}
                        spacing={2}
                    >
                        <TextField
                            fullWidth
                            label="Valid from"
                            onChange={(event) => {
                                setValidFrom(event.target.value)
                            }}
                            slotProps={{
                                inputLabel: { shrink: true },
                            }}
                            type="datetime-local"
                            value={validFrom}
                        />
                        <TextField
                            fullWidth
                            label="Valid until"
                            onChange={(event) => {
                                setValidUntil(event.target.value)
                            }}
                            slotProps={{
                                inputLabel: { shrink: true },
                            }}
                            type="datetime-local"
                            value={validUntil}
                        />
                    </Stack>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="create-authorization-assignment-form"
                    type="submit"
                    variant="contained"
                >
                    Assign role
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface DeactivateAuthorizationRoleDialogProps {
    tenantId: string
    role: AuthorizationRole
    onClose: () => void
    onSuccess: (message: string) => void
}

export function DeactivateAuthorizationRoleDialog({
    tenantId,
    role,
    onClose,
    onSuccess,
}: DeactivateAuthorizationRoleDialogProps) {
    const mutation = useDeactivateAuthorizationRole(tenantId)

    return (
        <Dialog onClose={onClose} open>
            <DialogTitle>Deactivate role?</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ paddingTop: 1 }}>
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The role could not be deactivated.',
                            )}
                        </Alert>
                    )}
                    <Typography>
                        Deactivate {role.name}? Existing assignments will no longer provide permissions from this role.
                    </Typography>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={() => {
                        mutation.mutate(role.id, {
                            onSuccess: () => {
                                onSuccess(
                                    `${role.name} was deactivated.`,
                                )
                                onClose()
                            },
                        })
                    }}
                    variant="contained"
                >
                    Deactivate role
                </Button>
            </DialogActions>
        </Dialog>
    )
}

interface DeactivateAuthorizationAssignmentDialogProps {
    tenantId: string
    assignment: AuthorizationUserRoleAssignment
    onClose: () => void
    onSuccess: (message: string) => void
}

export function DeactivateAuthorizationAssignmentDialog({
    tenantId,
    assignment,
    onClose,
    onSuccess,
}: DeactivateAuthorizationAssignmentDialogProps) {
    const mutation =
        useDeactivateAuthorizationAssignment(tenantId)

    return (
        <Dialog onClose={onClose} open>
            <DialogTitle>Deactivate assignment?</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ paddingTop: 1 }}>
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The assignment could not be deactivated.',
                            )}
                        </Alert>
                    )}
                    <Typography>
                        Remove the active {assignment.roleName} grant from {assignment.userFullName}?
                    </Typography>
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={onClose}
                >
                    Cancel
                </Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={() => {
                        mutation.mutate(
                            {
                                assignmentId: assignment.id,
                                userId: assignment.userId,
                            },
                            {
                                onSuccess: () => {
                                    onSuccess(
                                        `${assignment.roleName} assignment was deactivated.`,
                                    )
                                    onClose()
                                },
                            },
                        )
                    }}
                    variant="contained"
                >
                    Deactivate assignment
                </Button>
            </DialogActions>
        </Dialog>
    )
}
