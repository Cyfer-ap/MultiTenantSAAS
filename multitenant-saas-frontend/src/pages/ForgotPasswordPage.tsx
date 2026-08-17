import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import KeyRoundedIcon from '@mui/icons-material/KeyRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Checkbox,
    Container,
    FormControlLabel,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { normalizeApiError } from '../api/apiError'
import { authApi } from '../features/auth/api/authApi'
import type { WorkspaceLoginOption } from '../features/auth/types/auth'
import { useForgotPassword } from '../features/password-reset/hooks/usePasswordReset'

type RecoveryStep = 'email' | 'code' | 'workspace' | 'sent'

function isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

export function ForgotPasswordPage() {
    const navigate = useNavigate()
    const mutation = useForgotPassword()

    const [step, setStep] = useState<RecoveryStep>('email')
    const [email, setEmail] = useState('')
    const [challengeId, setChallengeId] = useState<string | null>(null)
    const [workspaceGrantId, setWorkspaceGrantId] = useState<string | null>(null)
    const [code, setCode] = useState('')
    const [trustBrowser, setTrustBrowser] = useState(true)
    const [workspaces, setWorkspaces] = useState<WorkspaceLoginOption[]>([])
    const [busy, setBusy] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [infoMessage, setInfoMessage] = useState<string | null>(null)
    const [copyFeedback, setCopyFeedback] = useState<string | null>(null)

    const resetLink = mutation.data?.devResetToken
        ? `${window.location.origin}/reset-password?token=${encodeURIComponent(mutation.data.devResetToken)}`
        : null

    const acceptVerifiedWorkspaces = (
        availableWorkspaces: WorkspaceLoginOption[],
        grantId: string | null,
    ): void => {
        if (!grantId || availableWorkspaces.length === 0) {
            setErrorMessage('No active workspace is available for password recovery.')
            setWorkspaceGrantId(null)
            setWorkspaces([])
            setStep('email')
            return
        }

        setWorkspaceGrantId(grantId)
        setWorkspaces(availableWorkspaces)
        setInfoMessage(null)
        setStep('workspace')
    }

    const startVerification = async (): Promise<void> => {
        const normalizedEmail = email.trim().toLowerCase()

        setErrorMessage(null)
        setInfoMessage(null)

        if (!isValidEmail(normalizedEmail)) {
            setErrorMessage('Enter a valid email address.')
            return
        }

        setBusy(true)

        try {
            const response = await authApi.startWorkspaceDiscovery({
                email: normalizedEmail,
            })

            setEmail(normalizedEmail)

            if (!response.verificationRequired) {
                acceptVerifiedWorkspaces(response.workspaces, response.workspaceGrantId)
                return
            }

            if (!response.challengeId) {
                setErrorMessage('Could not start email verification. Please try again.')
                return
            }

            setChallengeId(response.challengeId)
            setCode('')
            setInfoMessage(response.message)
            setStep('code')
        } catch (error: unknown) {
            setErrorMessage(normalizeApiError(error).message)
        } finally {
            setBusy(false)
        }
    }

    const verifyCode = async (): Promise<void> => {
        setErrorMessage(null)

        if (!challengeId) {
            setErrorMessage('Email verification expired. Request a new code.')
            setStep('email')
            return
        }

        if (!/^\d{6}$/.test(code)) {
            setErrorMessage('Enter the 6-digit verification code.')
            return
        }

        setBusy(true)

        try {
            const response = await authApi.verifyWorkspaceDiscovery({
                challengeId,
                code,
                trustBrowser,
            })

            acceptVerifiedWorkspaces(response.workspaces, response.workspaceGrantId)
        } catch (error: unknown) {
            setErrorMessage(normalizeApiError(error).message)
        } finally {
            setBusy(false)
        }
    }

    const requestResetLink = async (workspace: WorkspaceLoginOption): Promise<void> => {
        if (!workspaceGrantId) {
            setErrorMessage('Email verification expired. Verify your email again.')
            setStep('email')
            return
        }

        setErrorMessage(null)
        setBusy(true)

        try {
            await mutation.mutateAsync({
                tenantId: workspace.tenantId,
                email,
                workspaceGrantId,
            })
            setStep('sent')
        } catch (error: unknown) {
            setErrorMessage(normalizeApiError(error).message)
        } finally {
            setBusy(false)
        }
    }

    const restart = (): void => {
        mutation.reset()
        setStep('email')
        setChallengeId(null)
        setWorkspaceGrantId(null)
        setCode('')
        setWorkspaces([])
        setErrorMessage(null)
        setInfoMessage(null)
        setCopyFeedback(null)
    }

    const copyResetLink = async (): Promise<void> => {
        if (!resetLink) {
            return
        }

        try {
            await navigator.clipboard.writeText(resetLink)
            setCopyFeedback('Reset link copied.')
        } catch {
            setCopyFeedback('Copy failed. Select and copy the link manually.')
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
                            <Typography color="text.secondary" sx={{ marginTop: 1 }}>
                                {step === 'email' && 'Start with your work email.'}
                                {step === 'code' && 'Verify ownership of your email.'}
                                {step === 'workspace' && 'Choose the workspace to recover.'}
                                {step === 'sent' && 'Password reset instructions are ready.'}
                            </Typography>
                        </Box>

                        <Stack spacing={2} sx={{ width: '100%' }}>
                            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
                            {infoMessage && <Alert severity="info">{infoMessage}</Alert>}

                            {step === 'email' && (
                                <>
                                    <TextField
                                        autoComplete="email"
                                        autoFocus
                                        disabled={busy}
                                        label="Email address"
                                        onChange={(event) => {
                                            setEmail(event.target.value)
                                            setErrorMessage(null)
                                        }}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter') {
                                                event.preventDefault()
                                                void startVerification()
                                            }
                                        }}
                                        required
                                        type="email"
                                        value={email}
                                    />
                                    <Button
                                        disabled={busy}
                                        onClick={() => {
                                            void startVerification()
                                        }}
                                        size="large"
                                        variant="contained"
                                    >
                                        {busy ? 'Checkingâ€¦' : 'Continue'}
                                    </Button>
                                </>
                            )}

                            {step === 'code' && (
                                <>
                                    <Typography variant="body2" color="text.secondary">
                                        Enter the code sent to <strong>{email}</strong>.
                                    </Typography>
                                    <TextField
                                        autoComplete="one-time-code"
                                        autoFocus
                                        disabled={busy}
                                        label="Verification code"
                                        slotProps={{
                                            htmlInput: {
                                                inputMode: 'numeric',
                                                maxLength: 6,
                                            },
                                        }}
                                        value={code}
                                        onChange={(event) => {
                                            setCode(
                                                event.target.value.replace(/\D/g, '').slice(0, 6),
                                            )
                                        }}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter') {
                                                event.preventDefault()
                                                void verifyCode()
                                            }
                                        }}
                                    />
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={trustBrowser}
                                                disabled={busy}
                                                onChange={(event) => {
                                                    setTrustBrowser(event.target.checked)
                                                }}
                                            />
                                        }
                                        label="Trust this browser for 30 days"
                                    />
                                    <Button
                                        disabled={busy}
                                        onClick={() => {
                                            void verifyCode()
                                        }}
                                        size="large"
                                        variant="contained"
                                    >
                                        {busy ? 'Verifyingâ€¦' : 'Verify email'}
                                    </Button>
                                    <Button
                                        disabled={busy}
                                        onClick={() => {
                                            void startVerification()
                                        }}
                                    >
                                        Send a new code
                                    </Button>
                                </>
                            )}

                            {step === 'workspace' && (
                                <>
                                    <Alert severity="info">
                                        Verified email: <strong>{email}</strong>
                                    </Alert>
                                    {workspaces.map((workspace) => (
                                        <Button
                                            key={workspace.tenantId}
                                            disabled={busy || mutation.isPending}
                                            onClick={() => {
                                                void requestResetLink(workspace)
                                            }}
                                            sx={{ justifyContent: 'flex-start', py: 1.5 }}
                                            variant="outlined"
                                        >
                                            Send reset link for {workspace.name}
                                        </Button>
                                    ))}
                                    <Button disabled={busy} onClick={restart}>
                                        Use a different email
                                    </Button>
                                </>
                            )}

                            {step === 'sent' && mutation.data && (
                                <>
                                    <Alert severity="success">{mutation.data.message}</Alert>

                                    {resetLink ? (
                                        <>
                                            <Alert severity="warning">
                                                Development mode only: the reset token is exposed
                                                because PASSWORD_RESET_EXPOSE_TOKEN is enabled.
                                            </Alert>
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
                                                <Alert severity="info">{copyFeedback}</Alert>
                                            )}
                                            <Button
                                                onClick={() => {
                                                    void copyResetLink()
                                                }}
                                                startIcon={<ContentCopyRoundedIcon />}
                                                variant="outlined"
                                            >
                                                Copy reset link
                                            </Button>
                                            <Button
                                                onClick={() => {
                                                    navigate(
                                                        `/reset-password?token=${encodeURIComponent(
                                                            mutation.data?.devResetToken ?? '',
                                                        )}`,
                                                    )
                                                }}
                                                variant="contained"
                                            >
                                                Reset password
                                            </Button>
                                        </>
                                    ) : (
                                        <Alert severity="info">
                                            Check your email for the single-use password reset link.
                                        </Alert>
                                    )}
                                </>
                            )}

                            <Button
                                disabled={busy}
                                onClick={() => {
                                    navigate('/login')
                                }}
                            >
                                Back to sign in
                            </Button>
                        </Stack>
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
