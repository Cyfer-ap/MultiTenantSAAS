import PasswordRoundedIcon from '@mui/icons-material/PasswordRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Container,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router'

import { ApiClientError } from '../api/apiError'
import { useResetPassword } from '../features/password-reset/hooks/usePasswordReset'

const passwordPattern = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/

function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'The password could not be reset.'
}

function getFieldError(error: unknown, field: string): string | undefined {
    const detail = error instanceof ApiClientError ? error.details?.[field] : undefined

    return typeof detail === 'string' ? detail : undefined
}

export function ResetPasswordPage() {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [validationError, setValidationError] = useState<string | null>(null)
    const mutation = useResetPassword()
    const resetToken = searchParams.get('token')?.trim() ?? ''

    const submit = async (event: FormEvent<HTMLFormElement>): Promise<void> => {
        event.preventDefault()

        if (!resetToken) {
            setValidationError('This reset link does not contain a token.')
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
            await mutation.mutateAsync({
                resetToken,
                newPassword: password,
                confirmPassword,
            })
        } catch {
            // The mutation error is rendered on the page.
        }
    }

    return (
        <Box
            sx={{
                alignItems: 'center',
                backgroundColor: 'grey.100',
                display: 'flex',
                minHeight: '100vh',
                py: 4,
            }}
        >
            <Container maxWidth="xs">
                <Paper
                    elevation={4}
                    sx={{
                        borderRadius: 3,
                        p: {
                            xs: 3,
                            sm: 4,
                        },
                    }}
                >
                    <Stack spacing={3} sx={{ alignItems: 'center' }}>
                        <Avatar
                            sx={{
                                backgroundColor: 'primary.main',
                                height: 48,
                                width: 48,
                            }}
                        >
                            <PasswordRoundedIcon />
                        </Avatar>

                        {mutation.isSuccess ? (
                            <>
                                <Box sx={{ textAlign: 'center' }}>
                                    <Typography component="h1" variant="h4">
                                        Password updated
                                    </Typography>
                                    <Typography color="text.secondary" sx={{ marginTop: 1 }}>
                                        Your previous sessions have been revoked.
                                    </Typography>
                                </Box>
                                <Alert severity="success" sx={{ width: '100%' }}>
                                    {mutation.data.message}
                                </Alert>
                                <Button
                                    fullWidth
                                    onClick={() => {
                                        navigate('/login', {
                                            replace: true,
                                        })
                                    }}
                                    size="large"
                                    variant="contained"
                                >
                                    Continue to sign in
                                </Button>
                            </>
                        ) : (
                            <>
                                <Box sx={{ textAlign: 'center' }}>
                                    <Typography component="h1" variant="h4">
                                        Reset password
                                    </Typography>
                                    <Typography color="text.secondary" sx={{ marginTop: 1 }}>
                                        Choose a new password for your account.
                                    </Typography>
                                </Box>

                                {!resetToken && (
                                    <Alert severity="error" sx={{ width: '100%' }}>
                                        This reset link is incomplete. Request a new password reset
                                        link.
                                    </Alert>
                                )}

                                <Box
                                    component="form"
                                    noValidate
                                    onSubmit={(event) => {
                                        void submit(event)
                                    }}
                                    sx={{ width: '100%' }}
                                >
                                    <Stack spacing={2}>
                                        {(validationError || mutation.isError) && (
                                            <Alert severity="error">
                                                {validationError ?? getErrorMessage(mutation.error)}
                                            </Alert>
                                        )}
                                        <TextField
                                            autoComplete="new-password"
                                            autoFocus
                                            disabled={mutation.isPending || !resetToken}
                                            error={Boolean(
                                                getFieldError(mutation.error, 'newPassword'),
                                            )}
                                            helperText={
                                                getFieldError(mutation.error, 'newPassword') ??
                                                '8–100 characters with uppercase, lowercase, number, special character, and no spaces.'
                                            }
                                            label="New password"
                                            onChange={(event) => {
                                                setPassword(event.target.value)
                                                setValidationError(null)
                                            }}
                                            required
                                            type="password"
                                            value={password}
                                        />
                                        <TextField
                                            autoComplete="new-password"
                                            disabled={mutation.isPending || !resetToken}
                                            error={Boolean(
                                                getFieldError(mutation.error, 'confirmPassword'),
                                            )}
                                            helperText={getFieldError(
                                                mutation.error,
                                                'confirmPassword',
                                            )}
                                            label="Confirm password"
                                            onChange={(event) => {
                                                setConfirmPassword(event.target.value)
                                                setValidationError(null)
                                            }}
                                            required
                                            type="password"
                                            value={confirmPassword}
                                        />
                                        <Button
                                            disabled={mutation.isPending || !resetToken}
                                            size="large"
                                            type="submit"
                                            variant="contained"
                                        >
                                            {mutation.isPending
                                                ? 'Resetting password…'
                                                : 'Reset password'}
                                        </Button>
                                        <Button
                                            disabled={mutation.isPending}
                                            onClick={() => {
                                                navigate('/forgot-password')
                                            }}
                                        >
                                            Request a new link
                                        </Button>
                                    </Stack>
                                </Box>
                            </>
                        )}
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
