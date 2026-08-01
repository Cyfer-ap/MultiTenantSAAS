import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Container,
    Link,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useForm } from 'react-hook-form'
import {
    Link as RouterLink,
    useLocation,
    useNavigate,
} from 'react-router'

import { normalizeApiError } from '../api/apiError'
import { useSystemAdmin } from '../features/system-admin/hooks/useSystemAdmin'
import type { SystemAdminLoginInput } from '../features/system-admin/types/systemAdmin'

interface SystemLoginRouteState {
    from?: unknown
}

function resolveRedirectPath(state: unknown): string {
    if (typeof state !== 'object' || state === null) {
        return '/system/dashboard'
    }

    const { from } = state as SystemLoginRouteState

    if (
        typeof from === 'string' &&
        from.startsWith('/system/') &&
        !from.startsWith('//')
    ) {
        return from
    }

    return '/system/dashboard'
}

export function SystemLoginPage() {
    const { login } = useSystemAdmin()
    const location = useLocation()
    const navigate = useNavigate()
    const {
        register,
        handleSubmit,
        clearErrors,
        setError,
        formState: { errors, isSubmitting },
    } = useForm<SystemAdminLoginInput>({
        defaultValues: {
            email: '',
            password: '',
        },
    })

    const submit = async (
        input: SystemAdminLoginInput,
    ): Promise<void> => {
        clearErrors('root')

        try {
            await login({
                email: input.email.trim().toLowerCase(),
                password: input.password,
            })
            navigate(resolveRedirectPath(location.state), {
                replace: true,
            })
        }
        catch (error: unknown) {
            setError('root', {
                type: 'server',
                message: normalizeApiError(error).message,
            })
        }
    }

    return (
        <Box
            sx={{
                alignItems: 'center',
                background:
                    'linear-gradient(145deg, #eef2ff 0%, #f8fafc 48%, #e0f2fe 100%)',
                display: 'flex',
                minHeight: '100vh',
                py: 4,
            }}
        >
            <Container maxWidth="xs">
                <Paper
                    elevation={8}
                    sx={{ borderRadius: 4, p: { xs: 3, sm: 4 } }}
                >
                    <Stack spacing={3} sx={{ alignItems: 'center' }}>
                        <Avatar
                            sx={{
                                bgcolor: 'primary.main',
                                height: 52,
                                width: 52,
                            }}
                        >
                            <AdminPanelSettingsRoundedIcon />
                        </Avatar>

                        <Box sx={{ textAlign: 'center' }}>
                            <Typography component="h1" variant="h4">
                                System console
                            </Typography>
                            <Typography
                                color="text.secondary"
                                sx={{ mt: 1 }}
                            >
                                Sign in as a platform administrator
                            </Typography>
                        </Box>

                        <Box
                            component="form"
                            noValidate
                            onSubmit={handleSubmit(submit)}
                            sx={{ width: '100%' }}
                        >
                            <Stack spacing={2}>
                                {errors.root?.message && (
                                    <Alert severity="error">
                                        {errors.root.message}
                                    </Alert>
                                )}

                                <TextField
                                    autoComplete="email"
                                    autoFocus
                                    disabled={isSubmitting}
                                    error={Boolean(errors.email)}
                                    fullWidth
                                    helperText={errors.email?.message}
                                    label="Email address"
                                    type="email"
                                    {...register('email', {
                                        required: 'Email address is required.',
                                        pattern: {
                                            value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                                            message: 'Enter a valid email address.',
                                        },
                                    })}
                                />

                                <TextField
                                    autoComplete="current-password"
                                    disabled={isSubmitting}
                                    error={Boolean(errors.password)}
                                    fullWidth
                                    helperText={errors.password?.message}
                                    label="Password"
                                    type="password"
                                    {...register('password', {
                                        required: 'Password is required.',
                                    })}
                                />

                                <Button
                                    disabled={isSubmitting}
                                    fullWidth
                                    size="large"
                                    type="submit"
                                    variant="contained"
                                >
                                    {isSubmitting
                                        ? 'Signing in…'
                                        : 'Sign in to system console'}
                                </Button>
                            </Stack>
                        </Box>

                        <Typography color="text.secondary" variant="body2">
                            Looking for your tenant workspace?{' '}
                            <Link component={RouterLink} to="/login">
                                Tenant sign in
                            </Link>
                        </Typography>
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
