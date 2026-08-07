import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    FormControl,
    FormHelperText,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { ApiClientError } from '../../../api/apiError'
import { authStorage } from '../../auth/storage/authStorage'
import type {
    TenantRole,
    UserStatus,
} from '../../auth/types/auth'
import {
    useCreateTenantUser,
    useUnlockTenantUserLogin,
    useUpdateTenantUser,
    useUpdateTenantUserRole,
    useUpdateTenantUserStatus,
} from '../hooks/useUserManagement'
import type { TenantUser } from '../types/users'

interface DialogBaseProps {
    open: boolean
    tenantId: string
    onClose: () => void
    onSuccess: (message: string) => void
}

interface UserDialogProps extends DialogBaseProps {
    user: TenantUser | null
}

interface ChangeUserStatusDialogProps
    extends UserDialogProps {
    activationAllowed?: boolean
    activationRestrictionMessage?: string
}

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

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The requested user change could not be completed.'
}

function getFieldError(
    error: unknown,
    field: string,
): string | undefined {
    return error instanceof ApiClientError
        ? error.details?.[field]
        : undefined
}

export function CreateUserDialog({
    open,
    tenantId,
    onClose,
    onSuccess,
}: DialogBaseProps) {
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [role, setRole] =
        useState<TenantRole>('TENANT_USER')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation = useCreateTenantUser(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        const normalizedName = fullName.trim()
        const normalizedEmail = email.trim().toLowerCase()

        if (normalizedName.length < 2) {
            setValidationError(
                'Full name must contain at least 2 characters.',
            )
            return
        }

        if (!normalizedEmail.includes('@')) {
            setValidationError('Enter a valid email address.')
            return
        }

        if (!passwordPattern.test(password)) {
            setValidationError(
                'Password must be 8–100 characters and contain uppercase, lowercase, number, and special characters with no spaces.',
            )
            return
        }

        setValidationError(null)

        try {
            const createdUser = await mutation.mutateAsync({
                fullName: normalizedName,
                email: normalizedEmail,
                password,
                role,
            })

            onSuccess(
                `${createdUser.fullName} was created successfully.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={closeDialog}
            open={open}
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Add tenant user</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Create credentials and assign the user&apos;s tenant role.
                    </DialogContentText>

                    <Stack spacing={2}>
                        {(validationError || mutation.isError) && (
                            <Alert severity="error">
                                {validationError ??
                                    getErrorMessage(mutation.error)}
                            </Alert>
                        )}

                        <TextField
                            autoFocus
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'fullName',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'fullName',
                            )}
                            label="Full name"
                            onChange={(event) => {
                                setFullName(event.target.value)
                                setValidationError(null)
                            }}
                            required
                            value={fullName}
                        />

                        <TextField
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'email',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'email',
                            )}
                            label="Email"
                            onChange={(event) => {
                                setEmail(event.target.value)
                                setValidationError(null)
                            }}
                            required
                            type="email"
                            value={email}
                        />

                        <TextField
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'password',
                                ),
                            )}
                            helperText={
                                getFieldError(
                                    mutation.error,
                                    'password',
                                ) ??
                                '8–100 characters with uppercase, lowercase, number, special character, and no spaces.'
                            }
                            label="Initial password"
                            onChange={(event) => {
                                setPassword(event.target.value)
                                setValidationError(null)
                            }}
                            required
                            type="password"
                            value={password}
                        />

                        <FormControl
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'role',
                                ),
                            )}
                        >
                            <InputLabel id="create-user-role-label">
                                Role
                            </InputLabel>
                            <Select
                                label="Role"
                                labelId="create-user-role-label"
                                onChange={(event) => {
                                    setRole(
                                        event.target.value as TenantRole,
                                    )
                                }}
                                value={role}
                            >
                                {Object.entries(roleLabels).map(
                                    ([value, label]) => (
                                        <MenuItem
                                            key={value}
                                            value={value}
                                        >
                                            {label}
                                        </MenuItem>
                                    ),
                                )}
                            </Select>
                            <FormHelperText>
                                {getFieldError(
                                    mutation.error,
                                    'role',
                                )}
                            </FormHelperText>
                        </FormControl>
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Creating…'
                            : 'Create user'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function EditUserDialog({
    open,
    tenantId,
    user,
    onClose,
    onSuccess,
}: UserDialogProps) {
    const [fullName, setFullName] = useState(
        user?.fullName ?? '',
    )
    const [email, setEmail] = useState(
        user?.email ?? '',
    )
    const mutation = useUpdateTenantUser(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!user) {
            return
        }

        try {
            const updatedUser = await mutation.mutateAsync({
                userId: user.id,
                input: {
                    fullName: fullName.trim(),
                    email: email.trim().toLowerCase(),
                },
            })

            const currentSession = authStorage.read()

            if (
                currentSession?.tenantId === tenantId &&
                currentSession.userId === updatedUser.id
            ) {
                authStorage.write({
                    ...currentSession,
                    fullName: updatedUser.fullName,
                    email: updatedUser.email,
                })
            }

            onSuccess(
                `${updatedUser.fullName}'s profile was updated.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={closeDialog}
            open={open}
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Edit user profile</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ marginTop: 1 }}>
                        {mutation.isError && (
                            <Alert severity="error">
                                {getErrorMessage(mutation.error)}
                            </Alert>
                        )}

                        <TextField
                            autoFocus
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'fullName',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'fullName',
                            )}
                            label="Full name"
                            onChange={(event) => {
                                setFullName(event.target.value)
                            }}
                            required
                            value={fullName}
                        />

                        <TextField
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'email',
                                ),
                            )}
                            helperText={getFieldError(
                                mutation.error,
                                'email',
                            )}
                            label="Email"
                            onChange={(event) => {
                                setEmail(event.target.value)
                            }}
                            required
                            type="email"
                            value={email}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Save profile'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ChangeUserRoleDialog({
    open,
    tenantId,
    user,
    onClose,
    onSuccess,
}: UserDialogProps) {
    const [role, setRole] = useState<TenantRole>(
        user?.role ?? 'TENANT_USER',
    )
    const mutation = useUpdateTenantUserRole(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!user) {
            return
        }

        try {
            const updatedUser = await mutation.mutateAsync({
                userId: user.id,
                input: { role },
            })

            onSuccess(
                `${updatedUser.fullName}'s role is now ${roleLabels[updatedUser.role]}.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="xs"
            onClose={closeDialog}
            open={open}
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Change user role</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Choose the tenant role for {user?.fullName}.
                    </DialogContentText>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ marginBottom: 2 }}>
                            {getErrorMessage(mutation.error)}
                        </Alert>
                    )}

                    <FormControl fullWidth>
                        <InputLabel id="change-user-role-label">
                            Role
                        </InputLabel>
                        <Select
                            label="Role"
                            labelId="change-user-role-label"
                            onChange={(event) => {
                                setRole(
                                    event.target.value as TenantRole,
                                )
                            }}
                            value={role}
                        >
                            {Object.entries(roleLabels).map(
                                ([value, label]) => (
                                    <MenuItem key={value} value={value}>
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={
                            mutation.isPending || role === user?.role
                        }
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Change role'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ChangeUserStatusDialog({
    open,
    tenantId,
    user,
    onClose,
    onSuccess,
    activationAllowed = true,
    activationRestrictionMessage =
        'The current subscription does not allow reactivating this user.',
}: ChangeUserStatusDialogProps) {
    const [status, setStatus] = useState<UserStatus>(
        user?.status ?? 'ACTIVE',
    )
    const mutation = useUpdateTenantUserStatus(tenantId)
    const activationRestricted = Boolean(
        user?.status !== 'ACTIVE' &&
        status === 'ACTIVE' &&
        !activationAllowed,
    )

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!user) {
            return
        }

        try {
            const updatedUser = await mutation.mutateAsync({
                userId: user.id,
                input: { status },
            })

            onSuccess(
                `${updatedUser.fullName}'s status is now ${statusLabels[updatedUser.status]}.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog
            fullWidth
            maxWidth="xs"
            onClose={closeDialog}
            open={open}
        >
            <Box component="form" onSubmit={(event) => {
                void submit(event)
            }}>
                <DialogTitle>Change account status</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Inactive and suspended users cannot sign in. Their active refresh tokens are revoked.
                    </DialogContentText>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ marginBottom: 2 }}>
                            {getErrorMessage(mutation.error)}
                        </Alert>
                    )}

                    {user?.status !== 'ACTIVE' &&
                        !activationAllowed && (
                        <Alert
                            severity="warning"
                            sx={{ marginBottom: 2 }}
                        >
                            {activationRestrictionMessage}
                        </Alert>
                    )}

                    <FormControl fullWidth>
                        <InputLabel id="change-user-status-label">
                            Status
                        </InputLabel>
                        <Select
                            label="Status"
                            labelId="change-user-status-label"
                            onChange={(event) => {
                                setStatus(
                                    event.target.value as UserStatus,
                                )
                            }}
                            value={status}
                        >
                            {Object.entries(statusLabels).map(
                                ([value, label]) => (
                                    <MenuItem
                                        disabled={
                                            value === 'ACTIVE' &&
                                            user?.status !== 'ACTIVE' &&
                                            !activationAllowed
                                        }
                                        key={value}
                                        value={value}
                                    >
                                        {label}
                                    </MenuItem>
                                ),
                            )}
                        </Select>
                    </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeDialog}
                    >
                        Cancel
                    </Button>
                    <Button
                        disabled={
                            mutation.isPending ||
                            status === user?.status ||
                            activationRestricted
                        }
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Saving…'
                            : 'Change status'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function UnlockUserDialog({
    open,
    tenantId,
    user,
    onClose,
    onSuccess,
}: UserDialogProps) {
    const mutation = useUnlockTenantUserLogin(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const unlock = async (): Promise<void> => {
        if (!user) {
            return
        }

        try {
            const updatedUser = await mutation.mutateAsync(
                user.id,
            )

            onSuccess(
                `${updatedUser.fullName}'s login lock was cleared.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog onClose={closeDialog} open={open}>
            <DialogTitle>Unlock user login?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    This clears failed sign-in attempts for {user?.fullName}. It does not change the user&apos;s password or account status.
                </DialogContentText>

                {mutation.isError && (
                    <Alert severity="error" sx={{ marginTop: 2 }}>
                        {getErrorMessage(mutation.error)}
                    </Alert>
                )}
            </DialogContent>
            <DialogActions>
                <Button
                    disabled={mutation.isPending}
                    onClick={closeDialog}
                >
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    onClick={() => {
                        void unlock()
                    }}
                    variant="contained"
                >
                    {mutation.isPending
                        ? 'Unlocking…'
                        : 'Unlock login'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
