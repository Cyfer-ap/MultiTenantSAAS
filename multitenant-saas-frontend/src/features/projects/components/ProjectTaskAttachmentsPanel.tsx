import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
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
    IconButton,
    LinearProgress,
    MenuItem,
    Paper,
    Skeleton,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import type { ChangeEvent } from 'react'
import { useMemo, useState } from 'react'

import {
    useDeleteTaskAttachment,
    useTaskAttachmentDownload,
    useTaskAttachments,
    useUploadTaskAttachment,
} from '../hooks/useProjectTaskCollaboration'
import type { TaskAttachment, TaskComment } from '../types/taskCollaboration'

interface ProjectTaskAttachmentsPanelProps {
    tenantId: string
    projectId: string
    taskId: string
    currentUserId: string
    comments: TaskComment[]
    readOnly: boolean
    onFeedback: (message: string) => void
}

const MAX_ATTACHMENT_SIZE = 25 * 1024 * 1024

function formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDateTime(value: string): string {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function errorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function commentLabel(comment: TaskComment): string {
    const body = (comment.body ?? '').replace(/\s+/g, ' ').trim()
    if (!body) return 'Comment'
    return body.length > 48 ? `${body.slice(0, 48)}…` : body
}

export function ProjectTaskAttachmentsPanel({
    tenantId,
    projectId,
    taskId,
    currentUserId,
    comments,
    readOnly,
    onFeedback,
}: ProjectTaskAttachmentsPanelProps) {
    const [target, setTarget] = useState('task')
    const [error, setError] = useState<string | null>(null)
    const [deleteCandidate, setDeleteCandidate] = useState<TaskAttachment | null>(null)
    const pageParams = { page: 0, size: 50, sortDir: 'desc' as const }

    const attachmentsQuery = useTaskAttachments(tenantId, projectId, taskId, pageParams, true)
    const uploadMutation = useUploadTaskAttachment(tenantId, projectId, taskId)
    const deleteMutation = useDeleteTaskAttachment(tenantId, projectId, taskId)
    const downloadMutation = useTaskAttachmentDownload(tenantId, projectId, taskId)

    const attachableComments = useMemo(
        () =>
            comments.filter(
                (comment) => !comment.deleted && comment.authorUserId === currentUserId,
            ),
        [comments, currentUserId],
    )

    const selectFile = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.target.files?.[0]
        event.target.value = ''
        if (!file || readOnly || uploadMutation.isPending) return

        setError(null)
        if (file.size <= 0) {
            setError('Choose a non-empty file.')
            return
        }
        if (file.size > MAX_ATTACHMENT_SIZE) {
            setError('Attachments cannot exceed 25 MiB.')
            return
        }

        try {
            await uploadMutation.mutateAsync({
                file,
                commentId: target === 'task' ? null : target,
            })
            onFeedback('Attachment uploaded.')
        } catch (uploadError) {
            setError(errorMessage(uploadError, 'The attachment could not be uploaded.'))
        }
    }

    const view = async (attachment: TaskAttachment) => {
        setError(null)
        try {
            const response = await downloadMutation.mutateAsync(attachment.id)
            const anchor = document.createElement('a')
            anchor.href = response.downloadUrl
            anchor.target = '_blank'
            anchor.rel = 'noopener noreferrer'
            anchor.click()
        } catch (viewError) {
            setError(errorMessage(viewError, 'The attachment could not be opened.'))
        }
    }

    const download = async (attachment: TaskAttachment) => {
        setError(null)
        try {
            const response = await downloadMutation.mutateAsync(attachment.id)
            const fileResponse = await fetch(response.downloadUrl)
            if (!fileResponse.ok) {
                throw new Error(`Attachment download failed with status ${fileResponse.status}.`)
            }
            const objectUrl = URL.createObjectURL(await fileResponse.blob())
            const anchor = document.createElement('a')
            anchor.href = objectUrl
            anchor.download = attachment.filename
            anchor.click()
            URL.revokeObjectURL(objectUrl)
        } catch (downloadError) {
            setError(errorMessage(downloadError, 'The attachment could not be downloaded.'))
        }
    }

    const remove = async () => {
        if (!deleteCandidate) return
        setError(null)
        try {
            await deleteMutation.mutateAsync(deleteCandidate.id)
            setDeleteCandidate(null)
            onFeedback('Attachment deleted.')
        } catch (deleteError) {
            setError(errorMessage(deleteError, 'The attachment could not be deleted.'))
        }
    }

    return (
        <>
            <Stack spacing={2}>
                {readOnly && (
                    <Alert severity="info">
                        Attachments are read-only for this task. Existing files remain available.
                    </Alert>
                )}

                {!readOnly && (
                    <Paper
                        variant="outlined"
                        sx={(theme) => ({
                            background:
                                theme.palette.mode === 'dark'
                                    ? 'linear-gradient(145deg, rgba(53,92,135,0.14), rgba(83,57,118,0.08) 55%, rgba(17,24,39,0.12))'
                                    : 'linear-gradient(145deg, rgba(80,120,160,0.08), rgba(115,92,145,0.05))',
                            borderColor:
                                theme.palette.mode === 'dark'
                                    ? 'rgba(115,155,205,0.24)'
                                    : 'divider',
                            padding: 2,
                        })}
                    >
                        <Stack spacing={1.5}>
                            <Typography sx={{ fontWeight: 700 }} variant="subtitle2">
                                Add attachment
                            </Typography>
                            <TextField
                                label="Attach to"
                                onChange={(event) => setTarget(event.target.value)}
                                select
                                size="small"
                                value={target}
                            >
                                <MenuItem value="task">Task</MenuItem>
                                {attachableComments.map((comment) => (
                                    <MenuItem key={comment.id} value={comment.id}>
                                        Comment: {commentLabel(comment)}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <Button
                                component="label"
                                disabled={uploadMutation.isPending}
                                startIcon={
                                    uploadMutation.isPending ? (
                                        <CircularProgress color="inherit" size={16} />
                                    ) : (
                                        <AttachFileRoundedIcon />
                                    )
                                }
                                variant="contained"
                            >
                                {uploadMutation.isPending ? 'Uploading…' : 'Choose file'}
                                <input
                                    aria-label="Choose attachment file"
                                    hidden
                                    onChange={(event) => void selectFile(event)}
                                    type="file"
                                />
                            </Button>
                            <Typography color="text.secondary" variant="caption">
                                Maximum size: 25 MiB. Files are stored privately and shared through
                                short-lived links.
                            </Typography>
                            {uploadMutation.isPending && (
                                <LinearProgress aria-label="Uploading attachment" />
                            )}
                        </Stack>
                    </Paper>
                )}

                {error && (
                    <Alert onClose={() => setError(null)} severity="error">
                        {error}
                    </Alert>
                )}

                {attachmentsQuery.isPending && (
                    <Stack aria-label="Loading task attachments" role="status" spacing={1}>
                        <Skeleton height={82} variant="rounded" />
                        <Skeleton height={82} variant="rounded" />
                    </Stack>
                )}

                {attachmentsQuery.isError && (
                    <Alert severity="error">
                        {errorMessage(
                            attachmentsQuery.error,
                            'Task attachments could not be loaded.',
                        )}
                    </Alert>
                )}

                {attachmentsQuery.isSuccess && attachmentsQuery.data.content.length === 0 && (
                    <Box sx={{ padding: 3, textAlign: 'center' }}>
                        <InsertDriveFileOutlinedIcon color="disabled" />
                        <Typography sx={{ marginTop: 1 }} variant="subtitle2">
                            No attachments yet
                        </Typography>
                        <Typography color="text.secondary" variant="body2">
                            Add files that belong with this task or one of your comments.
                        </Typography>
                    </Box>
                )}

                {attachmentsQuery.data?.content.map((attachment) => {
                    const ownAttachment = attachment.uploaderUserId === currentUserId
                    return (
                        <Paper
                            key={attachment.id}
                            variant="outlined"
                            sx={(theme) => ({
                                background:
                                    theme.palette.mode === 'dark'
                                        ? attachment.commentId
                                            ? 'linear-gradient(135deg, rgba(111,76,145,0.11), rgba(255,255,255,0.018) 55%)'
                                            : 'linear-gradient(135deg, rgba(46,113,145,0.12), rgba(255,255,255,0.018) 55%)'
                                        : 'background.paper',
                                borderColor:
                                    theme.palette.mode === 'dark'
                                        ? attachment.commentId
                                            ? 'rgba(160,118,202,0.25)'
                                            : 'rgba(88,159,190,0.26)'
                                        : 'divider',
                                padding: 1.5,
                                transition: 'border-color 160ms ease, transform 160ms ease',
                                '&:hover': {
                                    borderColor:
                                        theme.palette.mode === 'dark'
                                            ? attachment.commentId
                                                ? 'rgba(178,132,220,0.42)'
                                                : 'rgba(99,184,217,0.44)'
                                            : 'text.disabled',
                                    transform: 'translateY(-1px)',
                                },
                            })}
                        >
                            <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
                                <InsertDriveFileOutlinedIcon color="action" />
                                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                    <Typography
                                        noWrap
                                        sx={{ fontWeight: 650 }}
                                        title={attachment.filename}
                                        variant="body2"
                                    >
                                        {attachment.filename}
                                    </Typography>
                                    <Typography color="text.secondary" variant="caption">
                                        {formatBytes(attachment.sizeBytes)} · {attachment.uploaderName}
                                        {' · '}
                                        {formatDateTime(attachment.createdAt)}
                                    </Typography>
                                    <Box sx={{ marginTop: 0.5 }}>
                                        <Chip
                                            label={
                                                attachment.commentId
                                                    ? 'Comment attachment'
                                                    : 'Task attachment'
                                            }
                                            size="small"
                                            variant="outlined"
                                        />
                                    </Box>
                                </Box>
                                <Tooltip title="View attachment">
                                    <IconButton
                                        aria-label={`View ${attachment.filename}`}
                                        disabled={downloadMutation.isPending}
                                        onClick={() => void view(attachment)}
                                        size="small"
                                    >
                                        <VisibilityOutlinedIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>
                                <Tooltip title="Download attachment">
                                    <IconButton
                                        aria-label={`Download ${attachment.filename}`}
                                        disabled={downloadMutation.isPending}
                                        onClick={() => void download(attachment)}
                                        size="small"
                                    >
                                        <DownloadRoundedIcon fontSize="small" />
                                    </IconButton>
                                </Tooltip>
                                {ownAttachment && !readOnly && (
                                    <Tooltip title="Delete attachment">
                                        <IconButton
                                            aria-label={`Delete ${attachment.filename}`}
                                            disabled={deleteMutation.isPending}
                                            onClick={() => setDeleteCandidate(attachment)}
                                            size="small"
                                        >
                                            <DeleteOutlineRoundedIcon fontSize="small" />
                                        </IconButton>
                                    </Tooltip>
                                )}
                            </Stack>
                        </Paper>
                    )
                })}

                {attachmentsQuery.data && !attachmentsQuery.data.last && (
                    <Alert severity="info">
                        Showing the latest {attachmentsQuery.data.content.length} attachments.
                    </Alert>
                )}
            </Stack>

            <Dialog
                maxWidth="xs"
                fullWidth
                onClose={() => !deleteMutation.isPending && setDeleteCandidate(null)}
                open={Boolean(deleteCandidate)}
            >
                <DialogTitle>Delete attachment?</DialogTitle>
                <DialogContent>
                    <Typography variant="body2">
                        {deleteCandidate
                            ? `“${deleteCandidate.filename}” will be removed from this task and from private storage.`
                            : 'This attachment will be removed.'}
                    </Typography>
                    <Alert severity="warning" sx={{ marginTop: 2 }}>
                        This action cannot be undone.
                    </Alert>
                </DialogContent>
                <DialogActions>
                    <Button
                        disabled={deleteMutation.isPending}
                        onClick={() => setDeleteCandidate(null)}
                    >
                        Cancel
                    </Button>
                    <Button
                        color="error"
                        disabled={deleteMutation.isPending}
                        onClick={() => void remove()}
                        variant="contained"
                    >
                        {deleteMutation.isPending ? 'Deleting…' : 'Delete file'}
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
