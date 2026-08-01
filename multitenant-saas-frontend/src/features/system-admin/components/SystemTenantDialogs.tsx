import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
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
import type { TenantStatus } from '../../onboarding/types/onboarding'
import {
    useSystemTenantOnboarding,
    useUpdateSystemTenantStatus,
} from '../hooks/useSystemTenants'
import type { SystemTenant } from '../types/systemAdmin'

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/
const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

const statusLabels: Record<TenantStatus, string> = {
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
    return error instanceof ApiClientError
        ? error.details?.[field]
        : undefined
}

export function OnboardSystemTenantDialog({
    onClose,
    onSuccess,
    open,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
}) {
    const mutation = useSystemTenantOnboarding()
    const [tenantName, setTenantName] = useState('')
    const [tenantSlug, setTenantSlug] = useState('')
    const [adminFullName, setAdminFullName] = useState('')
    const [adminEmail, setAdminEmail] = useState('')
    const [adminPassword, setAdminPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)

    const close = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        const normalizedTenantName = tenantName.trim()
        const normalizedTenantSlug = tenantSlug.trim().toLowerCase()
        const normalizedAdminName = adminFullName.trim()
        const normalizedAdminEmail = adminEmail.trim().toLowerCase()

        if (
            normalizedTenantName.length < 2 ||
            normalizedTenantName.length > 100
        ) {
            setValidationError(
                'Workspace name must be between 2 and 100 characters.',
            )
            return
        }

        if (
            normalizedTenantSlug.length < 2 ||
            normalizedTenantSlug.length > 80 ||
            !slugPattern.test(normalizedTenantSlug)
        ) {
            setValidationError(
                'Workspace slug must be 2–80 lowercase letters, numbers, or single hyphens.',
            )
            return
        }

        if (
            normalizedAdminName.length < 2 ||
            normalizedAdminName.length > 100
        ) {
            setValidationError(
                'Administrator name must be between 2 and 100 characters.',
            )
            return
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedAdminEmail)) {
            setValidationError(
                'Enter a valid administrator email address.',
            )
            return
        }

        if (!passwordPattern.test(adminPassword)) {
            setValidationError(
                'Password must be 8–100 characters and contain uppercase, lowercase, number, and special characters with no spaces.',
            )
            return
        }

        if (adminPassword !== confirmPassword) {
            setValidationError('Passwords do not match.')
            return
        }

        setValidationError(null)

        try {
            const response = await mutation.mutateAsync({
                tenantName: normalizedTenantName,
                tenantSlug: normalizedTenantSlug,
                adminFullName: normalizedAdminName,
                adminEmail: normalizedAdminEmail,
                adminPassword,
            })
            onSuccess(response.message)
        }
        catch {
            // Mutation errors are rendered inside the dialog.
        }
    }

    return (
        <Dialog fullWidth maxWidth="sm" onClose={close} open={open}>
            <DialogTitle>Onboard tenant</DialogTitle>
            <DialogContent>
                {mutation.isSuccess ? (
                    <Stack spacing={2} sx={{ pt: 1 }}>
                        <Alert severity="success">
                            {mutation.data.message}
                        </Alert>
                        <Stack divider={<Divider flexItem />} spacing={1.5}>
                            <Box>
                                <Typography color="text.secondary" variant="caption">Workspace</Typography>
                                <Typography>{mutation.data.tenant.name}</Typography>
                            </Box>
                            <Box>
                                <Typography color="text.secondary" variant="caption">Tenant ID</Typography>
                                <Typography sx={{ overflowWrap: 'anywhere' }}>{mutation.data.tenant.id}</Typography>
                            </Box>
                            <Box>
                                <Typography color="text.secondary" variant="caption">Initial administrator</Typography>
                                <Typography>{mutation.data.adminUser.email}</Typography>
                            </Box>
                        </Stack>
                    </Stack>
                ) : (
                    <Stack
                        component="form"
                        id="system-tenant-onboarding-form"
                        onSubmit={(event) => { void submit(event) }}
                        spacing={2}
                        sx={{ pt: 1 }}
                    >
                        {(validationError || mutation.isError) && (
                            <Alert severity="error">
                                {validationError ?? getErrorMessage(
                                    mutation.error,
                                    'The tenant could not be onboarded.',
                                )}
                            </Alert>
                        )}
                        <TextField
                            autoFocus
                            disabled={mutation.isPending}
                            error={Boolean(getFieldError(mutation.error, 'tenantName'))}
                            helperText={getFieldError(mutation.error, 'tenantName')}
                            label="Workspace name"
                            onChange={(event) => { setTenantName(event.target.value); setValidationError(null) }}
                            value={tenantName}
                        />
                        <TextField
                            disabled={mutation.isPending}
                            error={Boolean(getFieldError(mutation.error, 'tenantSlug'))}
                            helperText={getFieldError(mutation.error, 'tenantSlug') ?? 'Used in URLs and tenant identification.'}
                            label="Workspace slug"
                            onChange={(event) => { setTenantSlug(event.target.value); setValidationError(null) }}
                            value={tenantSlug}
                        />
                        <Divider>Initial tenant administrator</Divider>
                        <TextField
                            disabled={mutation.isPending}
                            error={Boolean(getFieldError(mutation.error, 'adminFullName'))}
                            helperText={getFieldError(mutation.error, 'adminFullName')}
                            label="Administrator full name"
                            onChange={(event) => { setAdminFullName(event.target.value); setValidationError(null) }}
                            value={adminFullName}
                        />
                        <TextField
                            disabled={mutation.isPending}
                            error={Boolean(getFieldError(mutation.error, 'adminEmail'))}
                            helperText={getFieldError(mutation.error, 'adminEmail')}
                            label="Administrator email address"
                            onChange={(event) => { setAdminEmail(event.target.value); setValidationError(null) }}
                            type="email"
                            value={adminEmail}
                        />
                        <TextField
                            disabled={mutation.isPending}
                            error={Boolean(getFieldError(mutation.error, 'adminPassword'))}
                            helperText={getFieldError(mutation.error, 'adminPassword')}
                            label="Administrator password"
                            onChange={(event) => { setAdminPassword(event.target.value); setValidationError(null) }}
                            type="password"
                            value={adminPassword}
                        />
                        <TextField
                            disabled={mutation.isPending}
                            label="Confirm administrator password"
                            onChange={(event) => { setConfirmPassword(event.target.value); setValidationError(null) }}
                            type="password"
                            value={confirmPassword}
                        />
                    </Stack>
                )}
            </DialogContent>
            <DialogActions>
                <Button onClick={close}>
                    {mutation.isSuccess ? 'Done' : 'Cancel'}
                </Button>
                {!mutation.isSuccess && (
                    <Button
                        disabled={mutation.isPending}
                        form="system-tenant-onboarding-form"
                        type="submit"
                        variant="contained"
                    >
                        {mutation.isPending ? 'Onboarding…' : 'Onboard tenant'}
                    </Button>
                )}
            </DialogActions>
        </Dialog>
    )
}

