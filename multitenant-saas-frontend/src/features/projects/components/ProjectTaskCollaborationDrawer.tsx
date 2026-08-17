import AlternateEmailRoundedIcon from '@mui/icons-material/AlternateEmailRounded'
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined'
import ChatBubbleOutlineRoundedIcon from '@mui/icons-material/ChatBubbleOutlineRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import SendRoundedIcon from '@mui/icons-material/SendRounded'
import {
    Alert,
    Autocomplete,
    Avatar,
    Box,
    Button,
    Chip,
    CircularProgress,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    Divider,
    Drawer,
    IconButton,
    Paper,
    Skeleton,
    Stack,
    Tab,
    Tabs,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useMemo, useState } from 'react'

import {
    useCreateTaskComment,
    useDeleteTaskComment,
    useTaskActivity,
    useTaskComments,
    useUpdateTaskComment,
} from '../hooks/useProjectTaskCollaboration'
import type { TaskActivityType, TaskComment } from '../types/taskCollaboration'
import type { ProjectTask } from '../types/projectTasks'
import type { ProjectMember } from '../types/projects'

interface ProjectTaskCollaborationDrawerProps {
    open: boolean
    task: ProjectTask
    tenantId: string
    projectId: string
    currentUserId: string
    members: ProjectMember[]
    readOnly: boolean
    onClose: () => void
    onFeedback: (message: string) => void
}

type CollaborationTab = 'comments' | 'activity'

const statusLabels: Record<ProjectTask['status'], string> = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
}

const priorityLabels: Record<ProjectTask['priority'], string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    URGENT: 'Urgent',
}

const activityLabels: Record<TaskActivityType, string> = {
    TASK_CREATED: 'Task created',
    TASK_UPDATED: 'Task updated',
    STATUS_CHANGED: 'Status changed',
    ASSIGNEE_CHANGED: 'Assignee changed',
    TASK_CANCELLED: 'Task cancelled',
    COMMENT_ADDED: 'Comment added',
    COMMENT_EDITED: 'Comment edited',
    COMMENT_DELETED: 'Comment deleted',
}

function getInitials(name: string): string {
    return name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase())
        .join('')
}

function formatDateTime(value: string | null): string {
    if (!value) return 'No due date'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return '—'
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function MemberMentionPicker({
    members,
    value,
    onChange,
    disabled,
}: {
    members: ProjectMember[]
    value: ProjectMember[]
    onChange: (members: ProjectMember[]) => void
    disabled?: boolean
}) {
    const activeMembers = useMemo(
        () => members.filter((member) => member.userStatus === 'ACTIVE'),
        [members],
    )

    return (
        <Autocomplete
            disabled={disabled}
            filterSelectedOptions
            getOptionKey={(option) => option.userId}
            getOptionLabel={(option) => option.fullName}
            isOptionEqualToValue={(option, selected) => option.userId === selected.userId}
            multiple
            onChange={(_event, nextValue) => onChange(nextValue)}
            options={activeMembers}
            renderInput={(params) => (
                <TextField
                    {...params}
                    helperText="Mentioned teammates are attached to this comment."
                    label="Mention teammates"
                    placeholder={value.length === 0 ? 'Add @mentions' : undefined}
                    size="small"
                    slotProps={{
                        input: {
                            ...params.InputProps,
                            startAdornment: (
                                <>
                                    <AlternateEmailRoundedIcon
                                        fontSize="small"
                                        sx={{ color: 'text.secondary', marginRight: 0.5 }}
                                    />
                                    {params.InputProps.startAdornment}
                                </>
                            ),
                        },
                    }}
                />
            )}
            renderOption={(props, option) => (
                <li {...props} key={option.userId}>
                    <Stack>
                        <Typography variant="body2">{option.fullName}</Typography>
                        <Typography color="text.secondary" variant="caption">
                            {option.email}
                        </Typography>
                    </Stack>
                </li>
            )}
            value={value}
        />
    )
}

function CommentComposer({
    members,
    disabled,
    onSubmit,
    submitting,
}: {
    members: ProjectMember[]
    disabled: boolean
    onSubmit: (body: string, mentionedUserIds: string[]) => Promise<void>
    submitting: boolean
}) {
    const [body, setBody] = useState('')
    const [mentions, setMentions] = useState<ProjectMember[]>([])

    const submit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const normalized = body.trim()
        if (!normalized || disabled || submitting) return

        await onSubmit(
            normalized,
            mentions.map((member) => member.userId),
        )
        setBody('')
        setMentions([])
    }

    return (
        <Paper
            component="form"
            onSubmit={(event) => void submit(event)}
            variant="outlined"
            sx={{ padding: 2 }}
        >
            <Stack spacing={1.5}>
                <TextField
                    disabled={disabled}
                    label="Add a comment"
                    maxRows={8}
                    minRows={3}
                    multiline
                    onChange={(event) => setBody(event.target.value)}
                    placeholder="Share an update, ask a question, or leave context for the team…"
                    value={body}
                />
                <MemberMentionPicker
                    disabled={disabled}
                    members={members}
                    onChange={setMentions}
                    value={mentions}
                />
                <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
                    <Button
                        disabled={disabled || !body.trim() || submitting}
                        endIcon={
                            submitting ? (
                                <CircularProgress color="inherit" size={15} />
                            ) : (
                                <SendRoundedIcon />
                            )
                        }
                        type="submit"
                        variant="contained"
                    >
                        Comment
                    </Button>
                </Stack>
            </Stack>
        </Paper>
    )
}

