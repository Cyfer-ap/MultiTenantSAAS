import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import DrawRoundedIcon from '@mui/icons-material/DrawRounded'
import {
    Alert,
    Box,
    Button,
    CircularProgress,
    Divider,
    List,
    ListItemButton,
    ListItemText,
    Paper,
    Skeleton,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'
import { Link, useParams } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { hasProjectPermission } from '../../authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../../authorization/hooks/useCurrentAuthorization'
import { authorizationPermissionCodes } from '../../authorization/types/authorization'
import { useProjectDetails } from '../../projects/hooks/useProjectDetails'
import { useProjectMembers } from '../../projects/hooks/useProjectMembers'
import { WhiteboardEditor } from '../components/WhiteboardEditor'
import {
    useCreateWhiteboard,
    useDeleteWhiteboard,
    useWhiteboard,
    useWhiteboards,
} from '../hooks/useWhiteboards'

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

export function WhiteboardWorkspacePage() {
    const { session } = useAuth()
    const { projectId = '' } = useParams()
    const tenantId = session?.tenantId ?? ''
    const [selectedBoardId, setSelectedBoardId] = useState('')
    const [newBoardName, setNewBoardName] = useState('')
    const [editorEpoch, setEditorEpoch] = useState(0)
    const [feedback, setFeedback] = useState<string | null>(null)

    const projectQuery = useProjectDetails(tenantId, projectId)
    const authorizationQuery = useCurrentAuthorization()
    const membersQuery = useProjectMembers(
        tenantId,
        projectId,
        { page: 0, size: 100, sortBy: 'assignedAt', sortDir: 'asc' },
        Boolean(tenantId && projectId),
    )
    const boardsQuery = useWhiteboards(tenantId, projectId)
    const createMutation = useCreateWhiteboard(tenantId, projectId)
    const deleteMutation = useDeleteWhiteboard(tenantId, projectId)

    const boards = boardsQuery.data?.content ?? []
    const activeBoardId = selectedBoardId || boards[0]?.id || ''
    const boardQuery = useWhiteboard(tenantId, projectId, activeBoardId)
    const activeBoard = boardQuery.data

    const canManageByPermission = hasProjectPermission(
        authorizationQuery.data,
        authorizationPermissionCodes.PROJECT_TASK_MANAGE,
        projectId,
    )
    const isProjectLead = Boolean(
        session?.userId &&
            membersQuery.data?.content.some(
                (member) =>
                    member.userId === session.userId && member.projectRole === 'PROJECT_LEAD',
            ),
    )
    const archived = projectQuery.data?.status === 'ARCHIVED'
    const canManage = !archived && (canManageByPermission || isProjectLead)

    const createBoard = (): void => {
        const name = newBoardName.trim()
        if (!name || !canManage) return
        createMutation.mutate(
            { name, nodes: [], edges: [] },
            {
                onSuccess: (board) => {
                    setSelectedBoardId(board.id)
                    setNewBoardName('')
                    setEditorEpoch((value) => value + 1)
                    setFeedback('Whiteboard created')
                },
            },
        )
    }

    const deleteBoard = (): void => {
        if (!activeBoard || !canManage) return
        deleteMutation.mutate(
            { boardId: activeBoard.id, expectedVersion: activeBoard.version },
            {
                onSuccess: () => {
                    const nextBoard = boards.find((board) => board.id !== activeBoard.id)
                    setSelectedBoardId(nextBoard?.id ?? '')
                    setEditorEpoch((value) => value + 1)
                    setFeedback('Whiteboard deleted')
                },
            },
        )
    }

    const reloadBoard = async (): Promise<void> => {
        await boardQuery.refetch()
        setEditorEpoch((value) => value + 1)
    }

    if (projectQuery.isPending || boardsQuery.isPending) {
        return (
            <Stack aria-label="Loading whiteboard workspace" role="status" spacing={2}>
                <Skeleton height={48} width="45%" />
                <Skeleton height={720} variant="rounded" />
            </Stack>
        )
    }

    if (projectQuery.isError || boardsQuery.isError) {
        return (
            <Stack spacing={2}>
                <Button
                    component={Link}
                    startIcon={<ArrowBackRoundedIcon />}
                    sx={{ alignSelf: 'flex-start' }}
                    to={`/projects/${projectId}`}
                >
                    Back to project
                </Button>
                <Alert severity="error">
                    {getErrorMessage(
                        projectQuery.error ?? boardsQuery.error,
                        'The whiteboard workspace could not be loaded.',
                    )}
                </Alert>
            </Stack>
        )
    }

    return (
        <Stack spacing={2.5}>
            <Box>
                <Button
                    component={Link}
                    startIcon={<ArrowBackRoundedIcon />}
                    sx={{ marginBottom: 1 }}
                    to={`/projects/${projectId}`}
                >
                    Back to project
                </Button>
                <Typography color="text.secondary" variant="overline">
                    {projectQuery.data?.name ?? 'Project'} · Visual planning
                </Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <DrawRoundedIcon color="primary" />
                    <Typography component="h1" variant="h4">
                        Collaborative Whiteboard
                    </Typography>
                </Stack>
                <Typography color="text.secondary" sx={{ marginTop: 0.75 }} variant="body2">
                    Plan spatially with stickies, text, shapes and connectors, then turn useful nodes
                    into ordinary project tasks without bypassing task rules.
                </Typography>
            </Box>

            {archived && (
                <Alert severity="info">
                    This project is archived. Whiteboards remain visible but cannot be changed.
                </Alert>
            )}
            {feedback && (
                <Alert onClose={() => setFeedback(null)} severity="success">
                    {feedback}
                </Alert>
            )}
            {(createMutation.isError || deleteMutation.isError) && (
                <Alert severity="error">
                    {getErrorMessage(
                        createMutation.error ?? deleteMutation.error,
                        'The whiteboard operation could not be completed.',
                    )}
                </Alert>
            )}

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: { xs: '1fr', lg: '240px minmax(0, 1fr)' },
                }}
            >
                <Paper sx={{ alignSelf: 'start', padding: 1.5 }} variant="outlined">
                    <Stack spacing={1.25}>
                        <Box>
                            <Typography variant="h6">Boards</Typography>
                            <Typography color="text.secondary" variant="caption">
                                {boards.length} of 100 loaded
                            </Typography>
                        </Box>
                        {canManage && (
                            <Stack spacing={0.75}>
                                <TextField
                                    fullWidth
                                    label="New board name"
                                    onChange={(event) => setNewBoardName(event.target.value)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter') createBoard()
                                    }}
                                    size="small"
                                    value={newBoardName}
                                />
                                <Button
                                    disabled={createMutation.isPending || !newBoardName.trim()}
                                    onClick={createBoard}
                                    size="small"
                                    variant="contained"
                                >
                                    {createMutation.isPending ? 'Creating…' : 'Create board'}
                                </Button>
                            </Stack>
                        )}
                        <Divider />
                        {boards.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                No whiteboards yet.
                            </Typography>
                        ) : (
                            <List disablePadding dense>
                                {boards.map((board) => (
                                    <ListItemButton
                                        key={board.id}
                                        onClick={() => {
                                            setSelectedBoardId(board.id)
                                            setEditorEpoch((value) => value + 1)
                                        }}
                                        selected={board.id === activeBoardId}
                                        sx={{ borderRadius: 1 }}
                                    >
                                        <ListItemText
                                            primary={board.name}
                                            secondary={`v${board.version}`}
                                            slotProps={{ primary: { noWrap: true } }}
                                        />
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                        {activeBoard && canManage && (
                            <Button
                                color="error"
                                disabled={deleteMutation.isPending}
                                onClick={deleteBoard}
                                size="small"
                                startIcon={<DeleteOutlineRoundedIcon />}
                                variant="outlined"
                            >
                                Delete board
                            </Button>
                        )}
                    </Stack>
                </Paper>

                <Box sx={{ minWidth: 0 }}>
                    {!activeBoardId ? (
                        <Paper sx={{ padding: 4, textAlign: 'center' }} variant="outlined">
                            <Typography variant="h6">Start with a board</Typography>
                            <Typography color="text.secondary" sx={{ marginTop: 0.75 }}>
                                {canManage
                                    ? 'Create a whiteboard to begin visual planning.'
                                    : 'No whiteboard is available for this project yet.'}
                            </Typography>
                        </Paper>
                    ) : boardQuery.isPending ? (
                        <Stack aria-label="Loading whiteboard" role="status" spacing={1.5}>
                            <Skeleton height={52} variant="rounded" />
                            <Skeleton height={680} variant="rounded" />
                        </Stack>
                    ) : boardQuery.isError || !activeBoard ? (
                        <Alert
                            action={
                                <Button
                                    color="inherit"
                                    onClick={() => {
                                        void boardQuery.refetch()
                                    }}
                                    size="small"
                                >
                                    Retry
                                </Button>
                            }
                            severity="error"
                        >
                            {getErrorMessage(boardQuery.error, 'The selected whiteboard could not be loaded.')}
                        </Alert>
                    ) : (
                        <WhiteboardEditor
                            board={activeBoard}
                            canManage={canManage}
                            key={`${activeBoard.id}:${editorEpoch}`}
                            members={membersQuery.data?.content ?? []}
                            onReload={reloadBoard}
                            projectId={projectId}
                            tenantId={tenantId}
                        />
                    )}
                </Box>
            </Box>

            {(authorizationQuery.isPending || membersQuery.isPending) && (
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <CircularProgress size={16} />
                    <Typography color="text.secondary" variant="caption">
                        Resolving project edit authority…
                    </Typography>
                </Stack>
            )}
        </Stack>
    )
}