export function ChangeSystemTenantStatusDialog({
    onClose,
    onSuccess,
    open,
    tenant,
}: {
    onClose: () => void
    onSuccess: (message: string) => void
    open: boolean
    tenant: SystemTenant | null
}) {
    const mutation = useUpdateSystemTenantStatus()
    const [status, setStatus] = useState<TenantStatus>(
        tenant?.status ?? 'ACTIVE',
    )

    const submit = async (): Promise<void> => {
        if (!tenant || status === tenant.status) {
            return
        }

        try {
            await mutation.mutateAsync({
                tenantId: tenant.id,
                input: { status },
            })
            onSuccess(
                tenant.name + ' is now ' + statusLabels[status].toLowerCase() + '.',
            )
            onClose()
        }
        catch {
            // Mutation errors are rendered inside the dialog.
        }
    }

    return (
        <Dialog fullWidth maxWidth="xs" onClose={onClose} open={open}>
            <DialogTitle>Change tenant status</DialogTitle>
            <DialogContent>
                <Stack spacing={2} sx={{ pt: 1 }}>
                    <Typography color="text.secondary">
                        Update the platform access state for {tenant?.name}.
                        Non-active tenants cannot authenticate.
                    </Typography>
                    {mutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                mutation.error,
                                'The tenant status could not be updated.',
                            )}
                        </Alert>
                    )}
                    <FormControl fullWidth>
                        <InputLabel id="system-tenant-status-label">Status</InputLabel>
                        <Select
                            disabled={mutation.isPending}
                            label="Status"
                            labelId="system-tenant-status-label"
                            onChange={(event) => { setStatus(event.target.value as TenantStatus) }}
                            value={status}
                        >
                            {(Object.keys(statusLabels) as TenantStatus[]).map((value) => (
                                <MenuItem key={value} value={value}>
                                    {statusLabels[value]}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {status !== 'ACTIVE' && (
                        <Alert severity="warning">
                            Existing tenant refresh sessions will be revoked.
                        </Alert>
                    )}
                </Stack>
            </DialogContent>
            <DialogActions>
                <Button disabled={mutation.isPending} onClick={onClose}>Cancel</Button>
                <Button
                    disabled={mutation.isPending || !tenant || status === tenant.status}
                    onClick={() => { void submit() }}
                    variant="contained"
                >
                    {mutation.isPending ? 'Saving…' : 'Save status'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
