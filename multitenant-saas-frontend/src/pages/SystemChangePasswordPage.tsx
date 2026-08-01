import LockResetRoundedIcon from '@mui/icons-material/LockResetRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { ApiClientError } from '../api/apiError'
import { useChangeSystemAdminPassword } from '../features/system-admin/hooks/useSystemAdminPassword'

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/

function getFieldError(
    error: unknown,
    field: string,
): string | undefined {
    return error instanceof ApiClientError
        ? error.details?.[field]
        : undefined
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The system administrator password could not be changed.'
}

export function SystemChangePasswordPage() {
    const navigate = useNavigate()
    const mutation = useChangeSystemAdminPassword()
    const [currentPassword, setCurrentPassword] = useState('')
    const [newPassword, setNewPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const [successMessage, setSuccessMessage] =
        useState<string | null>(null)

    const clearFeedback = (): void => {
        setValidationError(null)
        setSuccessMessage(null)
        mutation.reset()
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!currentPassword) {
            setValidationError('Current password is required.')
            return
        }

        if (!passwordPattern.test(newPassword)) {
            setValidationError(
                'New password must be 8–100 characters and contain uppercase, lowercase, number, and special characters with no spaces.',
            )
            return
        }

        if (newPassword !== confirmPassword) {
            setValidationError('New password and confirmation do not match.')
            return
        }

        if (currentPassword === newPassword) {
            setValidationError(
                'New password must be different from the current password.',
            )
            return
        }

        setValidationError(null)
        setSuccessMessage(null)

        try {
            await mutation.mutateAsync({
                currentPassword,
                newPassword,
                confirmPassword,
            })
            setCurrentPassword('')
            setNewPassword('')
            setConfirmPassword('')
            setSuccessMessage(
                'Password changed successfully. Use the new password the next time you sign in.',
            )
        }
        catch {
            // The mutation error is rendered below.
        }
    }

    return (
        <Box>
            <Box>
                <Typography component="h1" variant="h4">
                    Change password
                </Typography>
                <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                    Update the password for your system-administrator account.
                </Typography>
            </Box>

            <Card sx={{ maxWidth: 680, mt: 3 }} variant="outlined">
                <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                    <Stack spacing={3}>
                        <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                            <Box
                                sx={{
                                    alignItems: 'center',
                                    bgcolor: 'primary.main',
                                    borderRadius: 2,
                                    color: 'primary.contrastText',
                                    display: 'flex',
                                    height: 44,
                                    justifyContent: 'center',
                                    width: 44,
                                }}
                            >
                                <LockResetRoundedIcon />
                            </Box>
                            <Box>
                                <Typography sx={{ fontWeight: 700 }}>
                                    Account security
                                </Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Enter your current password before choosing a new one.
                                </Typography>
                            </Box>
                        </Stack>

                        <Alert severity="info">
                            Your current system-console session remains active after this change.
                        </Alert>

                        {successMessage && (
                            <Alert severity="success">
                                {successMessage}
                            </Alert>
                        )}

                        {(validationError || mutation.isError) && (
                            <Alert severity="error">
                                {validationError ?? getErrorMessage(mutation.error)}
                            </Alert>
                        )}

                        <Stack
                            component="form"
                            onSubmit={(event) => { void submit(event) }}
                            spacing={2}
                        >
                            <TextField
                                autoComplete="current-password"
                                autoFocus
                                disabled={mutation.isPending}
                                error={Boolean(getFieldError(
                                    mutation.error,
                                    'currentPassword',
                                ))}
                                helperText={getFieldError(
                                    mutation.error,
                                    'currentPassword',
                                )}
                                label="Current password"
                                onChange={(event) => {
                                    setCurrentPassword(event.target.value)
                                    clearFeedback()
                                }}
                                required
                                type="password"
                                value={currentPassword}
                            />

                            <TextField
                                autoComplete="new-password"
                                disabled={mutation.isPending}
                                error={Boolean(getFieldError(
                                    mutation.error,
                                    'newPassword',
                                ))}
                                helperText={getFieldError(
                                    mutation.error,
                                    'newPassword',
                                ) ?? '8–100 characters with uppercase, lowercase, number, special character, and no spaces.'}
                                label="New password"
                                onChange={(event) => {
                                    setNewPassword(event.target.value)
                                    clearFeedback()
                                }}
                                required
                                type="password"
                                value={newPassword}
                            />

                            <TextField
                                autoComplete="new-password"
                                disabled={mutation.isPending}
                                error={Boolean(getFieldError(
                                    mutation.error,
                                    'confirmPassword',
                                ))}
                                helperText={getFieldError(
                                    mutation.error,
                                    'confirmPassword',
                                )}
                                label="Confirm new password"
                                onChange={(event) => {
                                    setConfirmPassword(event.target.value)
                                    clearFeedback()
                                }}
                                required
                                type="password"
                                value={confirmPassword}
                            />

                            <Stack
                                direction={{ xs: 'column-reverse', sm: 'row' }}
                                spacing={1.5}
                                sx={{ justifyContent: 'flex-end', pt: 1 }}
                            >
                                <Button
                                    disabled={mutation.isPending}
                                    onClick={() => { navigate('/system/dashboard') }}
                                    type="button"
                                >
                                    Cancel
                                </Button>
                                <Button
                                    disabled={mutation.isPending}
                                    type="submit"
                                    variant="contained"
                                >
                                    {mutation.isPending
                                        ? 'Changing password…'
                                        : 'Change password'}
                                </Button>
                            </Stack>
                        </Stack>
                    </Stack>
                </CardContent>
            </Card>
        </Box>
    )
}
