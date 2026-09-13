import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { useAuth } from '../features/auth/hooks/useAuth'
import {
    useAuthorizationAssignmentReferenceData,
    useAuthorizationPermissions,
} from '../features/authorization/hooks/useAuthorizationManagement'
import { useExplainAuthorizationAccess } from '../features/authorization/hooks/useAuthorizationDelegation'
import type {
    AuthorizationAccessContextType,
    AuthorizationAssignmentReferenceData,
    AuthorizationExplainAccessResult,
} from '../features/authorization/types/authorization'

const contextLabels: Record<AuthorizationAccessContextType, string> = {
    TENANT: 'Tenant',
    USER: 'User',
    PROJECT: 'Project',
    ORGANIZATIONAL_UNIT: 'Organizational unit',
    ORGANIZATIONAL_SUBTREE: 'Organizational subtree',
    DIRECT_REPORTS_ANCHOR: 'Direct-reports anchor',
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function formatDateTime(value: string | null): string {
    if (!value) {
        return 'No expiry'
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

function readableReason(value: string): string {
    return value
        .toLowerCase()
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ')
}

function getTargetOptions(
    contextType: AuthorizationAccessContextType,
    referenceData: AuthorizationAssignmentReferenceData | undefined,
) {
    if (!referenceData) {
        return []
    }

    switch (contextType) {
        case 'USER':
            return referenceData.users.map((user) => ({
                id: user.id,
                label: `${user.fullName} (${user.email})`,
            }))
        case 'PROJECT':
            return referenceData.projects
        case 'ORGANIZATIONAL_UNIT':
        case 'ORGANIZATIONAL_SUBTREE':
            return referenceData.organizationalUnits
        case 'DIRECT_REPORTS_ANCHOR':
            return referenceData.directReportsAnchors
        case 'TENANT':
            return []
    }
}

function ExplainResult({ result }: { result: AuthorizationExplainAccessResult }) {
    return (
        <Paper sx={{ marginTop: 3, padding: 3 }} variant="outlined">
            <Stack spacing={2}>
                <Stack
                    direction={{ xs: 'column', sm: 'row' }}
                    spacing={1}
                    sx={{ alignItems: { sm: 'center' }, justifyContent: 'space-between' }}
                >
                    <Box>
                        <Typography component="h2" variant="h6">
                            Access decision
                        </Typography>
                        <Typography color="text.secondary" variant="body2">
                            Evaluated {formatDateTime(result.evaluatedAt)}
                        </Typography>
                    </Box>
                    <Chip
                        color={result.granted ? 'success' : 'error'}
                        label={result.granted ? 'Granted' : 'Denied'}
                    />
                </Stack>

                <Alert severity={result.granted ? 'success' : 'warning'}>
                    {readableReason(result.reason)}
                </Alert>

                {result.matchedGrant && (
                    <>
                        <Divider />
                        <Stack spacing={1}>
                            <Typography sx={{ fontWeight: 700 }}>Matched authority</Typography>
                            <Typography variant="body2">
                                Role: {result.matchedGrant.roleCode}
                            </Typography>
                            <Typography variant="body2">
                                Scope: {result.matchedGrant.scopeType}
                            </Typography>
                            <Typography variant="body2">
                                Validity: {formatDateTime(result.matchedGrant.validFrom)} to{' '}
                                {formatDateTime(result.matchedGrant.validUntil)}
                            </Typography>
                            <Typography variant="body2">
                                Source:{' '}
                                <strong>
                                    {result.matchedGrant.grantSource === 'DELEGATED'
                                        ? 'Delegated authority'
                                        : 'Direct assignment'}
                                </strong>
                            </Typography>
                            {result.matchedGrant.grantSource === 'DELEGATED' && (
                                <Alert severity="info">
                                    Delegated by {result.matchedGrant.delegatorEmail ?? 'unknown'} ·
                                    delegation {result.matchedGrant.delegationId ?? 'unavailable'} ·
                                    parent assignment{' '}
                                    {result.matchedGrant.parentAssignmentId ?? 'unavailable'}
                                </Alert>
                            )}
                        </Stack>
                    </>
                )}
            </Stack>
        </Paper>
    )
}

export function AuthorizationExplainAccessPage() {
    const { session } = useAuth()
    const tenantId = session?.tenantId ?? ''
    const referenceDataQuery = useAuthorizationAssignmentReferenceData(tenantId)
    const permissionsQuery = useAuthorizationPermissions(tenantId)
    const explainMutation = useExplainAuthorizationAccess(tenantId)
    const [userId, setUserId] = useState('')
    const [permissionCode, setPermissionCode] = useState('')
    const [contextType, setContextType] = useState<AuthorizationAccessContextType>('TENANT')
    const [targetId, setTargetId] = useState('')
    const [effectiveAt, setEffectiveAt] = useState('')
    const [validationError, setValidationError] = useState<string | null>(null)

    const targetOptions = getTargetOptions(contextType, referenceDataQuery.data)
    const requiresTarget = contextType !== 'TENANT'
    const dataError = referenceDataQuery.error ?? permissionsQuery.error
    const dataLoading = referenceDataQuery.isPending || permissionsQuery.isPending

    const submit = (): void => {
        setValidationError(null)

        if (!userId || !permissionCode) {
            setValidationError('Choose a subject user and permission.')
            return
        }
        if (requiresTarget && !targetId) {
            setValidationError('Choose the target for this access context.')
            return
        }

        const effectiveAtIso = effectiveAt ? new Date(effectiveAt).toISOString() : null
        explainMutation.mutate({
            userId,
            permissionCode,
            contextType,
            targetId: requiresTarget ? targetId : null,
            effectiveAt: effectiveAtIso,
        })
    }

    return (
        <Box>
            <Typography component="h1" variant="h4">
                Explain access
            </Typography>
            <Typography color="text.secondary" sx={{ marginTop: 0.5 }}>
                Evaluate the same authorization engine used for enforcement and inspect the matched
                grant provenance.
            </Typography>

            {dataError && (
                <Alert severity="error" sx={{ marginTop: 2 }}>
                    {getErrorMessage(dataError, 'Authorization reference data could not be loaded.')}
                </Alert>
            )}

            {dataLoading && !dataError && (
                <Paper role="status" sx={{ marginTop: 2, padding: 4 }} variant="outlined">
                    <CircularProgress aria-label="Loading access explanation options" />
                </Paper>
            )}

            {!dataLoading && !dataError && (
                <Paper sx={{ marginTop: 2, padding: 3 }} variant="outlined">
                    <Stack spacing={2}>
                        <TextField
                            label="Subject user"
                            onChange={(event) => {
                                setUserId(event.target.value)
                            }}
                            select
                            value={userId}
                        >
                            {(referenceDataQuery.data?.users ?? []).map((user) => (
                                <MenuItem key={user.id} value={user.id}>
                                    {user.fullName} ({user.email})
                                </MenuItem>
                            ))}
                        </TextField>

                        <TextField
                            label="Permission"
                            onChange={(event) => {
                                setPermissionCode(event.target.value)
                            }}
                            select
                            value={permissionCode}
                        >
                            {(permissionsQuery.data ?? [])
                                .filter((permission) => permission.status === 'ACTIVE')
                                .map((permission) => (
                                    <MenuItem key={permission.id} value={permission.code}>
                                        {permission.name} ({permission.code})
                                    </MenuItem>
                                ))}
                        </TextField>

                        <TextField
                            label="Access context"
                            onChange={(event) => {
                                setContextType(event.target.value as AuthorizationAccessContextType)
                                setTargetId('')
                            }}
                            select
                            value={contextType}
                        >
                            {Object.entries(contextLabels).map(([value, label]) => (
                                <MenuItem key={value} value={value}>
                                    {label}
                                </MenuItem>
                            ))}
                        </TextField>

                        {requiresTarget && (
                            <TextField
                                label="Context target"
                                onChange={(event) => {
                                    setTargetId(event.target.value)
                                }}
                                select
                                value={targetId}
                            >
                                {targetOptions.map((option) => (
                                    <MenuItem key={option.id} value={option.id}>
                                        {option.label}
                                    </MenuItem>
                                ))}
                            </TextField>
                        )}

                        <TextField
                            helperText="Leave blank to evaluate at the current time."
                            label="Effective at"
                            onChange={(event) => {
                                setEffectiveAt(event.target.value)
                            }}
                            slotProps={{ inputLabel: { shrink: true } }}
                            type="datetime-local"
                            value={effectiveAt}
                        />

                        {(validationError || explainMutation.isError) && (
                            <Alert severity="error">
                                {validationError ??
                                    getErrorMessage(
                                        explainMutation.error,
                                        'Access could not be evaluated.',
                                    )}
                            </Alert>
                        )}

                        <Button
                            disabled={explainMutation.isPending}
                            onClick={submit}
                            startIcon={<SearchRoundedIcon />}
                            sx={{ alignSelf: 'flex-start' }}
                            variant="contained"
                        >
                            {explainMutation.isPending ? 'Evaluating…' : 'Explain access'}
                        </Button>
                    </Stack>
                </Paper>
            )}

            {explainMutation.data && <ExplainResult result={explainMutation.data} />}
        </Box>
    )
}
