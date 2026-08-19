import AlternateEmailRoundedIcon from '@mui/icons-material/AlternateEmailRounded'
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined'
import AttachFileRoundedIcon from '@mui/icons-material/AttachFileRounded'
import ChatBubbleOutlineRoundedIcon from '@mui/icons-material/ChatBubbleOutlineRounded'
import CloseRoundedIcon from '@mui/icons-material/CloseRounded'
import ContentCopyRoundedIcon from '@mui/icons-material/ContentCopyRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import ForumOutlinedIcon from '@mui/icons-material/ForumOutlined'
import HistoryRoundedIcon from '@mui/icons-material/HistoryRounded'
import PushPinOutlinedIcon from '@mui/icons-material/PushPinOutlined'
import PushPinRoundedIcon from '@mui/icons-material/PushPinRounded'
import ReplyRoundedIcon from '@mui/icons-material/ReplyRounded'
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
import { useEffect, useMemo, useRef, useState } from 'react'

import {
    useCreateTaskComment,
    useCreateTaskCommentReply,
    useDeleteTaskComment,
    usePinTaskComment,
    usePinnedTaskComments,
    useTaskActivity,
    useTaskCommentReplies,
    useTaskComments,
    useUnpinTaskComment,
    useUpdateTaskComment,
} from '../hooks/useProjectTaskCollaboration'
import { useTaskCommentDeepLink } from '../hooks/useTaskCommentDeepLink'
import type { TaskActivityType, TaskComment } from '../types/taskCollaboration'
import type { ProjectTask } from '../types/projectTasks'
import type { ProjectMember } from '../types/projects'
import { ProjectTaskAttachmentsPanel } from './ProjectTaskAttachmentsPanel'

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

type CollaborationTab = 'comments' | 'attachments' | 'activity'

interface DeepLinkTarget {
    commentId: string
    replyId: string
}

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
    COMMENT_REPLIED: 'Comment replied',
    COMMENT_EDITED: 'Comment edited',
    COMMENT_DELETED: 'Comment deleted',
    COMMENT_PINNED: 'Comment pinned',
    COMMENT_UNPINNED: 'Comment unpinned',
    ATTACHMENT_ADDED: 'Attachment added',
    ATTACHMENT_DELETED: 'Attachment deleted',
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

function readDeepLinkTarget(): DeepLinkTarget {
    if (typeof window === 'undefined') return { commentId: '', replyId: '' }
    const searchParams = new URL(window.location.href).searchParams
    return {
        commentId: searchParams.get('comment')?.trim() ?? '',
        replyId: searchParams.get('reply')?.trim() ?? '',
    }
}

