import DevicesRoundedIcon from '@mui/icons-material/DevicesRounded'
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded'
import {
    Alert,
    Box,
    Button,
    Card,
    CardContent,
    Dialog,
    DialogActions,
    DialogContent,
    DialogContentText,
    DialogTitle,
    Stack,
    Typography,
} from '@mui/material'
import { useState } from 'react'
import { useNavigate } from 'react-router'

import { useLogoutAllDevices } from '../hooks/useAccount'
import { authStorage } from '../storage/authStorage'

interface SessionSecurityCardProps {
    tenantId: string
    email: string
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error
        ? error.message
        : 'Your device sessions could not be revoked.'
}

export function SessionSecurityCard({
    tenantId,
    email,
}: SessionSecurityCardProps) {
    const navigate = useNavigate()
    const mutation = useLogoutAllDevices()
    const [confirmationOpen, setConfirmationOpen] =
        useState(false)

    const openConfirmation = (): void => {
        mutation.reset()
        setConfirmationOpen(true)
    }

    const closeConfirmation = (): void => {
        if (mutation.isPending) {
            return
        }

        mutation.reset()
        setConfirmationOpen(false)
    }

    const logoutAllDevices = async (): Promise<void> => {
        try {
            await mutation.mutateAsync()

            authStorage.clear()
            navigate('/login', {
                replace: true,
                state: {
                    tenantId,
                    email,
                    allDevicesSignedOut: true,
                },
            })
        }
        catch {
            // The mutation error is rendered in the confirmation dialog.
        }
    }

    return (
        <>
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
                            <DevicesRoundedIcon color="primary" />
                            <Box>
                                <Typography
                                    component="h2"
                                    sx={{ fontWeight: 700 }}
                                    variant="h6"
                                >
                                    Device sessions
                                </Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Revoke every refresh session associated with your account.
                                </Typography>
                            </Box>
                        </Stack>

                        <Button
                            color="error"
                            onClick={openConfirmation}
                            startIcon={<LogoutRoundedIcon />}
                            variant="outlined"
                        >
                            Sign out all devices
                        </Button>
                    </Stack>
                </CardContent>
            </Card>

            <Dialog
                aria-labelledby="logout-all-devices-title"
                fullWidth
                maxWidth="sm"
                onClose={closeConfirmation}
                open={confirmationOpen}
            >
                <DialogTitle id="logout-all-devices-title">
                    Sign out all devices?
                </DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        Every active refresh token for {email} will be revoked. This browser will sign out immediately.
                    </DialogContentText>

                    <Alert severity="warning" sx={{ mt: 2 }}>
                        This invalidates existing tenant access and refresh sessions. Other browsers will return to sign in on their next server request.
                    </Alert>

                    {mutation.isError && (
                        <Alert severity="error" sx={{ mt: 2 }}>
                            {getErrorMessage(mutation.error)}
                        </Alert>
                    )}
                </DialogContent>
                <DialogActions sx={{ px: 3, pb: 2.5 }}>
                    <Button
                        disabled={mutation.isPending}
                        onClick={closeConfirmation}
                    >
                        Keep sessions
                    </Button>
                    <Button
                        color="error"
                        disabled={mutation.isPending}
                        onClick={() => {
                            void logoutAllDevices()
                        }}
                        variant="contained"
                    >
                        {mutation.isPending
                            ? 'Signing out…'
                            : 'Sign out all devices'}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