export function ProjectTaskCollaborationDrawer({
    open,
    task,
    tenantId,
    projectId,
    currentUserId,
    members,
    readOnly,
    onClose,
    onFeedback,
}: ProjectTaskCollaborationDrawerProps) {
    const [tab, setTab] = useState<CollaborationTab>('comments')
    const [editingComment, setEditingComment] = useState<TaskComment | null>(null)
    const [editBody, setEditBody] = useState('')
    const [editMentions, setEditMentions] = useState<ProjectMember[]>([])
    const [deleteComment, setDeleteComment] = useState<TaskComment | null>(null)
    const [mutationError, setMutationError] = useState<string | null>(null)

    const pageParams = { page: 0, size: 50, sortDir: 'desc' as const }
    const commentsQuery = useTaskComments(tenantId, projectId, task.id, pageParams, open)
    const activityQuery = useTaskActivity(tenantId, projectId, task.id, pageParams, open)
    const createCommentMutation = useCreateTaskComment(tenantId, projectId, task.id)
    const updateCommentMutation = useUpdateTaskComment(tenantId, projectId, task.id)
    const deleteCommentMutation = useDeleteTaskComment(tenantId, projectId, task.id)

    const memberById = useMemo(
        () => new Map(members.map((member) => [member.userId, member])),
        [members],
    )

    const copyDeepLink = async () => {
        try {
            await navigator.clipboard.writeText(window.location.href)
            onFeedback('Task link copied to clipboard.')
        } catch {
            onFeedback('The task is open at the current URL. Copy it from the address bar.')
        }
    }

    const createComment = async (body: string, mentionedUserIds: string[]) => {
        setMutationError(null)
        try {
            await createCommentMutation.mutateAsync({ body, mentionedUserIds })
            onFeedback('Comment added.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The comment could not be added.'))
        }
    }

    const startEditing = (comment: TaskComment) => {
        setEditingComment(comment)
        setEditBody(comment.body ?? '')
        setEditMentions(
            comment.mentions
                .map((mention) => memberById.get(mention.userId))
                .filter((member): member is ProjectMember => Boolean(member)),
        )
        setMutationError(null)
    }

    const saveEdit = async () => {
        if (!editingComment || !editBody.trim()) return
        setMutationError(null)
        try {
            await updateCommentMutation.mutateAsync({
                commentId: editingComment.id,
                input: {
                    body: editBody.trim(),
                    mentionedUserIds: editMentions.map((member) => member.userId),
                },
            })
            setEditingComment(null)
            setEditBody('')
            setEditMentions([])
            onFeedback('Comment updated.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The comment could not be updated.'))
        }
    }

    const confirmDelete = async () => {
        if (!deleteComment) return
        setMutationError(null)
        try {
            await deleteCommentMutation.mutateAsync(deleteComment.id)
            setDeleteComment(null)
            onFeedback('Comment deleted.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The comment could not be deleted.'))
        }
    }

    return (
        <>
            <Drawer
                anchor="right"
                onClose={onClose}
                open={open}
                slotProps={{
                    paper: {
                        sx: {
                            backgroundImage: (theme) =>
                                theme.palette.mode === 'dark'
                                    ? 'linear-gradient(155deg, rgba(255,255,255,0.025), transparent 38%)'
                                    : 'linear-gradient(155deg, rgba(255,255,255,0.78), rgba(235,239,243,0.34))',
                            width: { xs: '100%', sm: 560 },
                        },
                    },
                }}
            >
                <Stack sx={{ height: '100%' }}>
                    <Box sx={{ padding: { xs: 2, sm: 2.5 } }}>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                            <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                <Typography color="text.secondary" variant="overline">
                                    Task collaboration
                                </Typography>
                                <Typography component="h2" variant="h5">
                                    {task.title}
                                </Typography>
                            </Box>
                            <Tooltip title="Copy task link">
                                <IconButton
                                    aria-label="Copy task link"
                                    onClick={() => void copyDeepLink()}
                                >
                                    <ContentCopyRoundedIcon />
                                </IconButton>
                            </Tooltip>
                            <IconButton aria-label="Close task details" onClick={onClose}>
                                <CloseRoundedIcon />
                            </IconButton>
                        </Stack>

                        <Stack
                            direction="row"
                            spacing={0.75}
                            sx={{ flexWrap: 'wrap', gap: 0.75, marginTop: 1.5 }}
                        >
                            <Chip
                                label={statusLabels[task.status]}
                                size="small"
                                variant="outlined"
                            />
                            <Chip label={priorityLabels[task.priority]} size="small" />
                            <Chip
                                icon={<AssignmentIndOutlinedIcon />}
                                label={task.assigneeName || 'Unassigned'}
                                size="small"
                                variant="outlined"
                            />
                        </Stack>

                        {task.description && (
                            <Typography
                                color="text.secondary"
                                sx={{ marginTop: 1.5 }}
                                variant="body2"
                            >
                                {task.description}
                            </Typography>
                        )}
                        <Typography color="text.secondary" sx={{ marginTop: 1 }} variant="caption">
                            Due {formatDateTime(task.dueAt)}
                        </Typography>
                    </Box>

                    <Divider />
                    <Tabs
                        aria-label="Task collaboration views"
                        onChange={(_event, value: CollaborationTab) => setTab(value)}
                        value={tab}
                        variant="fullWidth"
                    >
                        <Tab
                            icon={<ChatBubbleOutlineRoundedIcon fontSize="small" />}
                            iconPosition="start"
                            label="Comments"
                            value="comments"
                        />
                        <Tab
                            icon={<HistoryRoundedIcon fontSize="small" />}
                            iconPosition="start"
                            label="Activity"
                            value="activity"
                        />
                    </Tabs>
                    <Divider />

                    <Box sx={{ flexGrow: 1, overflowY: 'auto', padding: { xs: 2, sm: 2.5 } }}>
                        {mutationError && (
                            <Alert
                                onClose={() => setMutationError(null)}
                                severity="error"
                                sx={{ marginBottom: 2 }}
                            >
                                {mutationError}
                            </Alert>
                        )}

                        {tab === 'comments' && (
                            <Stack spacing={2}>
                                {readOnly ? (
                                    <Alert severity="info">
                                        This task is read-only. Existing discussion remains
                                        available for history.
                                    </Alert>
                                ) : (
                                    <CommentComposer
                                        disabled={readOnly}
                                        members={members}
                                        onSubmit={createComment}
                                        submitting={createCommentMutation.isPending}
                                    />
                                )}

                                {commentsQuery.isPending && (
                                    <Stack
                                        spacing={1.5}
                                        aria-label="Loading task comments"
                                        role="status"
                                    >
                                        <Skeleton height={112} variant="rounded" />
                                        <Skeleton height={90} variant="rounded" />
                                    </Stack>
                                )}
                                {commentsQuery.isError && (
                                    <Alert severity="error">
                                        {getErrorMessage(
                                            commentsQuery.error,
                                            'Task comments could not be loaded.',
                                        )}
                                    </Alert>
                                )}
                                {commentsQuery.isSuccess &&
                                    commentsQuery.data.content.length === 0 && (
                                        <Box sx={{ padding: 3, textAlign: 'center' }}>
                                            <ChatBubbleOutlineRoundedIcon color="disabled" />
                                            <Typography sx={{ marginTop: 1 }} variant="subtitle2">
                                                No comments yet
                                            </Typography>
                                            <Typography color="text.secondary" variant="body2">
                                                Start the conversation with context the whole
                                                project can keep.
                                            </Typography>
                                        </Box>
                                    )}

                                {commentsQuery.data?.content.map((comment) => {
                                    const ownComment = comment.authorUserId === currentUserId
                                    const isEditing = editingComment?.id === comment.id

                                    return (
                                        <Paper
                                            key={comment.id}
                                            variant="outlined"
                                            sx={{ padding: 2 }}
                                        >
                                            <Stack spacing={1.25}>
                                                <Stack
                                                    direction="row"
                                                    spacing={1}
                                                    sx={{ alignItems: 'flex-start' }}
                                                >
                                                    <Avatar
                                                        sx={{ height: 34, width: 34, fontSize: 12 }}
                                                    >
                                                        {getInitials(comment.authorName)}
                                                    </Avatar>
                                                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                                        <Typography
                                                            sx={{ fontWeight: 700 }}
                                                            variant="body2"
                                                        >
                                                            {comment.authorName}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="caption"
                                                        >
                                                            {formatDateTime(comment.createdAt)}
                                                            {comment.editedAt ? ' · edited' : ''}
                                                        </Typography>
                                                    </Box>
                                                    {ownComment &&
                                                        !comment.deleted &&
                                                        !readOnly && (
                                                            <Stack direction="row">
                                                                <Tooltip title="Edit comment">
                                                                    <IconButton
                                                                        aria-label={`Edit comment by ${comment.authorName}`}
                                                                        onClick={() =>
                                                                            startEditing(comment)
                                                                        }
                                                                        size="small"
                                                                    >
                                                                        <EditOutlinedIcon fontSize="small" />
                                                                    </IconButton>
                                                                </Tooltip>
                                                                <Tooltip title="Delete comment">
                                                                    <IconButton
                                                                        aria-label={`Delete comment by ${comment.authorName}`}
                                                                        onClick={() =>
                                                                            setDeleteComment(
                                                                                comment,
                                                                            )
                                                                        }
                                                                        size="small"
                                                                    >
                                                                        <DeleteOutlineRoundedIcon fontSize="small" />
                                                                    </IconButton>
                                                                </Tooltip>
                                                            </Stack>
                                                        )}
                                                </Stack>

                                                {comment.deleted ? (
                                                    <Typography
                                                        color="text.secondary"
                                                        fontStyle="italic"
                                                        variant="body2"
                                                    >
                                                        Comment deleted
                                                    </Typography>
                                                ) : isEditing ? (
                                                    <Stack spacing={1.25}>
                                                        <TextField
                                                            label="Edit comment"
                                                            multiline
                                                            minRows={3}
                                                            onChange={(event) =>
                                                                setEditBody(event.target.value)
                                                            }
                                                            value={editBody}
                                                        />
                                                        <MemberMentionPicker
                                                            members={members}
                                                            onChange={setEditMentions}
                                                            value={editMentions}
                                                        />
                                                        <Stack
                                                            direction="row"
                                                            spacing={1}
                                                            sx={{ justifyContent: 'flex-end' }}
                                                        >
                                                            <Button
                                                                onClick={() =>
                                                                    setEditingComment(null)
                                                                }
                                                            >
                                                                Cancel
                                                            </Button>
                                                            <Button
                                                                disabled={
                                                                    !editBody.trim() ||
                                                                    updateCommentMutation.isPending
                                                                }
                                                                onClick={() => void saveEdit()}
                                                                variant="contained"
                                                            >
                                                                Save
                                                            </Button>
                                                        </Stack>
                                                    </Stack>
                                                ) : (
                                                    <Typography
                                                        sx={{ whiteSpace: 'pre-wrap' }}
                                                        variant="body2"
                                                    >
                                                        {comment.body}
                                                    </Typography>
                                                )}

                                                {!comment.deleted &&
                                                    comment.mentions.length > 0 && (
                                                        <Stack
                                                            direction="row"
                                                            spacing={0.75}
                                                            sx={{ flexWrap: 'wrap', gap: 0.75 }}
                                                        >
                                                            {comment.mentions.map((mention) => (
                                                                <Chip
                                                                    icon={
                                                                        <AlternateEmailRoundedIcon />
                                                                    }
                                                                    key={mention.userId}
                                                                    label={mention.fullName}
                                                                    size="small"
                                                                    variant="outlined"
                                                                />
                                                            ))}
                                                        </Stack>
                                                    )}
                                            </Stack>
                                        </Paper>
                                    )
                                })}

                                {commentsQuery.data && !commentsQuery.data.last && (
                                    <Alert severity="info">
                                        Showing the latest {commentsQuery.data.content.length}{' '}
                                        comments.
                                    </Alert>
                                )}
                            </Stack>
                        )}

                        {tab === 'activity' && (
                            <Stack spacing={0}>
                                {activityQuery.isPending && (
                                    <Stack
                                        spacing={1.5}
                                        aria-label="Loading task activity"
                                        role="status"
                                    >
                                        <Skeleton height={70} />
                                        <Skeleton height={70} />
                                        <Skeleton height={70} />
                                    </Stack>
                                )}
                                {activityQuery.isError && (
                                    <Alert severity="error">
                                        {getErrorMessage(
                                            activityQuery.error,
                                            'Task activity could not be loaded.',
                                        )}
                                    </Alert>
                                )}
                                {activityQuery.data?.content.map((activity, index) => (
                                    <Stack
                                        direction="row"
                                        key={activity.id}
                                        spacing={1.5}
                                        sx={{ minHeight: 76 }}
                                    >
                                        <Stack sx={{ alignItems: 'center' }}>
                                            <Box
                                                sx={{
                                                    bgcolor: 'action.selected',
                                                    border: 1,
                                                    borderColor: 'divider',
                                                    borderRadius: '50%',
                                                    height: 30,
                                                    width: 30,
                                                    display: 'grid',
                                                    placeItems: 'center',
                                                }}
                                            >
                                                <HistoryRoundedIcon sx={{ fontSize: 16 }} />
                                            </Box>
                                            {index <
                                                (activityQuery.data?.content.length ?? 0) - 1 && (
                                                <Box
                                                    sx={{
                                                        bgcolor: 'divider',
                                                        flexGrow: 1,
                                                        marginY: 0.5,
                                                        width: '1px',
                                                    }}
                                                />
                                            )}
                                        </Stack>
                                        <Box sx={{ paddingBottom: 2 }}>
                                            <Typography sx={{ fontWeight: 650 }} variant="body2">
                                                {activityLabels[activity.type]}
                                            </Typography>
                                            <Typography color="text.secondary" variant="body2">
                                                {activity.summary}
                                            </Typography>
                                            <Typography color="text.secondary" variant="caption">
                                                {activity.actorName} ·{' '}
                                                {formatDateTime(activity.createdAt)}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                ))}
                                {activityQuery.isSuccess &&
                                    activityQuery.data.content.length === 0 && (
                                        <Typography color="text.secondary" variant="body2">
                                            No task activity has been recorded yet.
                                        </Typography>
                                    )}
                                {activityQuery.data && !activityQuery.data.last && (
                                    <Alert severity="info">
                                        Showing the latest {activityQuery.data.content.length}{' '}
                                        activity entries.
                                    </Alert>
                                )}
                            </Stack>
                        )}
                    </Box>
                </Stack>
            </Drawer>

            <Dialog onClose={() => setDeleteComment(null)} open={Boolean(deleteComment)}>
                <DialogTitle>Delete comment?</DialogTitle>
                <DialogContent>
                    <Typography color="text.secondary" variant="body2">
                        The comment text will be removed, but a deleted marker will remain in the
                        task history.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setDeleteComment(null)}>Keep comment</Button>
                    <Button
                        color="error"
                        disabled={deleteCommentMutation.isPending}
                        onClick={() => void confirmDelete()}
                    >
                        Delete
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    )
}
