import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SecurityRoundedIcon from '@mui/icons-material/SecurityRounded'
import VpnKeyRoundedIcon from '@mui/icons-material/VpnKeyRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    FormControl,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useCreateIdentityProvider,
    useDisableIdentityProvider,
    useEnableIdentityProvider,
    useIdentityProvider,
    useRotateIdentityProviderSecret,
    useUpdateIdentityProvider,
    useUpdateIdentityProviderPolicy,
    useVerifyIdentityProvider,
} from '../features/identity-provider/hooks/useIdentityProvider'
import type {
    TenantIdentityProvider,
    TenantIdentityProviderCreateInput,
    TenantIdentityProviderUpdateInput,
    TenantSsoMode,
} from '../features/identity-provider/types/identityProvider'

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function formatDateTime(value: string | null): string {
    if (!value) {
        return '—'
    }
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
        return '—'
    }
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function statusColor(status: TenantIdentityProvider['status']): 'default' | 'success' | 'warning' {
    if (status === 'VERIFIED') {
        return 'success'
    }
    if (status === 'DRAFT') {
        return 'warning'
    }
    return 'default'
}

function parseScopes(value: string): string[] {
    return [
        ...new Set(
            value
                .split(/[\s,]+/)
                .map((scope) => scope.trim())
                .filter(Boolean),
        ),
    ]
}

interface ProviderConfigurationFormProps {
    provider: TenantIdentityProvider | null
    saving: boolean
    onCreate: (input: TenantIdentityProviderCreateInput) => void
    onUpdate: (input: TenantIdentityProviderUpdateInput) => void
}

function ProviderConfigurationForm({
    provider,
    saving,
    onCreate,
    onUpdate,
}: ProviderConfigurationFormProps) {
    const [displayName, setDisplayName] = useState(provider?.displayName ?? '')
    const [issuerUri, setIssuerUri] = useState(provider?.issuerUri ?? '')
    const [clientId, setClientId] = useState(provider?.clientId ?? '')
    const [clientSecret, setClientSecret] = useState('')
    const [scopes, setScopes] = useState(
        (provider?.scopes ?? ['openid', 'profile', 'email']).join(' '),
    )

    const normalizedScopes = parseScopes(scopes)
    const canSave =
        displayName.trim().length > 0 &&
        issuerUri.trim().length > 0 &&
        clientId.trim().length > 0 &&
        normalizedScopes.includes('openid') &&
        (provider !== null || clientSecret.length >= 8) &&
        !saving

    const submit = (): void => {
        if (!canSave) {
            return
        }
        if (provider) {
            onUpdate({
                displayName: displayName.trim(),
                issuerUri: issuerUri.trim(),
                clientId: clientId.trim(),
                scopes: normalizedScopes,
            })
            return
        }
        onCreate({
            displayName: displayName.trim(),
            protocol: 'OIDC',
            issuerUri: issuerUri.trim(),
            clientId: clientId.trim(),
            clientSecret,
            scopes: normalizedScopes,
        })
    }

    return (
        <Stack spacing={2}>
            <TextField
                label="Provider display name"
                onChange={(event) => setDisplayName(event.target.value)}
                placeholder="Company SSO"
                value={displayName}
            />
            <TextField
                helperText="Public HTTPS OIDC issuer. Private/internal addresses are rejected by the backend."
                label="Issuer URI"
                onChange={(event) => setIssuerUri(event.target.value)}
                placeholder="https://idp.example.com"
                value={issuerUri}
            />
            <TextField
                label="Client ID"
                onChange={(event) => setClientId(event.target.value)}
                value={clientId}
            />
            {!provider && (
                <TextField
                    autoComplete="new-password"
                    helperText="Write-only. The secret is encrypted server-side and will never be returned by the API."
                    label="Client secret"
                    onChange={(event) => setClientSecret(event.target.value)}
                    type="password"
                    value={clientSecret}
                />
            )}
            <TextField
                helperText="Separate scopes with spaces or commas. The openid scope is mandatory."
                label="OIDC scopes"
                onChange={(event) => setScopes(event.target.value)}
                value={scopes}
            />
            <Box>
                <Button disabled={!canSave} onClick={submit} variant="contained">
                    {saving ? (
                        <CircularProgress color="inherit" size={20} />
                    ) : provider ? (
                        'Save configuration'
                    ) : (
                        'Create configuration'
                    )}
                </Button>
            </Box>
        </Stack>
    )
}

