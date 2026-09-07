import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
import {
    Alert,
    Avatar,
    Box,
    Button,
    CircularProgress,
    Container,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'

import { normalizeApiError } from '../../../api/apiError'
import { authApi } from '../api/authApi'
import { commitLoginResponse } from '../session/commitLoginResponse'

export function OidcCompletePage() {
    const location = useLocation()
    const navigate = useNavigate()
    const exchangeStarted = useRef(false)
    const [busy, setBusy] = useState(true)
    const [errorMessage, setErrorMessage] = useState<string | null>(null)

    useEffect(() => {
        if (exchangeStarted.current) {
            return
        }

        exchangeStarted.current = true

        const parameters = new URLSearchParams(location.search)
        const code = parameters.get('code')?.trim() ?? ''
        const providerError = parameters.get('error')?.trim() ?? ''

        if (location.search) {
            navigate(location.pathname, { replace: true })
        }

        if (providerError) {
            setErrorMessage('Single sign-on could not be completed. Please return to sign in.')
            setBusy(false)
            return
        }

        if (!code) {
            setErrorMessage(
                'The single sign-on session is missing or invalid. Please sign in again.',
            )
            setBusy(false)
            return
        }

        async function completeLogin(): Promise<void> {
            try {
                const response = await authApi.completeOidcLogin(code)
                commitLoginResponse(response)
                navigate('/dashboard', { replace: true })
            } catch (error: unknown) {
                setErrorMessage(normalizeApiError(error).message)
                setBusy(false)
            }
        }

        void completeLogin()
    }, [location.pathname, location.search, navigate])

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
                <Paper elevation={4} sx={{ borderRadius: 3, p: { xs: 3, sm: 4 } }}>
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
                                Completing sign in
                            </Typography>
                            <Typography sx={{ color: 'text.secondary', mt: 1 }}>
                                Securely establishing your workspace session.
                            </Typography>
                        </Box>

                        {busy && !errorMessage && (
                            <CircularProgress aria-label="Completing single sign-on" />
                        )}

                        {errorMessage && (
                            <Stack spacing={2} sx={{ width: '100%' }}>
                                <Alert severity="error">{errorMessage}</Alert>
                                <Button
                                    fullWidth
                                    variant="contained"
                                    onClick={() => {
                                        navigate('/login', { replace: true })
                                    }}
                                >
                                    Return to sign in
                                </Button>
                            </Stack>
                        )}
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
