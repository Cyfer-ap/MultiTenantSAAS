import LockOutlinedIcon from '@mui/icons-material/LockOutlined'
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
import { useForm } from 'react-hook-form'
import {
    useLocation,
    useNavigate,
} from 'react-router'

import { normalizeApiError } from '../../../api/apiError'
import { useAuth } from '../hooks/useAuth'
import type { LoginInput } from '../types/auth'

interface LoginRouteState {
    from?: unknown
}

function resolveRedirectPath(
    state: unknown,
): string {
    if (
        typeof state !== 'object' ||
        state === null
    ) {
        return '/dashboard'
    }

    const { from } = state as LoginRouteState

    if (
        typeof from === 'string' &&
        from.startsWith('/') &&
        !from.startsWith('//')
    ) {
        return from
    }

    return '/dashboard'
}

export function LoginPage() {
    const { login } = useAuth()
    const navigate = useNavigate()
    const location = useLocation()

    const {
        register,
        handleSubmit,
        clearErrors,
        setError,
        formState: {
            errors,
            isSubmitting,
        },
    } = useForm<LoginInput>({
        defaultValues: {
            tenantId: '',
            email: '',
            password: '',
        },
    })

    const submitLogin = async (
        input: LoginInput,
    ): Promise<void> => {
        clearErrors('root')

        try {
            await login({
                tenantId: input.tenantId.trim(),
                email: input.email.trim(),
                password: input.password,
            })

            navigate(
                resolveRedirectPath(location.state),
                {
                    replace: true,
                },
            )
        }
        catch (error: unknown) {
            const apiError = normalizeApiError(error)

            setError('root', {
                type: 'server',
                message: apiError.message,
            })
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
                    <Stack
                        spacing={3}
                        sx={{
                            alignItems: 'center',
                        }}
                    >
                        <Avatar
                            sx={{
                                backgroundColor: 'primary.main',
                                height: 48,
                                width: 48,
                            }}
                        >
                            <LockOutlinedIcon />
                        </Avatar>

                        <Box
                            sx={{
                                textAlign: 'center',
                            }}
                        >
                            <Typography
                                component="h1"
                                variant="h4"
                            >
                                Sign in
                            </Typography>

                            <Typography
                                sx={{
                                    color: 'text.secondary',
                                    mt: 1,
                                }}
                            >
                                Access your tenant workspace
                            </Typography>
                        </Box>

                        <Box
                            component="form"
                            noValidate
                            onSubmit={handleSubmit(submitLogin)}
                            sx={{
                                width: '100%',
                            }}
                        >
                            <Stack spacing={2}>
                                {errors.root?.message && (
                                    <Alert severity="error">
                                        {errors.root.message}
                                    </Alert>
                                )}

                                <TextField
                                    autoFocus
                                    fullWidth
                                    label="Tenant ID"
                                    autoComplete="organization"
                                    disabled={isSubmitting}
                                    error={Boolean(errors.tenantId)}
                                    helperText={
                                        errors.tenantId?.message
                                    }
                                    {...register('tenantId', {
                                        validate: (value) =>
                                            value.trim().length > 0 ||
                                            'Tenant ID is required.',
                                    })}
                                />

                                <TextField
                                    fullWidth
                                    label="Email address"
                                    type="email"
                                    autoComplete="email"
                                    disabled={isSubmitting}
                                    error={Boolean(errors.email)}
                                    helperText={errors.email?.message}
                                    {...register('email', {
                                        required:
                                            'Email address is required.',
                                        pattern: {
                                            value:
                                                /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                                            message:
                                                'Enter a valid email address.',
                                        },
                                    })}
                                />

                                <TextField
                                    fullWidth
                                    label="Password"
                                    type="password"
                                    autoComplete="current-password"
                                    disabled={isSubmitting}
                                    error={Boolean(errors.password)}
                                    helperText={
                                        errors.password?.message
                                    }
                                    {...register('password', {
                                        required: 'Password is required.',
                                    })}
                                />

                                <Button
                                    fullWidth
                                    type="submit"
                                    variant="contained"
                                    size="large"
                                    disabled={isSubmitting}
                                >
                                    {isSubmitting
                                        ? 'Signing in...'
                                        : 'Sign in'}
                                </Button>
                            </Stack>
                        </Box>
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}