interface RotateSecretDialogProps {
    open: boolean
    saving: boolean
    onClose: () => void
    onRotate: (secret: string) => void
}

function RotateSecretDialog({ open, saving, onClose, onRotate }: RotateSecretDialogProps) {
    const [secret, setSecret] = useState('')
    return (
        <Dialog fullWidth maxWidth="sm" onClose={saving ? undefined : onClose} open={open}>
            <DialogTitle>Rotate OIDC client secret</DialogTitle>
            <DialogContent>
                <Alert severity="warning" sx={{ mb: 2, mt: 0.5 }}>
                    Rotation invalidates provider verification and returns the tenant to optional
                    SSO until the provider is verified again.
                </Alert>
                <TextField
                    autoComplete="new-password"
                    fullWidth
                    label="New client secret"
                    onChange={(event) => setSecret(event.target.value)}
                    type="password"
                    value={secret}
                />
            </DialogContent>
            <DialogActions>
                <Button disabled={saving} onClick={onClose}>
                    Cancel
                </Button>
                <Button
                    disabled={saving || secret.length < 8}
                    onClick={() => onRotate(secret)}
                    variant="contained"
                >
                    {saving ? <CircularProgress color="inherit" size={20} /> : 'Rotate secret'}
                </Button>
            </DialogActions>
        </Dialog>
    )
}

