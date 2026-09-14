import AccountTreeRoundedIcon from '@mui/icons-material/AccountTreeRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import {
    Alert,
    Box,
    Chip,
    InputAdornment,
    List,
    ListItemButton,
    ListItemText,
    Paper,
    Skeleton,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { hasProjectPermission } from '../../authorization/access/authorizationAccess'
import { useCurrentAuthorization } from '../../authorization/hooks/useCurrentAuthorization'
import { authorizationPermissionCodes } from '../../authorization/types/authorization'
import { useProjectDetails } from '../../projects/hooks/useProjectDetails'
import { useProjectMember } from '../../projects/hooks/useProjectMembers'
import { useProjectTask } from '../../projects/hooks/useProjectTasks'
import { useGlobalSearch } from '../../search/hooks/useGlobalSearch'
import { TaskRelationshipsPanel } from '../components/TaskRelationshipsPanel'

const statusLabels = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
} as const

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

export function TaskPlanningPage() {
    const { session } = useAuth()
    const [searchParams, setSearchParams] = useSearchParams()
    const [search, setSearch] = useState('')
    const [feedback, setFeedback] = useState<string | null>(null)

    const tenantId = session?.tenantId ?? ''
    const userId = session?.userId ?? ''
    const projectId = searchParams.get('project')?.trim() ?? ''
    const taskId = searchParams.get('task')?.trim() ?? ''

    const searchQuery = useGlobalSearch(tenantId, search, { limit: 30 })
    const taskResults = useMemo(
        () => (searchQuery.data?.results ?? []).filter((result) => result.type === 'TASK' && result.parentId),
        [searchQuery.data?.results],
    )

    const taskQuery = useProjectTask(tenantId, projectId, taskId, Boolean(projectId && taskId))
    const projectQuery = useProjectDetails(tenantId, projectId)
    const authorizationQuery = useCurrentAuthorization()
    const canManageByPermission = hasProjectPermission(
        authorizationQuery.data,
        authorizationPermissionCodes.PROJECT_TASK_MANAGE,
        projectId,
    )
    const memberQuery = useProjectMember(
        tenantId,
        projectId,
        userId,
        Boolean(projectId && userId && !canManageByPermission),
    )
    const canManageRelationships =
        Boolean(taskQuery.data) &&
        taskQuery.data?.status !== 'CANCELLED' &&
        projectQuery.data?.status !== 'ARCHIVED' &&
        (canManageByPermission || memberQuery.data?.projectRole === 'PROJECT_LEAD')

    const selectTask = (nextProjectId: string, nextTaskId: string) => {
        setSearchParams({ project: nextProjectId, task: nextTaskId })
        setFeedback(null)
    }

    const openRelatedTask = (nextTaskId: string) => {
        if (!projectId) return
        setSearchParams({ project: projectId, task: nextTaskId })
        setFeedback(null)
    }

    return (
        <Stack spacing={2.5}>
            <Box>
                <Typography color="text.secondary" variant="overline">
                    Work management
                </Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <AccountTreeRoundedIcon color="primary" />
                    <Typography component="h1" variant="h4">
                        Task Planning
                    </Typography>
                </Stack>
                <Typography color="text.secondary" sx={{ marginTop: 0.75 }} variant="body2">
                    Organize accessible project tasks with subtasks, blockers, dependents and reusable labels.
                </Typography>
            </Box>

            {feedback && (
                <Alert onClose={() => setFeedback(null)} severity="success">
                    {feedback}
                </Alert>
            )}

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: { xs: '1fr', lg: 'minmax(280px, 0.8fr) minmax(0, 2fr)' },
                }}
            >
                <Paper sx={{ alignSelf: 'start', padding: 2 }} variant="outlined">
                    <Stack spacing={1.5}>
                        <Typography variant="subtitle1">Find a task</Typography>
                        <TextField
                            fullWidth
                            label="Search accessible tasks"
                            onChange={(event) => setSearch(event.target.value)}
                            placeholder="Type at least 2 characters"
                            slotProps={{
                                input: {
                                    startAdornment: (
                                        <InputAdornment position="start">
                                            <SearchRoundedIcon fontSize="small" />
                                        </InputAdornment>
                                    ),
                                },
                            }}
                            value={search}
                        />

                        {search.trim().length < 2 && (
                            <Typography color="text.secondary" variant="body2">
                                Search uses the same authorization-safe task discovery as Global Search.
                            </Typography>
                        )}
                        {searchQuery.isPending && search.trim().length >= 2 && (
                            <Stack aria-label="Searching tasks" role="status" spacing={1}>
                                <Skeleton height={58} variant="rounded" />
                                <Skeleton height={58} variant="rounded" />
                            </Stack>
                        )}
                        {searchQuery.isError && (
                            <Alert severity="error">
                                {getErrorMessage(searchQuery.error, 'Tasks could not be searched.')}
                            </Alert>
                        )}
                        {searchQuery.isSuccess && search.trim().length >= 2 && taskResults.length === 0 && (
                            <Typography color="text.secondary" variant="body2">
                                No accessible tasks match this search.
                            </Typography>
                        )}
                        {taskResults.length > 0 && (
                            <List disablePadding>
                                {taskResults.map((result) => (
                                    <ListItemButton
                                        key={`${result.parentId}:${result.id}`}
                                        onClick={() => selectTask(result.parentId ?? '', result.id)}
                                        selected={result.id === taskId && result.parentId === projectId}
                                    >
                                        <ListItemText
                                            primary={result.title}
                                            secondary={result.subtitle ?? 'Project task'}
                                        />
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                    </Stack>
                </Paper>

                <Box>
                    {!taskId && (
                        <Paper sx={{ padding: { xs: 3, sm: 5 }, textAlign: 'center' }} variant="outlined">
                            <AccountTreeRoundedIcon color="disabled" sx={{ fontSize: 42 }} />
                            <Typography sx={{ marginTop: 1 }} variant="h6">
                                Select a task to plan
                            </Typography>
                            <Typography color="text.secondary" variant="body2">
                                Parent/subtask hierarchy, dependency edges and labels stay scoped to the task's project.
                            </Typography>
                        </Paper>
                    )}

                    {taskId && taskQuery.isPending && (
                        <Stack aria-label="Loading selected task" role="status" spacing={1.5}>
                            <Skeleton height={100} variant="rounded" />
                            <Skeleton height={180} variant="rounded" />
                            <Skeleton height={180} variant="rounded" />
                        </Stack>
                    )}

                    {taskId && taskQuery.isError && (
                        <Alert severity="error">
                            {getErrorMessage(taskQuery.error, 'The selected task could not be loaded.')}
                        </Alert>
                    )}

                    {taskQuery.data && (
                        <Stack spacing={2}>
                            <Paper sx={{ padding: 2 }} variant="outlined">
                                <Stack
                                    direction={{ xs: 'column', sm: 'row' }}
                                    spacing={1.5}
                                    sx={{ alignItems: { sm: 'center' } }}
                                >
                                    <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                                        <Typography color="text.secondary" variant="overline">
                                            {projectQuery.data?.name ?? 'Project task'}
                                        </Typography>
                                        <Typography component="h2" variant="h5">
                                            {taskQuery.data.title}
                                        </Typography>
                                        {taskQuery.data.description && (
                                            <Typography color="text.secondary" variant="body2">
                                                {taskQuery.data.description}
                                            </Typography>
                                        )}
                                    </Box>
                                    <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                                        <Chip label={statusLabels[taskQuery.data.status]} size="small" />
                                        <Chip label={taskQuery.data.priority.toLowerCase()} size="small" variant="outlined" />
                                    </Stack>
                                </Stack>
                                <Typography color="text.secondary" sx={{ marginTop: 1.25 }} variant="caption">
                                    <Link to={`/projects/${projectId}?task=${taskId}`}>Open project task</Link>
                                    {!canManageRelationships && ' · Relationship changes are read-only for your current access.'}
                                </Typography>
                            </Paper>

                            <TaskRelationshipsPanel
                                canManage={canManageRelationships}
                                onFeedback={setFeedback}
                                onOpenTask={openRelatedTask}
                                projectId={projectId}
                                task={taskQuery.data}
                                tenantId={tenantId}
                            />
                        </Stack>
                    )}
                </Box>
            </Box>
        </Stack>
    )
}
