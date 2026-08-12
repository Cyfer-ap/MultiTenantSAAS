import HowToRegRoundedIcon from '@mui/icons-material/HowToRegRounded'
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
import {
    useNavigate,
    useSearchParams,
} from 'react-router'

import { ApiClientError } from '../api/apiError'
import { useAcceptInvitation } from '../features/invitations/hooks/useTenantInvitations'

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The invitation could not be accepted.'
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

export function AcceptInvitationPage() {
    const navigate = useNavigate()
    const [searchParams] = useSearchParams()
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] =
        useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const mutation = useAcceptInvitation()
    const invitationToken =
        searchParams.get('token')?.trim() ?? ''

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        if (!invitationToken) {
            setValidationError(
                'This invitation link does not contain a token.',
            )
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
                invitationToken,
                newPassword: password,
                confirmPassword,
            })
        }
        catch {
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
                            <HowToRegRoundedIcon />
                        </Avatar>

                        {mutation.isSuccess ? (
                            <>
                                <Box sx={{ textAlign: 'center' }}>
                                    <Typography
                                        component="h1"
                                        variant="h4"
                                    >
                                        Welcome, {mutation.data.user.fullName}
                                    </Typography>
                                    <Typography
                                        color="text.secondary"
                                        sx={{ marginTop: 1 }}
                                    >
                                        Your account is active and ready to use.
                                    </Typography>
                                </Box>

                                <Alert severity="success" sx={{ width: '100%' }}>
                                    Invitation accepted successfully.
                                </Alert>

                                <Stack spacing={0.5} sx={{ width: '100%' }}>
                                    <Typography color="text.secondary" variant="caption">
                                        Tenant ID
                                    </Typography>
                                    <Typography sx={{ overflowWrap: 'anywhere' }}>
                                        {mutation.data.user.tenantId}
                                    </Typography>
                                    <Typography
                                        color="text.secondary"
                                        sx={{ paddingTop: 1 }}
                                        variant="caption"
                                    >
                                        Email
                                    </Typography>
                                    <Typography>
                                        {mutation.data.user.email}
                                    </Typography>
                                </Stack>

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
                                    <Typography
                                        component="h1"
                                        variant="h4"
                                    >
                                        Join workspace
                                    </Typography>
                                    <Typography
                                        color="text.secondary"
                                        sx={{ marginTop: 1 }}
                                    >
                                        Set a secure password to activate your account.
                                    </Typography>
                                </Box>

                                {!invitationToken && (
                                    <Alert severity="error" sx={{ width: '100%' }}>
                                        This invitation link is incomplete. Ask your
                                        tenant administrator for a new invitation.
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
                                                {validationError ??
                                                    getErrorMessage(mutation.error)}
                                            </Alert>
                                        )}

                                        <TextField
                                            autoComplete="new-password"
                                            autoFocus
                                            disabled={
                                                mutation.isPending ||
                                                !invitationToken
                                            }
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'newPassword',
                                                ),
                                            )}
                                            helperText={
                                                getFieldError(
                                                    mutation.error,
                                                    'newPassword',
                                                ) ??
                                                '8–100 characters with uppercase, lowercase, number, special character, and no spaces.'
                                            }
                                            label="Password"
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
                                            disabled={
                                                mutation.isPending ||
                                                !invitationToken
                                            }
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'confirmPassword',
                                                ),
                                            )}
                                            helperText={getFieldError(
                                                mutation.error,
                                                'confirmPassword',
                                            )}
                                            label="Confirm password"
                                            onChange={(event) => {
                                                setConfirmPassword(
                                                    event.target.value,
                                                )
                                                setValidationError(null)
                                            }}
                                            required
                                            type="password"
                                            value={confirmPassword}
                                        />

                                        <Button
                                            disabled={
                                                mutation.isPending ||
                                                !invitationToken
                                            }
                                            fullWidth
                                            size="large"
                                            type="submit"
                                            variant="contained"
                                        >
                                            {mutation.isPending
                                                ? 'Activating account…'
                                                : 'Accept invitation'}
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
