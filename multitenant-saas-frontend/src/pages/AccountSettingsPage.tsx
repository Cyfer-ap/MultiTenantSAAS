import AccountCircleRoundedIcon from '@mui/icons-material/AccountCircleRounded'
import BusinessRoundedIcon from '@mui/icons-material/BusinessRounded'
import LockRoundedIcon from '@mui/icons-material/LockRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Divider,
    Stack,
    Typography,
} from '@mui/material'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router'

import { SessionSecurityCard } from '../features/auth/components/SessionSecurityCard'
import { useCurrentUser } from '../features/auth/hooks/useAccount'
import type {
    TenantRole,
    UserStatus,
} from '../features/auth/types/auth'

const roleLabels: Record<TenantRole, string> = {
    TENANT_ADMIN: 'Tenant administrator',
    TENANT_MANAGER: 'Tenant manager',
    TENANT_USER: 'Tenant user',
}

const statusLabels: Record<UserStatus, string> = {
    ACTIVE: 'Active',
    INACTIVE: 'Inactive',
    SUSPENDED: 'Suspended',
}

function getInitials(fullName: string): string {
    return fullName
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map((part) => part.charAt(0).toUpperCase())
        .join('') || 'U'
}

function getStatusColor(
    status: UserStatus,
): 'default' | 'success' | 'warning' {
    if (status === 'ACTIVE') {
        return 'success'
    }

    if (status === 'SUSPENDED') {
        return 'warning'
    }

    return 'default'
}

function InformationItem({
    label,
    value,
}: {
    label: string
    value: ReactNode
}) {
    return (
        <Box>
            <Typography
                color="text.secondary"
                component="dt"
                variant="caption"
            >
                {label}
            </Typography>
            <Typography
                component="dd"
                sx={{
                    fontWeight: 600,
                    m: 0,
                    mt: 0.5,
                    overflowWrap: 'anywhere',
                }}
                variant="body2"
            >
                {value}
            </Typography>
        </Box>
    )
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'Your account details could not be loaded.'
}

