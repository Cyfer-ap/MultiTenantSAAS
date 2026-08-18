import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
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

    const download = async (attachment: TaskAttachment) => {
        setError(null)
        try {
            const response = await downloadMutation.mutateAsync(attachment.id)
            const anchor = document.createElement('a')
            anchor.href = response.downloadUrl
            anchor.target = '_blank'
            anchor.rel = 'noopener noreferrer'
            anchor.click()
        } catch (downloadError) {
            setError(errorMessage(downloadError, 'The attachment could not be downloaded.'))
        }
    }

    const remove = async (attachment: TaskAttachment) => {
        setError(null)
        try {
            await deleteMutation.mutateAsync(attachment.id)
            onFeedback('Attachment deleted.')
        } catch (deleteError) {
            setError(errorMessage(deleteError, 'The attachment could not be deleted.'))
        }
    }

    return (
        <Stack spacing={2}>
            {readOnly && (
                <Alert severity="info">
                    Attachments are read-only for this task. Existing files remain available.
                </Alert>
            )}

            {!readOnly && (
                <Paper variant="outlined" sx={{ padding: 2 }}>
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
                            Maximum size: 25 MiB. Files are stored privately and downloaded through
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
                    {errorMessage(attachmentsQuery.error, 'Task attachments could not be loaded.')}
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
                    <Paper key={attachment.id} sx={{ padding: 1.5 }} variant="outlined">
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
                                        onClick={() => void remove(attachment)}
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
    )
}
