import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Container,
    Paper,
    Stack,
    Typography,
} from '@mui/material'
import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

import { externalAccessApi } from '../api/externalAccessApi'
import { guestSessionStorage } from '../storage/guestSessionStorage'

function formatDate(value: string | null): string {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function taskStatusLabel(status: string): string {
    return status.replaceAll('_', ' ').toLowerCase()
}

export function GuestPortalPage() {
    const [sessionToken, setSessionToken] = useState<string | null>(() =>
        guestSessionStorage.read(),
    )
    const [exchangePending, setExchangePending] = useState(false)
    const [accessError, setAccessError] = useState<string | null>(null)

    useEffect(() => {
        const invitationToken = new URLSearchParams(window.location.hash.slice(1)).get('token')

        if (!invitationToken) {
            return
        }

        window.history.replaceState(
            null,
            '',
            `${window.location.pathname}${window.location.search}`,
        )
        setExchangePending(true)
        setAccessError(null)

        let active = true

        void externalAccessApi
            .exchangeInvitation(invitationToken)
            .then((response) => {
                if (!active) return
                guestSessionStorage.write(response.sessionToken)
                setSessionToken(response.sessionToken)
            })
            .catch((error: unknown) => {
                if (!active) return
                guestSessionStorage.clear()
                setSessionToken(null)
                setAccessError(
                    error instanceof Error
                        ? error.message
                        : 'This guest invitation is invalid or no longer available.',
                )
            })
            .finally(() => {
                if (active) {
                    setExchangePending(false)
                }
            })

        return () => {
            active = false
        }
    }, [])

    const sessionQuery = useQuery({
        queryKey: ['guest-portal-session', sessionToken],
        queryFn: () => externalAccessApi.getGuestSession(sessionToken ?? ''),
        enabled: Boolean(sessionToken),
        retry: false,
    })

    const canReadTasks = Boolean(sessionQuery.data?.capabilities.includes('TASK_READ'))

    const tasksQuery = useQuery({
        queryKey: ['guest-portal-tasks', sessionToken],
        queryFn: () => externalAccessApi.getGuestTasks(sessionToken ?? ''),
        enabled: Boolean(sessionToken && canReadTasks),
        retry: false,
    })

    useEffect(() => {
        if (!sessionQuery.isError) return
        guestSessionStorage.clear()
        setSessionToken(null)
        setAccessError('This guest session has expired or was revoked.')
    }, [sessionQuery.isError])

    const clearSession = (): void => {
        guestSessionStorage.clear()
        setSessionToken(null)
        setAccessError(null)
    }

    return (
        <Box sx={{ backgroundColor: 'background.default', minHeight: '100vh', py: 5 }}>
            <Container maxWidth="md">
                <Stack spacing={3}>
                    <Stack spacing={0.5}>
                        <Typography component="h1" variant="h3">
                            Client / Guest Portal
                        </Typography>
                        <Typography color="text.secondary">
                            A bounded external view shared by the project team.
                        </Typography>
                    </Stack>

                    {exchangePending ? (
                        <Paper variant="outlined" sx={{ padding: 4, textAlign: 'center' }}>
                            <CircularProgress aria-label="Opening guest access" />
                            <Typography sx={{ marginTop: 2 }}>
                                Validating your guest invitation…
                            </Typography>
                        </Paper>
                    ) : null}

                    {accessError ? (
                        <Alert severity="error">
                            {accessError} Ask the project team for a new access link if needed.
                        </Alert>
                    ) : null}

                    {!exchangePending && !sessionToken && !accessError ? (
                        <Alert severity="info">
                            Open the one-time guest link supplied by the project team to access this
                            portal.
                        </Alert>
                    ) : null}

                    {sessionToken && sessionQuery.isPending ? (
                        <Paper variant="outlined" sx={{ padding: 4, textAlign: 'center' }}>
                            <CircularProgress aria-label="Loading guest project" />
                        </Paper>
                    ) : null}

                    {sessionQuery.data ? (
                        <>
                            <Paper variant="outlined" sx={{ padding: 3 }}>
                                <Stack spacing={2}>
                                    <Stack
                                        direction={{ xs: 'column', sm: 'row' }}
                                        spacing={1}
                                        sx={{
                                            alignItems: { sm: 'center' },
                                            justifyContent: 'space-between',
                                        }}
                                    >
                                        <Box>
                                            <Typography component="h2" variant="h4">
                                                {sessionQuery.data.project.name}
                                            </Typography>
                                            <Typography color="text.secondary">
                                                Shared with {sessionQuery.data.guestName}
                                            </Typography>
                                        </Box>
                                        <Chip
                                            label={sessionQuery.data.project.status
                                                .replaceAll('_', ' ')
                                                .toLowerCase()}
                                            variant="outlined"
                                        />
                                    </Stack>

                                    <Typography>
                                        {sessionQuery.data.project.description ||
                                            'No project description was shared.'}
                                    </Typography>

                                    <Stack
                                        direction="row"
                                        spacing={1}
                                        useFlexGap
                                        sx={{ flexWrap: 'wrap' }}
                                    >
                                        <Chip label="Project summary" size="small" />
                                        {canReadTasks ? <Chip label="Tasks" size="small" /> : null}
                                    </Stack>

                                    <Typography color="text.secondary" variant="caption">
                                        Access expires {formatDate(sessionQuery.data.grantExpiresAt)}
                                    </Typography>

                                    <Box>
                                        <Button color="inherit" onClick={clearSession} size="small">
                                            Clear this guest session
                                        </Button>
                                    </Box>
                                </Stack>
                            </Paper>

                            {canReadTasks ? (
                                <Stack spacing={2}>
                                    <Stack
                                        direction="row"
                                        spacing={1}
                                        sx={{
                                            alignItems: 'center',
                                            justifyContent: 'space-between',
                                        }}
                                    >
                                        <Typography component="h2" variant="h5">
                                            Shared tasks
                                        </Typography>
                                        <Button
                                            disabled={tasksQuery.isFetching}
                                            onClick={() => void tasksQuery.refetch()}
                                            startIcon={<RefreshRoundedIcon />}
                                            size="small"
                                            variant="outlined"
                                        >
                                            Refresh
                                        </Button>
                                    </Stack>

                                    {tasksQuery.isPending ? (
                                        <Box sx={{ py: 3, textAlign: 'center' }}>
                                            <CircularProgress aria-label="Loading shared tasks" />
                                        </Box>
                                    ) : null}

                                    {tasksQuery.isError ? (
                                        <Alert severity="error">
                                            Shared tasks could not be loaded. The grant may have
                                            changed or expired.
                                        </Alert>
                                    ) : null}

                                    {tasksQuery.data?.tasks.length === 0 ? (
                                        <Alert severity="info">
                                            No tasks are currently available in the shared view.
                                        </Alert>
                                    ) : null}

                                    {tasksQuery.data?.tasks.map((task) => (
                                        <Paper key={task.id} variant="outlined" sx={{ padding: 2 }}>
                                            <Stack spacing={1}>
                                                <Stack
                                                    direction="row"
                                                    spacing={1}
                                                    useFlexGap
                                                    sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                                                >
                                                    <Typography sx={{ fontWeight: 700 }}>
                                                        {task.title}
                                                    </Typography>
                                                    <Chip
                                                        label={taskStatusLabel(task.status)}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                    <Chip
                                                        label={task.priority.toLowerCase()}
                                                        size="small"
                                                    />
                                                </Stack>
                                                {task.description ? (
                                                    <Typography>{task.description}</Typography>
                                                ) : null}
                                                <Stack
                                                    direction="row"
                                                    spacing={2}
                                                    useFlexGap
                                                    sx={{ flexWrap: 'wrap' }}
                                                >
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        Due {formatDate(task.dueAt)}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        Updated {formatDate(task.updatedAt)}
                                                    </Typography>
                                                </Stack>
                                            </Stack>
                                        </Paper>
                                    ))}
                                </Stack>
                            ) : (
                                <Alert severity="info">
                                    This guest access does not include the project task list.
                                </Alert>
                            )}
                        </>
                    ) : null}
                </Stack>
            </Container>
        </Box>
    )
}
