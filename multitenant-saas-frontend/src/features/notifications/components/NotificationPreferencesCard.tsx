import EmailOutlinedIcon from '@mui/icons-material/EmailOutlined'
import NotificationsActiveRoundedIcon from '@mui/icons-material/NotificationsActiveRounded'
import {
    Alert,
    Box,
    Card,
    CardContent,
    Chip,
    CircularProgress,
    Divider,
    FormControlLabel,
    Stack,
    Switch,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import {
    useNotificationPreferences,
    useUpdateNotificationPreference,
} from '../hooks/useNotifications'
import type { NotificationType } from '../types/notifications'

const preferenceLabels: Record<NotificationType, { title: string; description: string }> = {
    TASK_ASSIGNED: {
        title: 'Task assignments',
        description: 'When another teammate assigns or reassigns a task to you.',
    },
    TASK_STATUS_CHANGED: {
        title: 'Task status changes',
        description: 'When someone else changes the status of a task assigned to you.',
    },
    TASK_COMMENT_ADDED: {
        title: 'Task comments',
        description: 'When a teammate adds a top-level comment to a task assigned to you.',
    },
    TASK_COMMENT_REPLIED: {
        title: 'Comment replies',
        description: 'When a teammate replies to one of your task comments.',
    },
    TASK_COMMENT_MENTIONED: {
        title: 'Mentions',
        description: 'When a teammate mentions you in a task comment or reply.',
    },
    PROJECT_MEMBERSHIP_CHANGED: {
        title: 'Project membership',
        description: 'Changes to your project membership or project role.',
    },
    WORKSPACE_INVITATION: {
        title: 'Workspace invitations',
        description: 'Workspace invitation and onboarding notifications.',
    },
    SECURITY_ALERT: {
        title: 'Security alerts',
        description: 'Security-critical account and session notifications.',
    },
}

function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : 'Notification preferences could not be updated.'
}

export function NotificationPreferencesCard({ tenantId }: { tenantId: string }) {
    const preferencesQuery = useNotificationPreferences(tenantId)
    const updatePreference = useUpdateNotificationPreference(tenantId)
    const [error, setError] = useState<string | null>(null)
    const [updatingType, setUpdatingType] = useState<NotificationType | null>(null)

    const toggleEmail = async (type: NotificationType, emailEnabled: boolean) => {
        setError(null)
        setUpdatingType(type)
        try {
            await updatePreference.mutateAsync({ type, input: { emailEnabled } })
        } catch (mutationError) {
            setError(getErrorMessage(mutationError))
        } finally {
            setUpdatingType(null)
        }
    }

    return (
        <Card variant="outlined">
            <CardContent sx={{ p: { xs: 2.5, sm: 3 } }}>
                <Stack spacing={2.5}>
                    <Stack direction="row" spacing={1.5} sx={{ alignItems: 'center' }}>
                        <NotificationsActiveRoundedIcon color="primary" />
                        <Box>
                            <Typography component="h2" sx={{ fontWeight: 700 }} variant="h6">
                                Notifications
                            </Typography>
                            <Typography color="text.secondary" variant="body2">
                                In-app notifications always stay available. Choose which events also
                                send email.
                            </Typography>
                        </Box>
                    </Stack>

                    <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }} useFlexGap>
                        <Chip label="In-app · always on" size="small" variant="outlined" />
                        <Chip
                            icon={<EmailOutlinedIcon />}
                            label="Email · configurable"
                            size="small"
                            variant="outlined"
                        />
                    </Stack>

                    <Divider />

                    {error && (
                        <Alert onClose={() => setError(null)} severity="error">
                            {error}
                        </Alert>
                    )}

                    {preferencesQuery.isPending && (
                        <Stack
                            aria-label="Loading notification preferences"
                            direction="row"
                            spacing={1.5}
                            sx={{ alignItems: 'center', py: 2 }}
                        >
                            <CircularProgress size={20} />
                            <Typography color="text.secondary" variant="body2">
                                Loading notification preferences…
                            </Typography>
                        </Stack>
                    )}

                    {preferencesQuery.isError && (
                        <Alert severity="error">{getErrorMessage(preferencesQuery.error)}</Alert>
                    )}

                    {preferencesQuery.data && (
                        <Stack divider={<Divider flexItem />} spacing={0}>
                            {preferencesQuery.data.map((preference) => {
                                const copy = preferenceLabels[preference.type]
                                const updating = updatingType === preference.type

                                return (
                                    <Stack
                                        direction={{ xs: 'column', sm: 'row' }}
                                        key={preference.type}
                                        spacing={1.5}
                                        sx={{
                                            alignItems: { sm: 'center' },
                                            justifyContent: 'space-between',
                                            py: 1.5,
                                        }}
                                    >
                                        <Box sx={{ pr: { sm: 2 } }}>
                                            <Typography sx={{ fontWeight: 600 }} variant="body2">
                                                {copy.title}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {copy.description}
                                            </Typography>
                                        </Box>

                                        <FormControlLabel
                                            control={
                                                <Switch
                                                    checked={preference.emailEnabled}
                                                    disabled={
                                                        !preference.emailConfigurable ||
                                                        updatePreference.isPending
                                                    }
                                                    slotProps={{
                                                        input: {
                                                            'aria-label': `${copy.title} email notifications`,
                                                        },
                                                    }}
                                                    onChange={(event) => {
                                                        void toggleEmail(
                                                            preference.type,
                                                            event.target.checked,
                                                        )
                                                    }}
                                                />
                                            }
                                            label={
                                                !preference.emailConfigurable
                                                    ? 'Required'
                                                    : updating
                                                      ? 'Saving…'
                                                      : 'Email'
                                            }
                                            labelPlacement="start"
                                            sx={{ m: 0, minWidth: 120 }}
                                        />
                                    </Stack>
                                )
                            })}
                        </Stack>
                    )}
                </Stack>
            </CardContent>
        </Card>
    )
}
