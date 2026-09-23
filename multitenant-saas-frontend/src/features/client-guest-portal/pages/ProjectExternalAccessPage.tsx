import AddRoundedIcon from '@mui/icons-material/AddRounded'
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Chip,
    FormControlLabel,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { FormEvent } from 'react'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { externalAccessApi } from '../api/externalAccessApi'
import type { ExternalAccessGrant } from '../types/externalAccess'

function formatDate(value: string | null): string {
    if (!value) return '—'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function defaultExpiryValue(): string {
    const date = new Date(Date.now() + 7 * 24 * 60 * 60 * 1000)
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
    return local.toISOString().slice(0, 16)
}

function grantStateColor(
    state: ExternalAccessGrant['state'],
): 'default' | 'success' | 'warning' | 'error' {
    switch (state) {
        case 'ACCEPTED':
            return 'success'
        case 'INVITED':
            return 'warning'
        case 'EXPIRED':
        case 'REVOKED':
            return 'error'
        default:
            return 'default'
    }
}

export function ProjectExternalAccessPage() {
    const { session } = useAuth()
    const { projectId = '' } = useParams()
    const tenantId = session?.tenantId ?? ''
    const queryClient = useQueryClient()

    const [guestName, setGuestName] = useState('')
    const [guestEmail, setGuestEmail] = useState('')
    const [expiresAt, setExpiresAt] = useState(defaultExpiryValue)
    const [shareTasks, setShareTasks] = useState(true)
    const [inviteLink, setInviteLink] = useState<string | null>(null)
    const [feedback, setFeedback] = useState<string | null>(null)

    const queryKey = useMemo(
        () => ['external-access-grants', tenantId, projectId] as const,
        [tenantId, projectId],
    )

    const grantsQuery = useQuery({
        queryKey,
        queryFn: () => externalAccessApi.listGrants(tenantId, projectId),
        enabled: Boolean(tenantId && projectId),
    })

    const createMutation = useMutation({
        mutationFn: () =>
            externalAccessApi.createGrant(tenantId, projectId, {
                guestName: guestName.trim(),
                guestEmail: guestEmail.trim(),
                expiresAt: new Date(expiresAt).toISOString(),
                capabilities: shareTasks ? ['PROJECT_READ', 'TASK_READ'] : ['PROJECT_READ'],
            }),
        onSuccess: async (result) => {
            const link = `${window.location.origin}/guest#token=${encodeURIComponent(
                result.invitationToken,
            )}`
            setInviteLink(link)
            setFeedback('External access created. Copy the one-time guest link now.')
            setGuestName('')
            setGuestEmail('')
            setExpiresAt(defaultExpiryValue())
            setShareTasks(true)
            await queryClient.invalidateQueries({ queryKey })
        },
    })

    const revokeMutation = useMutation({
        mutationFn: (grantId: string) =>
            externalAccessApi.revokeGrant(tenantId, projectId, grantId),
        onSuccess: async () => {
            setFeedback('External access revoked. Existing guest sessions are no longer valid.')
            await queryClient.invalidateQueries({ queryKey })
        },
    })

    const submit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setInviteLink(null)
        setFeedback(null)
        createMutation.mutate()
    }

    const copyInviteLink = async (): Promise<void> => {
        if (!inviteLink) return
        await navigator.clipboard.writeText(inviteLink)
        setFeedback('Guest link copied.')
    }

    return (
        <Stack spacing={3}>
            <Stack spacing={0.5}>
                <Button
                    component={Link}
                    startIcon={<ArrowBackRoundedIcon />}
                    sx={{ alignSelf: 'flex-start' }}
                    to={`/projects/${projectId}`}
                >
                    Back to project
                </Button>
                <Typography component="h1" variant="h4">
                    Client / Guest Access
                </Typography>
                <Typography color="text.secondary">
                    Share a bounded project view without adding the recipient as a tenant member.
                </Typography>
            </Stack>

            <Alert severity="info">
                Guest access is separate from tenant membership and RBAC. This first slice can share
                project summary information and, optionally, the project task list.
            </Alert>

            <Paper component="form" onSubmit={submit} variant="outlined" sx={{ padding: 3 }}>
                <Stack spacing={2}>
                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                        <TextField
                            fullWidth
                            label="Guest name"
                            onChange={(event) => setGuestName(event.target.value)}
                            required
                            value={guestName}
                        />
                        <TextField
                            fullWidth
                            label="Guest email"
                            onChange={(event) => setGuestEmail(event.target.value)}
                            required
                            type="email"
                            value={guestEmail}
                        />
                    </Stack>

                    <TextField
                        label="Access expires"
                        onChange={(event) => setExpiresAt(event.target.value)}
                        required
                        slotProps={{ inputLabel: { shrink: true } }}
                        type="datetime-local"
                        value={expiresAt}
                    />

                    <FormControlLabel
                        control={
                            <Checkbox
                                checked={shareTasks}
                                onChange={(event) => setShareTasks(event.target.checked)}
                            />
                        }
                        label="Share project tasks"
                    />

                    <Box>
                        <Button
                            disabled={
                                createMutation.isPending ||
                                !guestName.trim() ||
                                !guestEmail.trim() ||
                                !expiresAt
                            }
                            startIcon={<AddRoundedIcon />}
                            type="submit"
                            variant="contained"
                        >
                            Create guest access
                        </Button>
                    </Box>

                    {createMutation.isError ? (
                        <Alert severity="error">
                            {createMutation.error instanceof Error
                                ? createMutation.error.message
                                : 'External access could not be created.'}
                        </Alert>
                    ) : null}

                    {inviteLink ? (
                        <Alert
                            action={
                                <Button
                                    color="inherit"
                                    onClick={() => void copyInviteLink()}
                                    startIcon={<ContentCopyRoundedIcon />}
                                    size="small"
                                >
                                    Copy
                                </Button>
                            }
                            severity="warning"
                        >
                            This one-time invitation secret is only returned now. Store or send the
                            link before leaving this page.
                            <TextField
                                fullWidth
                                margin="dense"
                                slotProps={{ input: { readOnly: true } }}
                                value={inviteLink}
                            />
                        </Alert>
                    ) : null}
                </Stack>
            </Paper>

            {feedback ? (
                <Alert onClose={() => setFeedback(null)} severity="success">
                    {feedback}
                </Alert>
            ) : null}

            <Stack spacing={1}>
                <Typography component="h2" variant="h5">
                    Existing guest access
                </Typography>
                <Typography color="text.secondary" variant="body2">
                    Revoking a grant immediately invalidates any guest session issued from it.
                </Typography>
            </Stack>

            {grantsQuery.isPending ? (
                <Typography color="text.secondary">Loading guest access…</Typography>
            ) : null}

            {grantsQuery.isError ? (
                <Alert
                    action={
                        <Button
                            color="inherit"
                            onClick={() => void grantsQuery.refetch()}
                            size="small"
                        >
                            Retry
                        </Button>
                    }
                    severity="error"
                >
                    Guest access could not be loaded.
                </Alert>
            ) : null}

            {grantsQuery.data?.content.length === 0 ? (
                <Alert severity="info">No external access grants have been created yet.</Alert>
            ) : null}

            <Stack spacing={2}>
                {grantsQuery.data?.content.map((grant) => (
                    <Paper key={grant.id} variant="outlined" sx={{ padding: 2 }}>
                        <Stack
                            direction={{ xs: 'column', md: 'row' }}
                            spacing={2}
                            sx={{ justifyContent: 'space-between' }}
                        >
                            <Stack spacing={1}>
                                <Stack
                                    direction="row"
                                    spacing={1}
                                    useFlexGap
                                    sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                                >
                                    <Typography sx={{ fontWeight: 700 }}>
                                        {grant.guestName}
                                    </Typography>
                                    <Chip
                                        color={grantStateColor(grant.state)}
                                        label={grant.state}
                                        size="small"
                                    />
                                </Stack>
                                <Typography color="text.secondary" variant="body2">
                                    {grant.guestEmail}
                                </Typography>
                                <Stack
                                    direction="row"
                                    spacing={1}
                                    useFlexGap
                                    sx={{ flexWrap: 'wrap' }}
                                >
                                    {grant.capabilities.map((capability) => (
                                        <Chip
                                            key={capability}
                                            label={
                                                capability === 'PROJECT_READ'
                                                    ? 'Project summary'
                                                    : 'Tasks'
                                            }
                                            size="small"
                                            variant="outlined"
                                        />
                                    ))}
                                </Stack>
                                <Typography color="text.secondary" variant="caption">
                                    Expires {formatDate(grant.expiresAt)}
                                    {grant.acceptedAt
                                        ? ` · accepted ${formatDate(grant.acceptedAt)}`
                                        : ''}
                                </Typography>
                            </Stack>

                            <Button
                                color="error"
                                disabled={grant.state === 'REVOKED' || revokeMutation.isPending}
                                onClick={() => revokeMutation.mutate(grant.id)}
                                startIcon={<DeleteOutlineRoundedIcon />}
                                sx={{ alignSelf: { md: 'flex-start' } }}
                                variant="outlined"
                            >
                                Revoke
                            </Button>
                        </Stack>
                    </Paper>
                ))}
            </Stack>

            {revokeMutation.isError ? (
                <Alert severity="error">
                    {revokeMutation.error instanceof Error
                        ? revokeMutation.error.message
                        : 'External access could not be revoked.'}
                </Alert>
            ) : null}
        </Stack>
    )
}
