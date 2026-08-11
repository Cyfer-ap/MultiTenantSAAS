import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import KeyRoundedIcon from '@mui/icons-material/KeyRounded'
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
import { useNavigate } from 'react-router'

import { ApiClientError } from '../api/apiError'
import { useForgotPassword } from '../features/password-reset/hooks/usePasswordReset'

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The password reset request could not be completed.'
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

export function ForgotPasswordPage() {
    const navigate = useNavigate()
    const [tenantId, setTenantId] = useState('')
    const [email, setEmail] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)
    const [copyFeedback, setCopyFeedback] =
        useState<string | null>(null)
    const mutation = useForgotPassword()

    const resetLink = mutation.data?.devResetToken
        ? `${window.location.origin}/reset-password?token=${encodeURIComponent(mutation.data.devResetToken)}`
        : null

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        const normalizedTenantId = tenantId.trim()
        const normalizedEmail = email.trim().toLowerCase()

        if (!normalizedTenantId) {
            setValidationError('Tenant ID is required.')
            return
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
            setValidationError('Enter a valid email address.')
            return
        }

        setValidationError(null)
        setCopyFeedback(null)

        try {
            await mutation.mutateAsync({
                tenantId: normalizedTenantId,
                email: normalizedEmail,
            })
        }
        catch {
            // The mutation error is rendered on the page.
        }
    }

    const copyResetLink = async (): Promise<void> => {
        if (!resetLink) {
            return
        }

        try {
            await navigator.clipboard.writeText(resetLink)
            setCopyFeedback('Reset link copied.')
        }
        catch {
            setCopyFeedback(
                'Copy failed. Select and copy the link manually.',
            )
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
                            <KeyRoundedIcon />
                        </Avatar>

                        <Box sx={{ textAlign: 'center' }}>
                            <Typography component="h1" variant="h4">
                                Forgot password
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1 }}
                            >
                                Request a one-time link for your tenant account.
                            </Typography>
                        </Box>

                        {mutation.isSuccess ? (
                            <Stack spacing={2} sx={{ width: '100%' }}>
                                <Alert severity="success">
                                    {mutation.data.message}
                                </Alert>

                                {resetLink ? (
                                    <>
                                        <Typography color="text.secondary">
                                            This development link expires after 15
                                            minutes and can be used only once.
                                        </Typography>
                                        <TextField
                                            fullWidth
                                            label="Reset link"
                                            slotProps={{
                                                htmlInput: {
                                                    readOnly: true,
                                                },
                                            }}
                                            value={resetLink}
                                        />
                                        {copyFeedback && (
                                            <Alert severity="info">
                                                {copyFeedback}
                                            </Alert>
                                        )}
                                        <Button
                                            onClick={() => {
                                                void copyResetLink()
                                            }}
                                            startIcon={
                                                <ContentCopyRoundedIcon />
                                            }
                                            variant="outlined"
                                        >
                                            Copy reset link
                                        </Button>
                                        <Button
                                            onClick={() => {
                                                navigate(
                                                    `/reset-password?token=${encodeURIComponent(mutation.data.devResetToken ?? '')}`,
                                                )
                                            }}
                                            variant="contained"
                                        >
                                            Reset password
                                        </Button>
                                    </>
                                ) : (
                                    <Alert severity="info">
                                        Check your email for the password reset link.
                                    </Alert>
                                )}

                                <Button
                                    onClick={() => {
                                        navigate('/login')
                                    }}
                                >
                                    Back to sign in
                                </Button>
                            </Stack>
                        ) : (
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
                                        autoComplete="organization"
                                        autoFocus
                                        disabled={mutation.isPending}
                                        label="Tenant ID"
                                        onChange={(event) => {
                                            setTenantId(event.target.value)
                                            setValidationError(null)
                                        }}
                                        required
                                        value={tenantId}
                                    />
                                    <TextField
                                        autoComplete="email"
                                        disabled={mutation.isPending}
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
                                        label="Email address"
                                        onChange={(event) => {
                                            setEmail(event.target.value)
                                            setValidationError(null)
                                        }}
                                        required
                                        type="email"
                                        value={email}
                                    />
                                    <Button
                                        disabled={mutation.isPending}
                                        size="large"
                                        type="submit"
                                        variant="contained"
                                    >
                                        {mutation.isPending
                                            ? 'Requesting link…'
                                            : 'Request reset link'}
                                    </Button>
                                    <Button
                                        disabled={mutation.isPending}
                                        onClick={() => {
                                            navigate('/login')
                                        }}
                                    >
                                        Back to sign in
                                    </Button>
                                </Stack>
                            </Box>
                        )}
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
