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
    TextField,
    Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'

import { externalAccessApi } from '../api/externalAccessApi'
import type { GuestTask } from '../types/externalAccess'
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

function GuestTaskCard({
    task,
    sessionToken,
    canComment,
}: {
    task: GuestTask
    sessionToken: string
    canComment: boolean
}) {
    const queryClient = useQueryClient()
    const [commentsOpen, setCommentsOpen] = useState(false)
    const [body, setBody] = useState('')
    const commentQueryKey = ['guest-portal-comments', sessionToken, task.id] as const

    const commentsQuery = useQuery({
        queryKey: commentQueryKey,
        queryFn: () => externalAccessApi.getGuestTaskComments(sessionToken, task.id),
        enabled: canComment && commentsOpen,
        retry: false,
    })

    const createCommentMutation = useMutation({
        mutationFn: () =>
            externalAccessApi.createGuestTaskComment(sessionToken, task.id, body.trim()),
        onSuccess: async () => {
            setBody('')
            await queryClient.invalidateQueries({ queryKey: commentQueryKey })
        },
    })

    return (
        <Paper variant="outlined" sx={{ padding: 2 }}>
            <Stack spacing={1.5}>
                <Stack
                    direction="row"
                    spacing={1}
                    useFlexGap
                    sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                >
                    <Typography sx={{ fontWeight: 700 }}>{task.title}</Typography>
                    <Chip label={taskStatusLabel(task.status)} size="small" variant="outlined" />
                    <Chip label={task.priority.toLowerCase()} size="small" />
                </Stack>

                {task.description ? <Typography>{task.description}</Typography> : null}

                <Stack direction="row" spacing={2} useFlexGap sx={{ flexWrap: 'wrap' }}>
                    <Typography color="text.secondary" variant="caption">
                        Due {formatDate(task.dueAt)}
                    </Typography>
                    <Typography color="text.secondary" variant="caption">
                        Updated {formatDate(task.updatedAt)}
                    </Typography>
                </Stack>

                {canComment ? (
                    <>
                        <Box>
                            <Button
                                onClick={() => setCommentsOpen((current) => !current)}
                                size="small"
                                variant="outlined"
                            >
                                {commentsOpen ? 'Hide guest comments' : 'Guest comments'}
                            </Button>
                        </Box>

                        {commentsOpen ? (
                            <Stack spacing={1.5}>
                                {commentsQuery.isPending ? (
                                    <Typography color="text.secondary" variant="body2">
                                        Loading guest comments…
                                    </Typography>
                                ) : null}

                                {commentsQuery.isError ? (
                                    <Alert severity="error">
                                        Guest comments could not be loaded. The access grant may
                                        have changed or expired.
                                    </Alert>
                                ) : null}

                                {commentsQuery.data?.comments.length === 0 ? (
                                    <Alert severity="info">
                                        No guest comments have been added to this task yet.
                                    </Alert>
                                ) : null}

                                {commentsQuery.data?.comments.map((comment) => (
                                    <Paper
                                        key={comment.id}
                                        variant="outlined"
                                        sx={{ padding: 1.5 }}
                                    >
                                        <Stack spacing={0.5}>
                                            <Typography sx={{ fontWeight: 700 }} variant="body2">
                                                {comment.guestName}
                                            </Typography>
                                            <Typography
                                                sx={{ whiteSpace: 'pre-wrap' }}
                                                variant="body2"
                                            >
                                                {comment.body}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {formatDate(comment.createdAt)}
                                            </Typography>
                                        </Stack>
                                    </Paper>
                                ))}

                                <TextField
                                    label="Add a guest comment"
                                    maxRows={6}
                                    minRows={2}
                                    multiline
                                    onChange={(event) => setBody(event.target.value)}
                                    value={body}
                                />
                                <Box>
                                    <Button
                                        disabled={!body.trim() || createCommentMutation.isPending}
                                        onClick={() => createCommentMutation.mutate()}
                                        variant="contained"
                                    >
                                        Post comment
                                    </Button>
                                </Box>

                                {createCommentMutation.isError ? (
                                    <Alert severity="error">
                                        {createCommentMutation.error instanceof Error
                                            ? createCommentMutation.error.message
                                            : 'The guest comment could not be created.'}
                                    </Alert>
                                ) : null}
                            </Stack>
                        ) : null}
                    </>
                ) : null}
            </Stack>
        </Paper>
    )
}

export function GuestPortalPage() {
    const [invitationToken] = useState<string | null>(() => {
        if (typeof window === 'undefined') return null
        return new URLSearchParams(window.location.hash.slice(1)).get('token')
    })
    const [sessionToken, setSessionToken] = useState<string | null>(() =>
        invitationToken ? null : guestSessionStorage.read(),
    )
    const [exchangePending, setExchangePending] = useState(Boolean(invitationToken))
    const [accessError, setAccessError] = useState<string | null>(null)

    useEffect(() => {
        if (!invitationToken) {
            return
        }

        window.history.replaceState(
            null,
            '',
            `${window.location.pathname}${window.location.search}`,
        )

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
    }, [invitationToken])

    const sessionQuery = useQuery({
        queryKey: ['guest-portal-session', sessionToken],
        queryFn: () => externalAccessApi.getGuestSession(sessionToken ?? ''),
        enabled: Boolean(sessionToken),
        retry: false,
    })

    const canReadTasks = Boolean(sessionQuery.data?.capabilities.includes('TASK_READ'))
    const canComment = Boolean(sessionQuery.data?.capabilities.includes('TASK_COMMENT_CREATE'))

    const tasksQuery = useQuery({
        queryKey: ['guest-portal-tasks', sessionToken],
        queryFn: () => externalAccessApi.getGuestTasks(sessionToken ?? ''),
        enabled: Boolean(sessionToken && canReadTasks),
        retry: false,
    })

    useEffect(() => {
        if (sessionQuery.isError) {
            guestSessionStorage.clear()
        }
    }, [sessionQuery.isError])

    const visibleAccessError = sessionQuery.isError
        ? 'This guest session has expired or was revoked.'
        : accessError

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

                    {visibleAccessError ? (
                        <Alert
                            action={
                                sessionQuery.isError ? (
                                    <Button color="inherit" onClick={clearSession} size="small">
                                        Clear session
                                    </Button>
                                ) : undefined
                            }
                            severity="error"
                        >
                            {visibleAccessError} Ask the project team for a new access link if
                            needed.
                        </Alert>
                    ) : null}

                    {!exchangePending && !sessionToken && !visibleAccessError ? (
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
                                        {canComment ? (
                                            <Chip label="Guest comments" size="small" />
                                        ) : null}
                                    </Stack>

                                    <Typography color="text.secondary" variant="caption">
                                        Access expires{' '}
                                        {formatDate(sessionQuery.data.grantExpiresAt)}
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
                                        <GuestTaskCard
                                            canComment={canComment}
                                            key={task.id}
                                            sessionToken={sessionToken}
                                            task={task}
                                        />
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
