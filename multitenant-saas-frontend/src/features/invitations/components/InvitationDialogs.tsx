import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
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
import type { TenantRole } from '../../auth/types/auth'
import {
    useCreateInvitation,
    useRevokeInvitation,
} from '../hooks/useTenantInvitations'
import type { TenantInvitation } from '../types/invitations'

interface DialogBaseProps {
    open: boolean
    tenantId: string
    onClose: () => void
}

interface RevokeInvitationDialogProps
    extends DialogBaseProps {
    invitation: TenantInvitation
    onSuccess: (message: string) => void
}

const roleLabels: Record<TenantRole, string> = {
    TENANT_ADMIN: 'Administrator',
    TENANT_MANAGER: 'Manager',
    TENANT_USER: 'User',
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The invitation operation could not be completed.'
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

export function CreateInvitationDialog({
    open,
    tenantId,
    onClose,
}: DialogBaseProps) {
    const [fullName, setFullName] = useState('')
    const [email, setEmail] = useState('')
    const [role, setRole] =
        useState<TenantRole>('TENANT_USER')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const [copyFeedback, setCopyFeedback] =
        useState<string | null>(null)
    const mutation = useCreateInvitation(tenantId)

    const acceptanceLink = mutation.data?.devInvitationToken
        ? `${window.location.origin}/accept-invitation?token=${encodeURIComponent(mutation.data.devInvitationToken)}`
        : null

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

        setValidationError(null)

        try {
            await mutation.mutateAsync({
                fullName: normalizedName,
                email: normalizedEmail,
                role,
            })
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    const copyAcceptanceLink = async (): Promise<void> => {
        if (!acceptanceLink) {
            return
        }

        try {
            await navigator.clipboard.writeText(acceptanceLink)
            setCopyFeedback('Invitation link copied.')
        }
        catch {
            setCopyFeedback(
                'Copy failed. Select and copy the link manually.',
            )
        }
    }

    if (mutation.isSuccess) {
        return (
            <Dialog
                fullWidth
                maxWidth="sm"
                onClose={closeDialog}
                open={open}
            >
                <DialogTitle>Invitation ready</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{ marginTop: 1 }}>
                        <Alert severity="success">
                            {mutation.data.fullName} can now join the workspace.
                        </Alert>

                        <DialogContentText>
                            Send this one-time link to {mutation.data.email}.
                            It is available only after creation in the current
                            development environment.
                        </DialogContentText>

                        {acceptanceLink ? (
                            <TextField
                                fullWidth
                                label="Invitation link"
                                slotProps={{
                                    htmlInput: {
                                        readOnly: true,
                                    },
                                }}
                                value={acceptanceLink}
                            />
                        ) : (
                            <Alert severity="info">
                                The invitation was created. Delivery is handled
                                by the configured email service.
                            </Alert>
                        )}

                        {copyFeedback && (
                            <Alert severity="info">
                                {copyFeedback}
                            </Alert>
                        )}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    {acceptanceLink && (
                        <Button
                            onClick={() => {
                                void copyAcceptanceLink()
                            }}
                            startIcon={<ContentCopyRoundedIcon />}
                        >
                            Copy link
                        </Button>
                    )}
                    <Button onClick={closeDialog} variant="contained">
                        Done
                    </Button>
                </DialogActions>
            </Dialog>
        )
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
                <DialogTitle>Invite tenant user</DialogTitle>
                <DialogContent>
                    <DialogContentText sx={{ marginBottom: 2 }}>
                        Invite a person to set their password and join this
                        tenant with the selected role.
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

                        <FormControl
                            error={Boolean(
                                getFieldError(
                                    mutation.error,
                                    'role',
                                ),
                            )}
                        >
                            <InputLabel id="invitation-role-label">
                                Role
                            </InputLabel>
                            <Select
                                label="Role"
                                labelId="invitation-role-label"
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
                            : 'Create invitation'}
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function RevokeInvitationDialog({
    open,
    tenantId,
    invitation,
    onClose,
    onSuccess,
}: RevokeInvitationDialogProps) {
    const mutation = useRevokeInvitation(tenantId)

    const closeDialog = (): void => {
        if (!mutation.isPending) {
            onClose()
        }
    }

    const revoke = async (): Promise<void> => {
        try {
            await mutation.mutateAsync(
                invitation.invitationId,
            )
            onSuccess(
                `Invitation for ${invitation.email} was revoked.`,
            )
            onClose()
        }
        catch {
            // The mutation error is rendered in the dialog.
        }
    }

    return (
        <Dialog onClose={closeDialog} open={open}>
            <DialogTitle>Revoke invitation?</DialogTitle>
            <DialogContent>
                <DialogContentText>
                    {invitation.fullName} will no longer be able to use this
                    invitation to join the tenant.
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
                    Keep invitation
                </Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={() => {
                        void revoke()
                    }}
                    variant="contained"
                >
                    {mutation.isPending
                        ? 'Revoking…'
                        : 'Revoke invitation'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}