export function AccountSettingsPage() {
    const navigate = useNavigate()
    const currentUserQuery = useCurrentUser()
    const currentUser = currentUserQuery.data

    return (
        <Box>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { sm: 'center' },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h1" variant="h4">
                        Account settings
                    </Typography>
                    <Typography color="text.secondary" sx={{ mt: 0.5 }}>
                        Review your workspace identity and manage account security.
                    </Typography>
                </Box>

                <Button
                    disabled={currentUserQuery.isFetching}
                    onClick={() => {
                        void currentUserQuery.refetch()
                    }}
                    startIcon={<RefreshRoundedIcon />}
                    variant="outlined"
                >
                    Refresh
                </Button>
            </Stack>

            {currentUserQuery.isPending && (
                <Card sx={{ mt: 3 }} variant="outlined">
                    <CardContent
                        sx={{
                            display: 'grid',
                            minHeight: 220,
                            placeItems: 'center',
                        }}
                    >
                        <CircularProgress aria-label="Loading account settings" />
                    </CardContent>
                </Card>
            )}

            {currentUserQuery.isError && (
                <Alert
                    action={(
                        <Button
                            color="inherit"
                            onClick={() => {
                                void currentUserQuery.refetch()
                            }}
                            size="small"
                        >
                            Retry
                        </Button>
                    )}
                    severity="error"
                    sx={{ mt: 3 }}
                >
                    {getErrorMessage(currentUserQuery.error)}
                </Alert>
            )}

            {currentUser && (
                <Box
                    sx={{
                        display: 'grid',
                        gap: 3,
                        gridTemplateColumns: {
                            xs: '1fr',
                            lg: 'minmax(0, 1fr) minmax(0, 1fr)',
                        },
                        mt: 3,
                    }}
                >
                    <Card variant="outlined">
                        <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                            <Stack spacing={3}>
                                <Stack
                                    direction="row"
                                    spacing={2}
                                    sx={{ alignItems: 'center' }}
                                >
                                    <Avatar
                                        sx={{
                                            bgcolor: 'primary.main',
                                            height: 56,
                                            width: 56,
                                        }}
                                    >
                                        {getInitials(currentUser.fullName)}
                                    </Avatar>
                                    <Box sx={{ minWidth: 0 }}>
                                        <Typography
                                            component="h2"
                                            sx={{ fontWeight: 700 }}
                                            variant="h6"
                                        >
                                            {currentUser.fullName}
                                        </Typography>
                                        <Typography
                                            color="text.secondary"
                                            sx={{ overflowWrap: 'anywhere' }}
                                            variant="body2"
                                        >
                                            {currentUser.email}
                                        </Typography>
                                    </Box>
                                </Stack>

                                <Stack
                                    direction="row"
                                    spacing={1}
                                    sx={{ flexWrap: 'wrap' }}
                                    useFlexGap
                                >
                                    <Chip
                                        icon={<AccountCircleRoundedIcon />}
                                        label={roleLabels[currentUser.role]}
                                        size="small"
                                        variant="outlined"
                                    />
                                    <Chip
                                        color={getStatusColor(currentUser.status)}
                                        label={statusLabels[currentUser.status]}
                                        size="small"
                                    />
                                </Stack>

                                <Divider />

                                <Box
                                    component="dl"
                                    sx={{
                                        display: 'grid',
                                        gap: 2.5,
                                        gridTemplateColumns: {
                                            xs: '1fr',
                                            sm: 'repeat(2, minmax(0, 1fr))',
                                        },
                                        m: 0,
                                    }}
                                >
                                    <InformationItem
                                        label="Full name"
                                        value={currentUser.fullName}
                                    />
                                    <InformationItem
                                        label="Email address"
                                        value={currentUser.email}
                                    />
                                    <InformationItem
                                        label="Role"
                                        value={roleLabels[currentUser.role]}
                                    />
                                    <InformationItem
                                        label="User ID"
                                        value={currentUser.userId}
                                    />
                                </Box>

                                <Alert severity="info">
                                    Name, email, role, and status changes use your workspace user-management controls.
                                </Alert>
                            </Stack>
                        </CardContent>
                    </Card>

                    <Stack spacing={3}>
                        <Card variant="outlined">
                            <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                                <Stack spacing={2.5}>
                                    <Stack
                                        direction="row"
                                        spacing={1.5}
                                        sx={{ alignItems: 'center' }}
                                    >
                                        <BusinessRoundedIcon color="primary" />
                                        <Box>
                                            <Typography
                                                component="h2"
                                                sx={{ fontWeight: 700 }}
                                                variant="h6"
                                            >
                                                Workspace
                                            </Typography>
                                            <Typography color="text.secondary" variant="body2">
                                                Tenant context attached to this account.
                                            </Typography>
                                        </Box>
                                    </Stack>

                                    <Divider />

                                    <Box
                                        component="dl"
                                        sx={{
                                            display: 'grid',
                                            gap: 2.5,
                                            gridTemplateColumns: {
                                                xs: '1fr',
                                                sm: 'repeat(2, minmax(0, 1fr))',
                                            },
                                            m: 0,
                                        }}
                                    >
                                        <InformationItem
                                            label="Workspace name"
                                            value={currentUser.tenantName}
                                        />
                                        <InformationItem
                                            label="Workspace slug"
                                            value={currentUser.tenantSlug}
                                        />
                                        <InformationItem
                                            label="Tenant ID"
                                            value={currentUser.tenantId}
                                        />
                                    </Box>
                                </Stack>
                            </CardContent>
                        </Card>

                        <Card variant="outlined">
                            <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                                <Stack
                                    direction={{ xs: 'column', sm: 'row' }}
                                    spacing={2}
                                    sx={{
                                        alignItems: { sm: 'center' },
                                        justifyContent: 'space-between',
                                    }}
                                >
                                    <Stack
                                        direction="row"
                                        spacing={1.5}
                                        sx={{ alignItems: 'center' }}
                                    >
                                        <LockRoundedIcon color="primary" />
                                        <Box>
                                            <Typography
                                                component="h2"
                                                sx={{ fontWeight: 700 }}
                                                variant="h6"
                                            >
                                                Password
                                            </Typography>
                                            <Typography color="text.secondary" variant="body2">
                                                Change your password and revoke every refresh session.
                                            </Typography>
                                        </Box>
                                    </Stack>

                                    <Button
                                        onClick={() => {
                                            navigate('/account/change-password')
                                        }}
                                        variant="contained"
                                    >
                                        Change password
                                    </Button>
                                </Stack>
                            </CardContent>
                        </Card>

                        <SessionSecurityCard
                            email={currentUser.email}
                            tenantId={currentUser.tenantId}
                        />
                    </Stack>
                </Box>
            )}
        </Box>
    )
}