export function TenantAuthenticationPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const providerQuery = useIdentityProvider(tenantId)
    const createProvider = useCreateIdentityProvider(tenantId)
    const updateProvider = useUpdateIdentityProvider(tenantId)
    const updatePolicy = useUpdateIdentityProviderPolicy(tenantId)
    const verifyProvider = useVerifyIdentityProvider(tenantId)
    const rotateSecret = useRotateIdentityProviderSecret(tenantId)
    const enableProvider = useEnableIdentityProvider(tenantId)
    const disableProvider = useDisableIdentityProvider(tenantId)
    const [rotateDialogOpen, setRotateDialogOpen] = useState(false)
    const [feedback, setFeedback] = useState<string | null>(null)

    const provider = providerQuery.data ?? null
    const mutationError =
        createProvider.error ??
        updateProvider.error ??
        updatePolicy.error ??
        verifyProvider.error ??
        rotateSecret.error ??
        enableProvider.error ??
        disableProvider.error

    const configurationKey = provider
        ? `${provider.id}:${provider.updatedAt}:${provider.secretVersion}`
        : 'new-provider'

    const changePolicy = (ssoMode: TenantSsoMode): void => {
        updatePolicy.mutate(ssoMode, {
            onSuccess: () => setFeedback(`SSO policy updated to ${ssoMode.toLowerCase()}.`),
        })
    }

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{ alignItems: { sm: 'flex-start' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Authentication
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Configure tenant OIDC federation, SSO enforcement, and recovery controls.
                    </Typography>
                </Box>
                <Button
                    disabled={providerQuery.isFetching}
                    onClick={() => void providerQuery.refetch()}
                    startIcon={<RefreshRoundedIcon />}
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            {mutationError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                    {getErrorMessage(mutationError, 'The SSO operation could not be completed.')}
                </Alert>
            )}

            <Alert icon={<SecurityRoundedIcon />} severity="info" sx={{ mt: 3 }}>
                Provider secrets remain server-side. SSO enforcement is allowed only after
                verification and only while at least one active tenant administrator retains a
                password-based break-glass path.
            </Alert>

            {providerQuery.isPending && (
                <Stack sx={{ alignItems: 'center', p: 5 }}>
                    <CircularProgress />
                </Stack>
            )}

            {providerQuery.isError && (
                <Alert severity="error" sx={{ mt: 3 }}>
                    {getErrorMessage(
                        providerQuery.error,
                        'Authentication configuration could not be loaded.',
                    )}
                </Alert>
            )}

            {providerQuery.isSuccess && (
                <>
                    <Paper variant="outlined" sx={{ mt: 3 }}>
                        <Stack
                            direction={{ xs: 'column', md: 'row' }}
                            spacing={2}
                            sx={{
                                alignItems: { md: 'center' },
                                justifyContent: 'space-between',
                                p: 2.5,
                            }}
                        >
                            <Box>
                                <Typography variant="h6">OIDC identity provider</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Configuration changes and secret rotation require verification
                                    again before federated login is available.
                                </Typography>
                            </Box>
                            {provider && (
                                <Stack
                                    direction="row"
                                    spacing={1}
                                    sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                                >
                                    <Chip
                                        color={statusColor(provider.status)}
                                        label={provider.status}
                                        size="small"
                                    />
                                    {provider.status === 'DISABLED' ? (
                                        <Button
                                            disabled={enableProvider.isPending}
                                            onClick={() =>
                                                enableProvider.mutate(undefined, {
                                                    onSuccess: () =>
                                                        setFeedback(
                                                            'Identity provider enabled in draft state. Verify it before using SSO.',
                                                        ),
                                                })
                                            }
                                            variant="contained"
                                        >
                                            Enable
                                        </Button>
                                    ) : (
                                        <Button
                                            color="error"
                                            disabled={disableProvider.isPending}
                                            onClick={() => {
                                                if (
                                                    window.confirm(
                                                        'Disable this identity provider? SSO policy will return to optional and federated login will stop.',
                                                    )
                                                ) {
                                                    disableProvider.mutate(undefined, {
                                                        onSuccess: () =>
                                                            setFeedback(
                                                                'Identity provider disabled.',
                                                            ),
                                                    })
                                                }
                                            }}
                                            variant="outlined"
                                        >
                                            Disable
                                        </Button>
                                    )}
                                </Stack>
                            )}
                        </Stack>
                        <Divider />
                        <Box sx={{ p: 2.5 }}>
                            {provider?.status === 'DISABLED' && (
                                <Alert severity="warning" sx={{ mb: 2 }}>
                                    This provider is disabled. Re-enable it to return it to draft
                                    state, then verify it before federated login can resume.
                                </Alert>
                            )}
                            <ProviderConfigurationForm
                                key={configurationKey}
                                onCreate={(input) =>
                                    createProvider.mutate(input, {
                                        onSuccess: () =>
                                            setFeedback(
                                                'Identity-provider configuration created in draft state.',
                                            ),
                                    })
                                }
                                onUpdate={(input) =>
                                    updateProvider.mutate(input, {
                                        onSuccess: () =>
                                            setFeedback(
                                                'Identity-provider configuration updated. Verification is required again.',
                                            ),
                                    })
                                }
                                provider={provider}
                                saving={createProvider.isPending || updateProvider.isPending}
                            />
                        </Box>
                    </Paper>

                    {provider && (
                        <Paper variant="outlined" sx={{ mt: 3 }}>
                            <Box sx={{ p: 2.5 }}>
                                <Typography variant="h6">
                                    Verification and secret lifecycle
                                </Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Verify controlled discovery/JWKS metadata before enabling
                                    federated authentication.
                                </Typography>
                            </Box>
                            <Divider />
                            <Stack spacing={2} sx={{ p: 2.5 }}>
                                <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                                    <Box sx={{ flex: 1 }}>
                                        <Typography variant="subtitle2">Last verified</Typography>
                                        <Typography color="text.secondary" variant="body2">
                                            {formatDateTime(provider.verifiedAt)}
                                        </Typography>
                                    </Box>
                                    <Box sx={{ flex: 1 }}>
                                        <Typography variant="subtitle2">Client secret</Typography>
                                        <Typography color="text.secondary" variant="body2">
                                            {provider.clientSecretHint} · version{' '}
                                            {provider.secretVersion}
                                        </Typography>
                                    </Box>
                                    <Box sx={{ flex: 1 }}>
                                        <Typography variant="subtitle2">Last rotated</Typography>
                                        <Typography color="text.secondary" variant="body2">
                                            {formatDateTime(provider.secretRotatedAt)}
                                        </Typography>
                                    </Box>
                                </Stack>
                                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                                    <Button
                                        disabled={
                                            provider.status === 'DISABLED' ||
                                            verifyProvider.isPending
                                        }
                                        onClick={() =>
                                            verifyProvider.mutate(undefined, {
                                                onSuccess: () =>
                                                    setFeedback(
                                                        'Identity provider verified successfully.',
                                                    ),
                                            })
                                        }
                                        startIcon={<CheckCircleOutlineRoundedIcon />}
                                        variant="contained"
                                    >
                                        {verifyProvider.isPending
                                            ? 'Verifying…'
                                            : provider.status === 'VERIFIED'
                                              ? 'Re-verify provider'
                                              : 'Verify provider'}
                                    </Button>
                                    <Button
                                        disabled={provider.status === 'DISABLED'}
                                        onClick={() => setRotateDialogOpen(true)}
                                        startIcon={<VpnKeyRoundedIcon />}
                                        variant="outlined"
                                    >
                                        Rotate client secret
                                    </Button>
                                </Stack>
                            </Stack>
                        </Paper>
                    )}

                    {provider && (
                        <Paper variant="outlined" sx={{ mt: 3 }}>
                            <Box sx={{ p: 2.5 }}>
                                <Typography variant="h6">SSO policy</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Choose whether workspace members may use passwords or must use
                                    the verified identity provider.
                                </Typography>
                            </Box>
                            <Divider />
                            <Stack spacing={2} sx={{ p: 2.5 }}>
                                <Alert severity="warning">
                                    Required SSO never removes the tenant-admin password break-glass
                                    path. The backend rejects enforcement unless an active tenant
                                    administrator with a password remains available.
                                </Alert>
                                <FormControl
                                    disabled={
                                        provider.status === 'DISABLED' || updatePolicy.isPending
                                    }
                                    sx={{ maxWidth: 360 }}
                                >
                                    <InputLabel id="tenant-sso-mode-label">
                                        Workspace SSO mode
                                    </InputLabel>
                                    <Select
                                        label="Workspace SSO mode"
                                        labelId="tenant-sso-mode-label"
                                        onChange={(event) =>
                                            changePolicy(event.target.value as TenantSsoMode)
                                        }
                                        value={provider.ssoMode}
                                    >
                                        <MenuItem value="OPTIONAL">
                                            Optional — password or SSO
                                        </MenuItem>
                                        <MenuItem
                                            disabled={provider.status !== 'VERIFIED'}
                                            value="REQUIRED"
                                        >
                                            Required — SSO for normal users
                                        </MenuItem>
                                    </Select>
                                </FormControl>
                                {provider.status !== 'VERIFIED' && (
                                    <Typography color="text.secondary" variant="body2">
                                        Verify the provider before required SSO can be selected.
                                    </Typography>
                                )}
                            </Stack>
                        </Paper>
                    )}
                </>
            )}

            <RotateSecretDialog
                key={rotateDialogOpen ? 'rotate-open' : 'rotate-closed'}
                onClose={() => setRotateDialogOpen(false)}
                onRotate={(secret) =>
                    rotateSecret.mutate(secret, {
                        onSuccess: () => {
                            setRotateDialogOpen(false)
                            setFeedback(
                                'Client secret rotated. Re-verify the provider before using SSO.',
                            )
                        },
                    })
                }
                open={rotateDialogOpen}
                saving={rotateSecret.isPending}
            />

            {feedback && (
                <Alert onClose={() => setFeedback(null)} severity="success" sx={{ mt: 3 }}>
                    {feedback}
                </Alert>
            )}
        </Box>
    )
}
