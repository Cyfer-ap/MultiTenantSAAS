import {
    Alert,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import { ApiClientError } from '../../../api/apiError'
import {
    useCreateSystemAdmin,
    useUnlockSystemAdminLogin,
    useUpdateSystemAdminStatus,
} from '../hooks/useSystemAdmins'
import type {
    SystemAdminRecord,
    SystemAdminStatus,
} from '../types/systemAdmin'

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/

const statusLabels: Record<SystemAdminStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function getFieldError(
    error: unknown,
    field: string,
): string | undefined {
    const detail = error instanceof ApiClientError
        ? error.details?.[field]
        : undefined

    return typeof detail === 'string'
        ? detail
        : undefined
}

export function CreateSystemAdminDialog({
    onClose,
    onSuccess,
    open,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
}) {
    const mutation = useCreateSystemAdmin()
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)

    const close = (): void => {
        if (!mutation.isPending) onClose()
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()
        const normalizedName = fullName.trim()
        const normalizedEmail = email.trim().toLowerCase()

        if (normalizedName.length < 2 || normalizedName.length > 150) {
            setValidationError(
                'Full name must be between 2 and 150 characters.',
            )
            return
        }

        if (
            normalizedEmail.length > 150 ||
            !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)
        ) {
            setValidationError('Enter a valid email address.')
            return
        }

        if (!passwordPattern.test(password)) {
            setValidationError(
                'Password must be 8–100 characters and contain uppercase, lowercase, number, and special characters with no spaces.',
            )
            return
        }

        if (password !== confirmPassword) {
            setValidationError('Passwords do not match.')
            return
        }

        setValidationError(null)

        try {
            const created = await mutation.mutateAsync({
                fullName: normalizedName,
                email: normalizedEmail,
                password,
            })
            onSuccess(`${created.email} was added as a system administrator.`)
            onClose()
        }
        catch {
            // Mutation errors are displayed in the dialog.
        }
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={close} open={open}>
            <DialogTitle>Create system administrator</DialogTitle>
            <DialogContent>
                <Stack
                    component="form"
                    id="create-system-admin-form"
                    onSubmit={(event) => { void submit(event) }}
                    spacing={2}
                    sx={{ pt: 1 }}
                >
                    {(validationError || mutation.isError) && (
                        <Alert severity="error">
                            {validationError ?? getErrorMessage(
                                mutation.error,
                                'The system administrator could not be created.',
                            )}
                        </Alert>
                    )}
                    <TextField
                        autoFocus
                        disabled={mutation.isPending}
                        error={Boolean(getFieldError(mutation.error, 'fullName'))}
                        helperText={getFieldError(mutation.error, 'fullName')}
                        label="Full name"
                        onChange={(event) => { setFullName(event.target.value); setValidationError(null) }}
                        value={fullName}
                    />
                    <TextField
                        disabled={mutation.isPending}
                        error={Boolean(getFieldError(mutation.error, 'email'))}
                        helperText={getFieldError(mutation.error, 'email')}
                        label="Email address"
                        onChange={(event) => { setEmail(event.target.value); setValidationError(null) }}
                        type="email"
                        value={email}
                    />
                    <TextField
                        disabled={mutation.isPending}
                        error={Boolean(getFieldError(mutation.error, 'password'))}
                        helperText={getFieldError(mutation.error, 'password')}
                        label="Temporary password"
                        onChange={(event) => { setPassword(event.target.value); setValidationError(null) }}
                        type="password"
                        value={password}
                    />
                    <TextField
                        disabled={mutation.isPending}
                        label="Confirm temporary password"
                        onChange={(event) => { setConfirmPassword(event.target.value); setValidationError(null) }}
                        type="password"
                        value={confirmPassword}
                    />
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={close}>
                    Cancel
                </Button>
                <Button
                    disabled={mutation.isPending}
                    form="create-system-admin-form"
                    type="submit"
                    variant="contained"
                >
                    {mutation.isPending ? 'Creating…' : 'Create administrator'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function ChangeSystemAdminStatusDialog({
    currentSystemAdminId,
    onClose,
    onSuccess,
    open,
    systemAdmin,
}: {
    currentSystemAdminId: string
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    systemAdmin: SystemAdminRecord
}) {
    const mutation = useUpdateSystemAdminStatus()
    const [status, setStatus] = useState<SystemAdminStatus>(
        systemAdmin.status,
    )
    const isCurrentAccount = systemAdmin.id === currentSystemAdminId

    const submit = async (): Promise<void> => {
        if (status === systemAdmin.status) return

        try {
            await mutation.mutateAsync({
                systemAdminId: systemAdmin.id,
                input: { status },
            })
            onSuccess(
                `${systemAdmin.email} is now ${statusLabels[status].toLowerCase()}.`,
            )
            onClose()
        }
        catch {
            // Mutation errors are displayed in the dialog.
        }
    }

    return (
        <Dialog fullWidth maxWidth="xs" onClose={onClose} open={open}>
            <DialogTitle>Change administrator status</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <Typography color="text.secondary">
                        Change platform access for {systemAdmin.email}.
                    </Typography>
                    {isCurrentAccount && (
                        <Alert severity="info">
                            Your own system-administrator account must remain active.
                        </Alert>
                    )}
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The administrator status could not be updated.',
                            )}
                        </Alert>
                    )}
                    <FormControl fullWidth>
                        <InputLabel id="system-admin-status-label">Status</InputLabel>
                        <Select
                            disabled={mutation.isPending}
                            label="Status"
                            labelId="system-admin-status-label"
                            onChange={(event) => { setStatus(event.target.value as SystemAdminStatus) }}
                            value={status}
                        >
                            {(Object.keys(statusLabels) as SystemAdminStatus[]).map((value) => (
                                <MenuItem
                                    disabled={isCurrentAccount && value !== 'ACTIVE'}
                                    key={value}
                                    value={value}
                                >
                                    {statusLabels[value]}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {status !== 'ACTIVE' && (
                        <Alert severity="warning">
                            At least one active system administrator must remain.
                        </Alert>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={onClose}>Cancel</Button>
                <Button
                    disabled={
                        mutation.isPending ||
                        status === systemAdmin.status ||
                        (isCurrentAccount && status !== 'ACTIVE')
                    }
                    onClick={() => { void submit() }}
                    variant="contained"
                >
                    {mutation.isPending ? 'Saving…' : 'Save status'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function UnlockSystemAdminDialog({
    onClose,
    onSuccess,
    open,
    systemAdmin,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    systemAdmin: SystemAdminRecord
}) {
    const mutation = useUnlockSystemAdminLogin()

    const unlock = async (): Promise<void> => {
        try {
            await mutation.mutateAsync(systemAdmin.id)
            onSuccess(`Login access was unlocked for ${systemAdmin.email}.`)
            onClose()
        }
        catch {
            // Mutation errors are displayed in the dialog.
        }
    }

    return (
        <Dialog fullWidth maxWidth="xs" onClose={onClose} open={open}>
            <DialogTitle>Unlock administrator login</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <Typography color="text.secondary">
                        Reset failed login attempts and remove the current login lock for {systemAdmin.email}?
                    </Typography>
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The administrator login could not be unlocked.',
                            )}
                        </Alert>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={onClose}>Cancel</Button>
                <Button
                    disabled={mutation.isPending}
                    onClick={() => { void unlock() }}
                    variant="contained"
                >
                    {mutation.isPending ? 'Unlocking…' : 'Unlock login'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
