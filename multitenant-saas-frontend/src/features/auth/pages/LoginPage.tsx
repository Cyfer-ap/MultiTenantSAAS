import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
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
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { normalizeApiError } from '../../../api/apiError'
import { authApi } from '../api/authApi'
import { useAuth } from '../hooks/useAuth'
import type { WorkspaceLoginOption } from '../types/auth'

const LEGACY_TRUSTED_BROWSER_TOKEN_KEY = 'multitenant-saas.trusted-email-browser'

type LoginStep = 'email' | 'code' | 'workspace' | 'password'

interface LoginRouteState {
    from?: unknown
    email?: unknown
    passwordChanged?: unknown
    allDevicesSignedOut?: unknown
}

function resolvePrefillValue(value: unknown): string {
    return typeof value === 'string' ? value : ''
}

function resolveRedirectPath(state: unknown): string {
    if (typeof state !== 'object' || state === null) {
        return '/dashboard'
    }

    const { from } = state as LoginRouteState

    if (typeof from === 'string' && from.startsWith('/') && !from.startsWith('//')) {
        return from
    }

    return '/dashboard'
}

function isValidEmail(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

export function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()
    const routeState =
        typeof location.state === 'object' && location.state !== null
            ? (location.state as LoginRouteState)
            : {}

    useEffect(() => {
        try {
            window.localStorage.removeItem(LEGACY_TRUSTED_BROWSER_TOKEN_KEY)
        } catch {
            // Ignore unavailable browser storage.
        }
    }, [])

    const [step, setStep] = useState<LoginStep>('email')
    const [email, setEmail] = useState(resolvePrefillValue(routeState.email))
    const [code, setCode] = useState('')
    const [password, setPassword] = useState('')
    const [challengeId, setChallengeId] = useState<string | null>(null)
    const [workspaceGrantId, setWorkspaceGrantId] = useState<string | null>(null)
    const [workspaces, setWorkspaces] = useState<WorkspaceLoginOption[]>([])
    const [selectedWorkspace, setSelectedWorkspace] = useState<WorkspaceLoginOption | null>(null)
    const [trustBrowser, setTrustBrowser] = useState(true)
    const [keepSignedIn, setKeepSignedIn] = useState(false)
    const [busy, setBusy] = useState(false)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)
    const [infoMessage, setInfoMessage] = useState<string | null>(null)

    const moveToWorkspaces = (
        availableWorkspaces: WorkspaceLoginOption[],
        grantId: string | null,
    ): void => {
        setWorkspaces(availableWorkspaces)

        if (availableWorkspaces.length === 0) {
            setErrorMessage('No active workspaces are available for this email.')
            setStep('email')
            return
        }

        if (!grantId) {
            setErrorMessage('Email verification expired. Verify your email again.')
            setWorkspaceGrantId(null)
            setStep('email')
            return
        }

        setWorkspaceGrantId(grantId)

        if (availableWorkspaces.length === 1) {
            setSelectedWorkspace(availableWorkspaces[0])
            setStep('password')
            return
        }

        setSelectedWorkspace(null)
        setStep('workspace')
    }

    const startDiscovery = async (): Promise<void> => {
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
                moveToWorkspaces(response.workspaces, response.workspaceGrantId)
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

            setInfoMessage(null)
            moveToWorkspaces(response.workspaces, response.workspaceGrantId)
        } catch (error: unknown) {
            setErrorMessage(normalizeApiError(error).message)
        } finally {
            setBusy(false)
        }
    }

    const submitPassword = async (): Promise<void> => {
        setErrorMessage(null)

        if (!selectedWorkspace) {
            setStep('workspace')
            return
        }

        if (!workspaceGrantId) {
            setErrorMessage('Email verification expired. Verify your email again.')
            setStep('email')
            return
        }

        if (!password) {
            setErrorMessage('Password is required.')
            return
        }

        setBusy(true)

        try {
            await login({
                tenantId: selectedWorkspace.tenantId,
                email,
                password,
                workspaceGrantId,
                keepSignedIn,
            })

            navigate(resolveRedirectPath(location.state), {
                replace: true,
            })
        } catch (error: unknown) {
            setErrorMessage(normalizeApiError(error).message)
        } finally {
            setBusy(false)
        }
    }

    const restart = (): void => {
        setStep('email')
        setCode('')
        setPassword('')
        setChallengeId(null)
        setWorkspaceGrantId(null)
        setWorkspaces([])
        setSelectedWorkspace(null)
        setKeepSignedIn(false)
        setErrorMessage(null)
        setInfoMessage(null)
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
                            <LockOutlinedIcon />
                        </Avatar>

                        <Box sx={{ textAlign: 'center' }}>
                            <Typography component="h1" variant="h4">
                                Sign in
                            </Typography>

                            <Typography sx={{ color: 'text.secondary', mt: 1 }}>
                                {step === 'email' && 'Start with your work email'}
                                {step === 'code' && 'Verify your email address'}
                                {step === 'workspace' && 'Choose a workspace'}
                                {step === 'password' &&
                                    `Sign in to ${selectedWorkspace?.name ?? 'your workspace'}`}
                            </Typography>
                        </Box>

                        <Stack spacing={2} sx={{ width: '100%' }}>
                            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
                            {infoMessage && <Alert severity="info">{infoMessage}</Alert>}

                            {routeState.passwordChanged === true && step === 'email' && (
                                <Alert severity="success">
                                    Password changed successfully. Sign in with your new password.
                                </Alert>
                            )}

                            {routeState.allDevicesSignedOut === true && step === 'email' && (
                                <Alert severity="success">
                                    All device refresh sessions were revoked. Sign in again to
                                    continue.
                                </Alert>
                            )}

                            {step === 'email' && (
                                <>
                                    <TextField
                                        autoFocus
                                        fullWidth
                                        label="Email address"
                                        type="email"
                                        autoComplete="email"
                                        value={email}
                                        disabled={busy}
                                        onChange={(event) => {
                                            setEmail(event.target.value)
                                        }}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter') {
                                                event.preventDefault()
                                                void startDiscovery()
                                            }
                                        }}
                                    />

                                    <Button
                                        fullWidth
                                        variant="contained"
                                        size="large"
                                        disabled={busy}
                                        onClick={() => {
                                            void startDiscovery()
                                        }}
                                    >
                                        {busy ? 'Checking...' : 'Continue'}
                                    </Button>

                                    <Button
                                        disabled={busy}
                                        fullWidth
                                        onClick={() => {
                                            navigate('/forgot-password')
                                        }}
                                    >
                                        Forgot password?
                                    </Button>

                                    <Button
                                        disabled={busy}
                                        fullWidth
                                        onClick={() => {
                                            navigate('/register')
                                        }}
                                        variant="outlined"
                                    >
                                        Create a workspace
                                    </Button>
                                </>
                            )}

                            {step === 'code' && (
                                <>
                                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                                        Enter the code sent to <strong>{email}</strong>.
                                    </Typography>

                                    <TextField
                                        autoFocus
                                        fullWidth
                                        label="Verification code"
                                        autoComplete="one-time-code"
                                        slotProps={{
                                            htmlInput: {
                                                inputMode: 'numeric',
                                                maxLength: 6,
                                            },
                                        }}
                                        value={code}
                                        disabled={busy}
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
                                        fullWidth
                                        variant="contained"
                                        size="large"
                                        disabled={busy}
                                        onClick={() => {
                                            void verifyCode()
                                        }}
                                    >
                                        {busy ? 'Verifying...' : 'Verify email'}
                                    </Button>

                                    <Button
                                        fullWidth
                                        disabled={busy}
                                        onClick={() => {
                                            void startDiscovery()
                                        }}
                                    >
                                        Send a new code
                                    </Button>

                                    <Button fullWidth disabled={busy} onClick={restart}>
                                        Use a different email
                                    </Button>
                                </>
                            )}

                            {step === 'workspace' && (
                                <>
                                    <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                                        Select the organization you want to access.
                                    </Typography>

                                    {workspaces.map((workspace) => (
                                        <Button
                                            key={workspace.tenantId}
                                            fullWidth
                                            variant="outlined"
                                            disabled={busy}
                                            onClick={() => {
                                                setSelectedWorkspace(workspace)
                                                setPassword('')
                                                setStep('password')
                                            }}
                                            sx={{ justifyContent: 'flex-start', py: 1.5 }}
                                        >
                                            {workspace.name}
                                        </Button>
                                    ))}

                                    <Button fullWidth disabled={busy} onClick={restart}>
                                        Use a different email
                                    </Button>
                                </>
                            )}

                            {step === 'password' && selectedWorkspace && (
                                <>
                                    <Alert severity="info">
                                        {email}
                                        <br />
                                        Workspace: {selectedWorkspace.name}
                                    </Alert>

                                    <TextField
                                        autoFocus
                                        fullWidth
                                        label="Password"
                                        type="password"
                                        autoComplete="current-password"
                                        value={password}
                                        disabled={busy}
                                        onChange={(event) => {
                                            setPassword(event.target.value)
                                        }}
                                        onKeyDown={(event) => {
                                            if (event.key === 'Enter') {
                                                event.preventDefault()
                                                void submitPassword()
                                            }
                                        }}
                                    />

                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={keepSignedIn}
                                                disabled={busy}
                                                onChange={(event) => {
                                                    setKeepSignedIn(event.target.checked)
                                                }}
                                            />
                                        }
                                        label="Keep me signed in"
                                    />

                                    <Button
                                        fullWidth
                                        variant="contained"
                                        size="large"
                                        disabled={busy}
                                        onClick={() => {
                                            void submitPassword()
                                        }}
                                    >
                                        {busy ? 'Signing in...' : 'Sign in'}
                                    </Button>

                                    {workspaces.length > 1 && (
                                        <Button
                                            fullWidth
                                            disabled={busy}
                                            onClick={() => {
                                                setPassword('')
                                                setSelectedWorkspace(null)
                                                setStep('workspace')
                                            }}
                                        >
                                            Choose another workspace
                                        </Button>
                                    )}

                                    <Button fullWidth disabled={busy} onClick={restart}>
                                        Use a different email
                                    </Button>
                                </>
                            )}
                        </Stack>
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