function MemberMentionPicker({
    members,
    value,
    onChange,
    disabled,
    helperText = 'Mentioned teammates are attached to this comment.',
}: {
    members: ProjectMember[]
    value: ProjectMember[]
    onChange: (members: ProjectMember[]) => void
    disabled?: boolean
    helperText?: string
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
                    helperText={helperText}
                    label="Mention teammates"
                    placeholder={value.length === 0 ? 'Add @mentions' : undefined}
                    size="small"
                    slotProps={{
                        ...params.slotProps,
                        input: {
                            ...params.slotProps.input,
                            startAdornment: (
                                <>
                                    <AlternateEmailRoundedIcon
                                        fontSize="small"
                                        sx={{ color: 'text.secondary', marginRight: 0.5 }}
                                    />
                                    {params.slotProps.input?.startAdornment}
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
    compact = false,
}: {
    members: ProjectMember[]
    disabled: boolean
    onSubmit: (body: string, mentionedUserIds: string[]) => Promise<void>
    submitting: boolean
    compact?: boolean
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
            sx={{ padding: compact ? 1.5 : 2 }}
        >
            <Stack spacing={1.25}>
                <TextField
                    disabled={disabled}
                    label={compact ? 'Write a reply' : 'Add a comment'}
                    maxRows={compact ? 5 : 8}
                    minRows={compact ? 2 : 3}
                    multiline
                    onChange={(event) => setBody(event.target.value)}
                    placeholder={
                        compact
                            ? 'Reply to this thread…'
                            : 'Share an update, ask a question, or leave context for the team…'
                    }
                    size={compact ? 'small' : undefined}
                    value={body}
                />
                <MemberMentionPicker
                    disabled={disabled}
                    helperText={
                        compact ? 'Optionally mention a teammate in this reply.' : undefined
                    }
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
                        size={compact ? 'small' : 'medium'}
                        type="submit"
                        variant="contained"
                    >
                        {compact ? 'Post reply' : 'Comment'}
                    </Button>
                </Stack>
            </Stack>
        </Paper>
    )
}

interface CommentCardProps {
    comment: TaskComment
    tenantId: string
    projectId: string
    taskId: string
    currentUserId: string
    members: ProjectMember[]
    readOnly: boolean
    allowThreading: boolean
    allowPinning: boolean
    focusCommentId?: string
    targetReply?: TaskComment
    onFeedback: (message: string) => void
}

function CommentCard({
    comment,
    tenantId,
    projectId,
    taskId,
    currentUserId,
    members,
    readOnly,
    allowThreading,
    allowPinning,
    focusCommentId,
    targetReply,
    onFeedback,
}: CommentCardProps) {
    const cardRef = useRef<HTMLDivElement | null>(null)
    const [expanded, setExpanded] = useState(false)
    const [replying, setReplying] = useState(false)
    const [editing, setEditing] = useState(false)
    const [editBody, setEditBody] = useState(comment.body ?? '')
    const [editMentions, setEditMentions] = useState<ProjectMember[]>([])
    const [confirmDelete, setConfirmDelete] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const focused = comment.id === focusCommentId
    const linkedReply =
        allowThreading && targetReply?.parentCommentId === comment.id ? targetReply : undefined

    const replyParams = { page: 0, size: 50, sortDir: 'asc' as const }
    const repliesQuery = useTaskCommentReplies(
        tenantId,
        projectId,
        taskId,
        comment.id,
        replyParams,
        allowThreading && expanded && comment.replyCount > 0,
    )
    const createReplyMutation = useCreateTaskCommentReply(tenantId, projectId, taskId)
    const updateMutation = useUpdateTaskComment(tenantId, projectId, taskId)
    const deleteMutation = useDeleteTaskComment(tenantId, projectId, taskId)
    const pinMutation = usePinTaskComment(tenantId, projectId, taskId)
    const unpinMutation = useUnpinTaskComment(tenantId, projectId, taskId)

    const memberById = useMemo(
        () => new Map(members.map((member) => [member.userId, member])),
        [members],
    )
    const ownComment = comment.authorUserId === currentUserId
    const canMutateBody = ownComment && !readOnly && !comment.deleted
    const replyAlreadyLoaded = Boolean(
        linkedReply && repliesQuery.data?.content.some((reply) => reply.id === linkedReply.id),
    )

    useEffect(() => {
        if (linkedReply) setExpanded(true)
    }, [linkedReply])

    useEffect(() => {
        if (!focused) return
        const frameId = window.requestAnimationFrame(() => {
            cardRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
        })
        return () => window.cancelAnimationFrame(frameId)
    }, [focused])

    const startEditing = () => {
        setEditBody(comment.body ?? '')
        setEditMentions(
            comment.mentions
                .map((mention) => memberById.get(mention.userId))
                .filter((member): member is ProjectMember => Boolean(member)),
        )
        setError(null)
        setEditing(true)
    }

    const saveEdit = async () => {
        const body = editBody.trim()
        if (!body) return
        setError(null)
        try {
            await updateMutation.mutateAsync({
                commentId: comment.id,
                input: {
                    body,
                    mentionedUserIds: editMentions.map((member) => member.userId),
                },
            })
            setEditing(false)
            onFeedback('Comment updated.')
        } catch (mutationError) {
            setError(getErrorMessage(mutationError, 'The comment could not be updated.'))
        }
    }

    const deleteCurrentComment = async () => {
        setError(null)
        try {
            await deleteMutation.mutateAsync(comment.id)
            setConfirmDelete(false)
            onFeedback('Comment deleted.')
        } catch (mutationError) {
            setError(getErrorMessage(mutationError, 'The comment could not be deleted.'))
        }
    }

    const submitReply = async (body: string, mentionedUserIds: string[]) => {
        setError(null)
        try {
            await createReplyMutation.mutateAsync({
                commentId: comment.id,
                input: { body, mentionedUserIds },
            })
            setExpanded(true)
            setReplying(false)
            onFeedback('Reply added.')
        } catch (mutationError) {
            setError(getErrorMessage(mutationError, 'The reply could not be added.'))
        }
    }

    const togglePin = async () => {
        setError(null)
        try {
            if (comment.pinned) {
                await unpinMutation.mutateAsync(comment.id)
                onFeedback('Comment unpinned.')
            } else {
                await pinMutation.mutateAsync(comment.id)
                onFeedback('Comment pinned.')
            }
        } catch (mutationError) {
            setError(
                getErrorMessage(
                    mutationError,
                    comment.pinned
                        ? 'The comment could not be unpinned.'
                        : 'The comment could not be pinned.',
                ),
            )
        }
    }

    return (
        <Paper
            data-comment-id={comment.id}
            data-deep-link-target={focused ? 'true' : undefined}
            ref={cardRef}
            variant="outlined"
            sx={{
                bgcolor: focused ? 'action.selected' : undefined,
                borderColor: focused || comment.pinned ? 'primary.main' : undefined,
                boxShadow: focused ? 2 : undefined,
                padding: 1.75,
                transition: 'background-color 180ms ease, border-color 180ms ease, box-shadow 180ms ease',
            }}
        >
            <Stack spacing={1.25}>
                {error && (
                    <Alert onClose={() => setError(null)} severity="error">
                        {error}
                    </Alert>
                )}
                <Stack direction="row" spacing={1.25} sx={{ alignItems: 'flex-start' }}>
                    <Avatar sx={{ height: 34, width: 34 }}>
                        {getInitials(comment.authorName)}
                    </Avatar>
                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                        <Stack
                            direction="row"
                            spacing={0.75}
                            sx={{ alignItems: 'center', flexWrap: 'wrap' }}
                        >
                            <Typography variant="subtitle2">{comment.authorName}</Typography>
                            {comment.pinned && (
                                <Chip
                                    icon={<PushPinRoundedIcon />}
                                    label="Pinned"
                                    size="small"
                                    variant="outlined"
                                />
                            )}
                            {comment.editedAt && !comment.deleted && (
                                <Typography color="text.secondary" variant="caption">
                                    edited
                                </Typography>
                            )}
                        </Stack>
                        <Typography color="text.secondary" variant="caption">
                            {formatDateTime(comment.createdAt)}
                        </Typography>
                    </Box>

                    {!readOnly && allowPinning && !comment.deleted && !comment.parentCommentId && (
                        <Tooltip title={comment.pinned ? 'Unpin comment' : 'Pin comment'}>
                            <IconButton
                                aria-label={`${comment.pinned ? 'Unpin' : 'Pin'} comment by ${comment.authorName}`}
                                disabled={pinMutation.isPending || unpinMutation.isPending}
                                onClick={() => void togglePin()}
                                size="small"
                            >
                                {comment.pinned ? <PushPinRoundedIcon /> : <PushPinOutlinedIcon />}
                            </IconButton>
                        </Tooltip>
                    )}

                    {canMutateBody && (
                        <>
                            <Tooltip title="Edit comment">
                                <IconButton
                                    aria-label={`Edit comment by ${comment.authorName}`}
                                    onClick={startEditing}
                                    size="small"
                                >
                                    <EditOutlinedIcon />
                                </IconButton>
                            </Tooltip>
                            <Tooltip title="Delete comment">
                                <IconButton
                                    aria-label={`Delete comment by ${comment.authorName}`}
                                    onClick={() => setConfirmDelete(true)}
                                    size="small"
                                >
                                    <DeleteOutlineRoundedIcon />
                                </IconButton>
                            </Tooltip>
                        </>
                    )}
                </Stack>

                {editing ? (
                    <Stack spacing={1.25}>
                        <TextField
                            autoFocus
                            label="Edit comment"
                            maxRows={8}
                            minRows={3}
                            multiline
                            onChange={(event) => setEditBody(event.target.value)}
                            value={editBody}
                        />
                        <MemberMentionPicker
                            members={members}
                            onChange={setEditMentions}
                            value={editMentions}
                        />
                        <Stack direction="row" spacing={1} sx={{ justifyContent: 'flex-end' }}>
                            <Button onClick={() => setEditing(false)}>Cancel</Button>
                            <Button
                                disabled={!editBody.trim() || updateMutation.isPending}
                                onClick={() => void saveEdit()}
                                variant="contained"
                            >
                                Save
                            </Button>
                        </Stack>
                    </Stack>
                ) : (
                    <Typography
                        color={comment.deleted ? 'text.secondary' : 'text.primary'}
                        sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}
                        variant="body2"
                    >
                        {comment.deleted ? 'This comment was deleted.' : comment.body}
                    </Typography>
                )}

                {!comment.deleted && comment.mentions.length > 0 && (
                    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                        {comment.mentions.map((mention) => (
                            <Chip
                                key={mention.userId}
                                label={`@${mention.fullName}`}
                                size="small"
                                variant="outlined"
                            />
                        ))}
                    </Stack>
                )}

                {allowThreading && !comment.deleted && (
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        {comment.replyCount > 0 && (
                            <Button
                                onClick={() => setExpanded((current) => !current)}
                                size="small"
                                startIcon={<ForumOutlinedIcon />}
                            >
                                {expanded ? 'Hide' : 'View'} {comment.replyCount}{' '}
                                {comment.replyCount === 1 ? 'reply' : 'replies'}
                            </Button>
                        )}
                        {!readOnly && (
                            <Button
                                onClick={() => setReplying((current) => !current)}
                                size="small"
                                startIcon={<ReplyRoundedIcon />}
                            >
                                Reply
                            </Button>
                        )}
                    </Stack>
                )}

                {allowThreading && replying && !readOnly && (
                    <Box sx={{ marginLeft: { xs: 0, sm: 3 } }}>
                        <CommentComposer
                            compact
                            disabled={readOnly}
                            members={members}
                            onSubmit={submitReply}
                            submitting={createReplyMutation.isPending}
                        />
                    </Box>
                )}

                {allowThreading && expanded && comment.replyCount > 0 && (
                    <Box
                        sx={{
                            borderLeft: 2,
                            borderColor: 'divider',
                            marginLeft: { xs: 1, sm: 3 },
                            paddingLeft: 1.5,
                        }}
                    >
                        {linkedReply && !replyAlreadyLoaded && (
                            <Stack spacing={1.25} sx={{ marginBottom: repliesQuery.data ? 1.25 : 0 }}>
                                <CommentCard
                                    allowPinning={false}
                                    allowThreading={false}
                                    comment={linkedReply}
                                    currentUserId={currentUserId}
                                    focusCommentId={linkedReply.id}
                                    members={members}
                                    onFeedback={onFeedback}
                                    projectId={projectId}
                                    readOnly={readOnly}
                                    taskId={taskId}
                                    tenantId={tenantId}
                                />
                            </Stack>
                        )}
                        {repliesQuery.isPending && (
                            <Stack spacing={1} aria-label="Loading comment replies" role="status">
                                <Skeleton height={82} variant="rounded" />
                            </Stack>
                        )}
                        {repliesQuery.isError && (
                            <Alert severity="error">
                                {getErrorMessage(
                                    repliesQuery.error,
                                    'Comment replies could not be loaded.',
                                )}
                            </Alert>
                        )}
                        {repliesQuery.data?.content && (
                            <Stack spacing={1.25}>
                                {repliesQuery.data.content.map((reply) => (
                                    <CommentCard
                                        allowPinning={false}
                                        allowThreading={false}
                                        comment={reply}
                                        currentUserId={currentUserId}
                                        focusCommentId={linkedReply?.id}
                                        key={reply.id}
                                        members={members}
                                        onFeedback={onFeedback}
                                        projectId={projectId}
                                        readOnly={readOnly}
                                        taskId={taskId}
                                        tenantId={tenantId}
                                    />
                                ))}
                            </Stack>
                        )}
                    </Box>
                )}
            </Stack>

            <Dialog onClose={() => setConfirmDelete(false)} open={confirmDelete}>
                <DialogTitle>Delete comment?</DialogTitle>
                <DialogContent>
                    <Typography color="text.secondary" variant="body2">
                        The comment will remain in the thread as a deleted placeholder.
                    </Typography>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setConfirmDelete(false)}>Cancel</Button>
                    <Button
                        color="error"
                        disabled={deleteMutation.isPending}
                        onClick={() => void deleteCurrentComment()}
                        variant="contained"
                    >
                        Delete
                    </Button>
                </DialogActions>
            </Dialog>
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
    const [mutationError, setMutationError] = useState<string | null>(null)
    const [deepLinkTarget, setDeepLinkTarget] = useState<DeepLinkTarget>(readDeepLinkTarget)

    const pageParams = { page: 0, size: 50, sortDir: 'desc' as const }
    const commentsQuery = useTaskComments(tenantId, projectId, task.id, pageParams, open)
    const pinnedCommentsQuery = usePinnedTaskComments(tenantId, projectId, task.id, open)
    const activityQuery = useTaskActivity(tenantId, projectId, task.id, pageParams, open)
    const createCommentMutation = useCreateTaskComment(tenantId, projectId, task.id)
    const targetCommentQuery = useTaskCommentDeepLink(
        tenantId,
        projectId,
        task.id,
        deepLinkTarget.commentId,
        open && Boolean(deepLinkTarget.commentId),
    )
    const targetReplyQuery = useTaskCommentDeepLink(
        tenantId,
        projectId,
        task.id,
        deepLinkTarget.replyId,
        open && Boolean(deepLinkTarget.replyId),
    )

    useEffect(() => {
        const syncTarget = () => setDeepLinkTarget(readDeepLinkTarget())
        syncTarget()
        window.addEventListener('popstate', syncTarget)
        return () => window.removeEventListener('popstate', syncTarget)
    }, [task.id])

    useEffect(() => {
        if (deepLinkTarget.commentId || deepLinkTarget.replyId) setTab('comments')
    }, [deepLinkTarget.commentId, deepLinkTarget.replyId])

    const pinnedIds = useMemo(
        () => new Set((pinnedCommentsQuery.data ?? []).map((comment) => comment.id)),
        [pinnedCommentsQuery.data],
    )
    const regularComments = useMemo(
        () => (commentsQuery.data?.content ?? []).filter((comment) => !pinnedIds.has(comment.id)),
        [commentsQuery.data?.content, pinnedIds],
    )
    const loadedCommentIds = useMemo(
        () =>
            new Set([
                ...(pinnedCommentsQuery.data ?? []).map((comment) => comment.id),
                ...(commentsQuery.data?.content ?? []).map((comment) => comment.id),
            ]),
        [commentsQuery.data?.content, pinnedCommentsQuery.data],
    )
    const linkedComment =
        targetCommentQuery.data &&
        !targetCommentQuery.data.parentCommentId &&
        !loadedCommentIds.has(targetCommentQuery.data.id)
            ? targetCommentQuery.data
            : undefined
    const targetReply =
        targetReplyQuery.data?.parentCommentId === deepLinkTarget.commentId
            ? targetReplyQuery.data
            : undefined
    const topLevelFocusId = deepLinkTarget.replyId ? undefined : deepLinkTarget.commentId

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

    const closeDrawer = () => {
        const url = new URL(window.location.href)
        url.searchParams.delete('comment')
        url.searchParams.delete('reply')
        window.history.replaceState({}, '', url)
        setDeepLinkTarget({ commentId: '', replyId: '' })
        onClose()
    }

    const renderCommentCard = (comment: TaskComment) => (
        <CommentCard
            allowPinning
            allowThreading
            comment={comment}
            currentUserId={currentUserId}
            focusCommentId={topLevelFocusId}
            key={comment.id}
            members={members}
            onFeedback={onFeedback}
            projectId={projectId}
            readOnly={readOnly}
            targetReply={
                comment.id === deepLinkTarget.commentId ? targetReply : undefined
            }
            taskId={task.id}
            tenantId={tenantId}
        />
    )

    return (
        <Drawer
            anchor="right"
            onClose={closeDrawer}
            open={open}
            slotProps={{
                paper: {
                    sx: {
                        backgroundImage: (theme) =>
                            theme.palette.mode === 'dark'
                                ? 'linear-gradient(155deg, rgba(255,255,255,0.025), transparent 38%)'
                                : 'linear-gradient(155deg, rgba(255,255,255,0.78), rgba(235,239,243,0.34))',
                        width: { xs: '100%', sm: 580 },
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
                        <IconButton aria-label="Close task details" onClick={closeDrawer}>
                            <CloseRoundedIcon />
                        </IconButton>
                    </Stack>

                    <Stack
                        direction="row"
                        spacing={0.75}
                        sx={{ flexWrap: 'wrap', gap: 0.75, marginTop: 1.5 }}
                    >
                        <Chip label={statusLabels[task.status]} size="small" variant="outlined" />
                        <Chip label={priorityLabels[task.priority]} size="small" />
                        <Chip
                            icon={<AssignmentIndOutlinedIcon />}
                            label={task.assigneeName || 'Unassigned'}
                            size="small"
                            variant="outlined"
                        />
                    </Stack>

                    {task.description && (
                        <Typography color="text.secondary" sx={{ marginTop: 1.5 }} variant="body2">
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
                        icon={<AttachFileRoundedIcon fontSize="small" />}
                        iconPosition="start"
                        label="Attachments"
                        value="attachments"
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
                                    This task is read-only. Existing discussion remains available
                                    for history.
                                </Alert>
                            ) : (
                                <CommentComposer
                                    disabled={readOnly}
                                    members={members}
                                    onSubmit={createComment}
                                    submitting={createCommentMutation.isPending}
                                />
                            )}

                            {(targetCommentQuery.isError || targetReplyQuery.isError) && (
                                <Alert severity="warning">
                                    The referenced comment could not be loaded. The task discussion
                                    is still available below.
                                </Alert>
                            )}

                            {linkedComment && (
                                <Stack spacing={1.25}>
                                    <Typography color="text.secondary" variant="subtitle2">
                                        Referenced comment
                                    </Typography>
                                    {renderCommentCard(linkedComment)}
                                </Stack>
                            )}

                            {pinnedCommentsQuery.isError && (
                                <Alert severity="warning">
                                    {getErrorMessage(
                                        pinnedCommentsQuery.error,
                                        'Pinned comments could not be loaded.',
                                    )}
                                </Alert>
                            )}

                            {(pinnedCommentsQuery.data?.length ?? 0) > 0 && (
                                <Stack spacing={1.25}>
                                    <Stack
                                        direction="row"
                                        spacing={0.75}
                                        sx={{ alignItems: 'center' }}
                                    >
                                        <PushPinRoundedIcon color="primary" fontSize="small" />
                                        <Typography variant="subtitle2">Pinned comments</Typography>
                                    </Stack>
                                    {pinnedCommentsQuery.data?.map(renderCommentCard)}
                                </Stack>
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
                            {commentsQuery.isSuccess && commentsQuery.data.content.length === 0 && !linkedComment && (
                                <Box sx={{ padding: 3, textAlign: 'center' }}>
                                    <ChatBubbleOutlineRoundedIcon color="disabled" />
                                    <Typography sx={{ marginTop: 1 }} variant="subtitle2">
                                        No comments yet
                                    </Typography>
                                    <Typography color="text.secondary" variant="body2">
                                        Start the conversation with context the whole project can
                                        keep.
                                    </Typography>
                                </Box>
                            )}

                            {regularComments.length > 0 && (
                                <Stack spacing={1.5}>
                                    {(pinnedCommentsQuery.data?.length ?? 0) > 0 && (
                                        <Typography color="text.secondary" variant="subtitle2">
                                            Discussion
                                        </Typography>
                                    )}
                                    {regularComments.map(renderCommentCard)}
                                </Stack>
                            )}
                        </Stack>
                    )}

                    {tab === 'attachments' && (
                        <ProjectTaskAttachmentsPanel
                            comments={commentsQuery.data?.content ?? []}
                            currentUserId={currentUserId}
                            onFeedback={onFeedback}
                            projectId={projectId}
                            readOnly={readOnly}
                            taskId={task.id}
                            tenantId={tenantId}
                        />
                    )}

                    {tab === 'activity' && (
                        <Stack spacing={1.25}>
                            {activityQuery.isPending && (
                                <Stack
                                    spacing={1.25}
                                    aria-label="Loading task activity"
                                    role="status"
                                >
                                    <Skeleton height={74} variant="rounded" />
                                    <Skeleton height={74} variant="rounded" />
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
                            {activityQuery.data?.content.length === 0 && (
                                <Typography color="text.secondary" variant="body2">
                                    No activity recorded yet.
                                </Typography>
                            )}
                            {activityQuery.data?.content.map((activity) => (
                                <Paper key={activity.id} sx={{ padding: 1.5 }} variant="outlined">
                                    <Stack
                                        direction="row"
                                        spacing={1.25}
                                        sx={{ alignItems: 'center' }}
                                    >
                                        <Avatar sx={{ height: 32, width: 32 }}>
                                            {getInitials(activity.actorName)}
                                        </Avatar>
                                        <Box sx={{ flexGrow: 1 }}>
                                            <Typography variant="subtitle2">
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
                                </Paper>
                            ))}
                        </Stack>
                    )}
                </Box>
            </Stack>
        </Drawer>
    )
}
