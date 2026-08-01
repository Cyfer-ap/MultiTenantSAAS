import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Container,
    Divider,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { ApiClientError } from '../api/apiError'
import { useTenantOnboarding } from '../features/onboarding/hooks/useTenantOnboarding'

const passwordPattern =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,100}$/
const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'The workspace could not be created.'
}

function getFieldError(
    error: unknown,
    field: string,
): string | undefined {
    return error instanceof ApiClientError
        ? error.details?.[field]
        : undefined
}

export function TenantOnboardingPage() {
    const navigate = useNavigate()
    const mutation = useTenantOnboarding()
    const [tenantName, setTenantName] = useState('')
    const [tenantSlug, setTenantSlug] = useState('')
    const [adminFullName, setAdminFullName] = useState('')
    const [adminEmail, setAdminEmail] = useState('')
    const [adminPassword, setAdminPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [validationError, setValidationError] =
        useState<string | null>(null)

    const clearValidationError = (): void => {
        setValidationError(null)
    }

    const submit = async (
        event: FormEvent<HTMLFormElement>,
    ): Promise<void> => {
        event.preventDefault()

        const normalizedTenantName = tenantName.trim()
        const normalizedTenantSlug = tenantSlug.trim().toLowerCase()
        const normalizedAdminFullName = adminFullName.trim()
        const normalizedAdminEmail = adminEmail.trim().toLowerCase()

        if (
            normalizedTenantName.length < 2 ||
            normalizedTenantName.length > 100
        ) {
            setValidationError(
                'Workspace name must be between 2 and 100 characters.',
            )
            return
        }

        if (
            normalizedTenantSlug.length < 2 ||
            normalizedTenantSlug.length > 80 ||
            !slugPattern.test(normalizedTenantSlug)
        ) {
            setValidationError(
                'Workspace slug must be 2–80 lowercase letters, numbers, or single hyphens, without a leading or trailing hyphen.',
            )
            return
        }

        if (
            normalizedAdminFullName.length < 2 ||
            normalizedAdminFullName.length > 100
        ) {
            setValidationError(
                'Administrator name must be between 2 and 100 characters.',
            )
            return
        }

        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedAdminEmail)) {
            setValidationError('Enter a valid administrator email address.')
            return
        }

        if (!passwordPattern.test(adminPassword)) {
            setValidationError(
                'Password must be 8–100 characters and contain uppercase, lowercase, number, and special characters with no spaces.',
            )
            return
        }

        if (adminPassword !== confirmPassword) {
            setValidationError('Passwords do not match.')
            return
        }

        setValidationError(null)

        try {
            await mutation.mutateAsync({
                tenantName: normalizedTenantName,
                tenantSlug: normalizedTenantSlug,
                adminFullName: normalizedAdminFullName,
                adminEmail: normalizedAdminEmail,
                adminPassword,
            })
        }
        catch {
            // The mutation error is rendered on the page.
        }
    }

    const continueToLogin = (): void => {
        if (!mutation.data) {
            return
        }

        navigate('/login', {
            replace: true,
            state: {
                tenantId: mutation.data.tenant.id,
                email: mutation.data.adminUser.email,
            },
        })
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
            <Container maxWidth="sm">
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
                            <BusinessRoundedIcon />
                        </Avatar>

                        {mutation.isSuccess ? (
                            <>
                                <Box sx={{ textAlign: 'center' }}>
                                    <Typography component="h1" variant="h4">
                                        Workspace created
                                    </Typography>
                                    <Typography
                                        color="text.secondary"
                                        sx={{ marginTop: 1 }}
                                    >
                                        Your administrator account is ready.
                                    </Typography>
                                </Box>

                                <Alert severity="success" sx={{ width: '100%' }}>
                                    {mutation.data.message}
                                </Alert>

                                <Stack
                                    divider={<Divider flexItem />}
                                    spacing={1.5}
                                    sx={{ width: '100%' }}
                                >
                                    <Box>
                                        <Typography
                                            color="text.secondary"
                                            variant="caption"
                                        >
                                            Workspace
                                        </Typography>
                                        <Typography>
                                            {mutation.data.tenant.name}
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography
                                            color="text.secondary"
                                            variant="caption"
                                        >
                                            Tenant ID
                                        </Typography>
                                        <Typography sx={{ overflowWrap: 'anywhere' }}>
                                            {mutation.data.tenant.id}
                                        </Typography>
                                    </Box>
                                    <Box>
                                        <Typography
                                            color="text.secondary"
                                            variant="caption"
                                        >
                                            Administrator email
                                        </Typography>
                                        <Typography>
                                            {mutation.data.adminUser.email}
                                        </Typography>
                                    </Box>
                                </Stack>

                                <Button
                                    fullWidth
                                    onClick={continueToLogin}
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
                                        Create workspace
                                    </Typography>
                                    <Typography
                                        color="text.secondary"
                                        sx={{ marginTop: 1 }}
                                    >
                                        Set up a tenant and its first administrator.
                                    </Typography>
                                </Box>

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
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'tenantName',
                                                ),
                                            )}
                                            helperText={getFieldError(
                                                mutation.error,
                                                'tenantName',
                                            )}
                                            label="Workspace name"
                                            onChange={(event) => {
                                                setTenantName(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            value={tenantName}
                                        />
                                        <TextField
                                            autoComplete="off"
                                            disabled={mutation.isPending}
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'tenantSlug',
                                                ),
                                            )}
                                            helperText={
                                                getFieldError(
                                                    mutation.error,
                                                    'tenantSlug',
                                                ) ??
                                                'Lowercase letters, numbers, and single hyphens.'
                                            }
                                            label="Workspace slug"
                                            onChange={(event) => {
                                                setTenantSlug(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            value={tenantSlug}
                                        />

                                        <Divider>Administrator</Divider>

                                        <TextField
                                            autoComplete="name"
                                            disabled={mutation.isPending}
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'adminFullName',
                                                ),
                                            )}
                                            helperText={getFieldError(
                                                mutation.error,
                                                'adminFullName',
                                            )}
                                            label="Full name"
                                            onChange={(event) => {
                                                setAdminFullName(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            value={adminFullName}
                                        />
                                        <TextField
                                            autoComplete="email"
                                            disabled={mutation.isPending}
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'adminEmail',
                                                ),
                                            )}
                                            helperText={getFieldError(
                                                mutation.error,
                                                'adminEmail',
                                            )}
                                            label="Email address"
                                            onChange={(event) => {
                                                setAdminEmail(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            type="email"
                                            value={adminEmail}
                                        />
                                        <TextField
                                            autoComplete="new-password"
                                            disabled={mutation.isPending}
                                            error={Boolean(
                                                getFieldError(
                                                    mutation.error,
                                                    'adminPassword',
                                                ),
                                            )}
                                            helperText={
                                                getFieldError(
                                                    mutation.error,
                                                    'adminPassword',
                                                ) ??
                                                '8–100 characters with uppercase, lowercase, number, special character, and no spaces.'
                                            }
                                            label="Password"
                                            onChange={(event) => {
                                                setAdminPassword(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            type="password"
                                            value={adminPassword}
                                        />
                                        <TextField
                                            autoComplete="new-password"
                                            disabled={mutation.isPending}
                                            label="Confirm password"
                                            onChange={(event) => {
                                                setConfirmPassword(event.target.value)
                                                clearValidationError()
                                            }}
                                            required
                                            type="password"
                                            value={confirmPassword}
                                        />

                                        <Button
                                            disabled={mutation.isPending}
                                            size="large"
                                            type="submit"
                                            variant="contained"
                                        >
                                            {mutation.isPending
                                                ? 'Creating workspace…'
                                                : 'Create workspace'}
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
                            </>
                        )}
                    </Stack>
                </Paper>
            </Container>
        </Box>
    )
}
