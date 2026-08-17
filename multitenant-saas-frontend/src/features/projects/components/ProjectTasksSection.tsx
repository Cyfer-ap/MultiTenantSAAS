import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined'
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined'
import CalendarTodayOutlinedIcon from '@mui/icons-material/CalendarTodayOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import SyncAltRoundedIcon from '@mui/icons-material/SyncAltRounded'
import TableRowsRoundedIcon from '@mui/icons-material/TableRowsRounded'
import ViewKanbanRoundedIcon from '@mui/icons-material/ViewKanbanRounded'
import {
    Alert,
    Avatar,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
    FormControlLabel,
    IconButton,
    InputAdornment,
    InputLabel,
    LinearProgress,
    ListItemIcon,
    Menu,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Stack,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Tooltip,
    Typography,
} from '@mui/material'
import type { DragEvent, FormEvent, MouseEvent } from 'react'
import { useMemo, useState } from 'react'

import { useProjectMember, useProjectMembers } from '../hooks/useProjectMembers'
import { useProjectTasks, useUpdateProjectTaskStatus } from '../hooks/useProjectTasks'
import type {
    ProjectTask,
    ProjectTaskPriority,
    ProjectTaskSortField,
    ProjectTaskStatus,
    ProjectTasksQueryParams,
} from '../types/projectTasks'
import type { ProjectMembersQueryParams, SortDirection } from '../types/projects'
import {
    AssignProjectTaskDialog,
    CancelProjectTaskDialog,
    ChangeProjectTaskStatusDialog,
    CreateProjectTaskDialog,
    EditProjectTaskDialog,
} from './ProjectTaskDialogs'

type StatusFilter = ProjectTaskStatus | 'ALL'
type PriorityFilter = ProjectTaskPriority | 'ALL'
type TaskDialog = 'edit' | 'status' | 'assignee' | 'cancel' | null
type TaskView = 'board' | 'table'
type ActiveTaskStatus = Exclude<ProjectTaskStatus, 'CANCELLED'>

interface ProjectTasksSectionProps {
    tenantId: string
    projectId: string
    userId: string
    canReadTasksByPermission: boolean
    canManageTasksByPermission: boolean
    projectArchived: boolean
    onFeedback: (message: string) => void
}

const taskStatusLabels: Record<ProjectTaskStatus, string> = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
}

const activeStatuses: ActiveTaskStatus[] = ['TODO', 'IN_PROGRESS', 'BLOCKED', 'COMPLETED']

const taskPriorityLabels: Record<ProjectTaskPriority, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    URGENT: 'Urgent',
}

const statusColors = {
    TODO: 'default',
    IN_PROGRESS: 'primary',
    BLOCKED: 'warning',
    COMPLETED: 'success',
    CANCELLED: 'default',
} as const

const priorityColors = {
    LOW: 'default',
    MEDIUM: 'info',
    HIGH: 'warning',
    URGENT: 'error',
} as const

function formatDateTime(value: string | null): string {
    if (!value) {
        return 'No due date'
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return '—'
    }

    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function isOverdue(task: ProjectTask): boolean {
    return Boolean(
        task.dueAt &&
            task.status !== 'COMPLETED' &&
            task.status !== 'CANCELLED' &&
            new Date(task.dueAt).getTime() < Date.now(),
    )
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function getInitials(name: string | null): string {
    if (!name) {
        return '?'
    }

    return name
        .split(/\s+/)
        .filter(Boolean)
        .slice(0, 2)
        .map((part) => part[0]?.toUpperCase())
        .join('')
}

function TaskTableSkeleton() {
    return (
        <Box aria-label="Loading project tasks" role="status">
            {[0, 1, 2, 3].map((row) => (
                <Stack direction="row" key={row} spacing={2} sx={{ padding: 2 }}>
                    <Skeleton width="30%" />
                    <Skeleton width="15%" />
                    <Skeleton width="12%" />
                    <Skeleton width="20%" />
                    <Skeleton width="16%" />
                </Stack>
            ))}
        </Box>
    )
}

function TaskBoardSkeleton() {
    return (
        <Box sx={{ display: 'flex', gap: 2, overflowX: 'auto', paddingBottom: 1 }}>
            {activeStatuses.map((status) => (
                <Paper key={status} variant="outlined" sx={{ minWidth: 280, padding: 2, width: 320 }}>
                    <Skeleton width="45%" />
                    <Skeleton height={130} sx={{ marginTop: 2 }} variant="rounded" />
                    <Skeleton height={110} sx={{ marginTop: 1 }} variant="rounded" />
                </Paper>
            ))}
        </Box>
    )
}

export function ProjectTasksSection({
    tenantId,
    projectId,
    userId,
    canReadTasksByPermission,
    canManageTasksByPermission,
    projectArchived,
    onFeedback,
}: ProjectTasksSectionProps) {
    const [view, setView] = useState<TaskView>('board')
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] = useState<StatusFilter>('ALL')
    const [priority, setPriority] = useState<PriorityFilter>('ALL')
    const [assigneeUserId, setAssigneeUserId] = useState('ALL')
    const [sortBy, setSortBy] = useState<ProjectTaskSortField>('createdAt')
    const [sortDir, setSortDir] = useState<SortDirection>('desc')
    const [showCancelled, setShowCancelled] = useState(false)
    const [createDialogOpen, setCreateDialogOpen] = useState(false)
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
    const [selectedTask, setSelectedTask] = useState<ProjectTask | null>(null)
    const [activeDialog, setActiveDialog] = useState<TaskDialog>(null)
    const [draggedTaskId, setDraggedTaskId] = useState<string | null>(null)
    const [movingTaskId, setMovingTaskId] = useState<string | null>(null)
    const [boardError, setBoardError] = useState<string | null>(null)

    const needsMembershipLookup = !canReadTasksByPermission || !canManageTasksByPermission
    const currentMemberQuery = useProjectMember(tenantId, projectId, userId, needsMembershipLookup)
    const isCurrentUserProjectMember = currentMemberQuery.isSuccess
    const isCurrentUserProjectLead = currentMemberQuery.data?.projectRole === 'PROJECT_LEAD'
    const canReadTasks = canReadTasksByPermission || isCurrentUserProjectMember
    const canManageTasks = canManageTasksByPermission || isCurrentUserProjectLead
    const taskAccessResolved =
        canReadTasksByPermission || currentMemberQuery.isSuccess || currentMemberQuery.isError

    const memberOptionsParams: ProjectMembersQueryParams = {
        page: 0,
        size: 100,
        sortBy: 'assignedAt',
        sortDir: 'asc',
    }

    const memberOptionsQuery = useProjectMembers(
        tenantId,
        projectId,
        memberOptionsParams,
        canReadTasks,
    )

    const queryParams: ProjectTasksQueryParams = {
        page: view === 'board' ? 0 : page,
        size: view === 'board' ? 100 : size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(priority === 'ALL' ? {} : { priority }),
        ...(assigneeUserId === 'ALL' ? {} : { assigneeUserId }),
        ...(search ? { search } : {}),
    }

    const tasksQuery = useProjectTasks(tenantId, projectId, queryParams, canReadTasks)
    const updateStatusMutation = useUpdateProjectTaskStatus(tenantId, projectId)
    const members = memberOptionsQuery.data?.content ?? []
    const hasFilters =
        search.length > 0 || status !== 'ALL' || priority !== 'ALL' || assigneeUserId !== 'ALL'

    const tasksByStatus = useMemo(() => {
        const grouped: Record<ProjectTaskStatus, ProjectTask[]> = {
            TODO: [],
            IN_PROGRESS: [],
            BLOCKED: [],
            COMPLETED: [],
            CANCELLED: [],
        }

        for (const task of tasksQuery.data?.content ?? []) {
            grouped[task.status].push(task)
        }

        return grouped
    }, [tasksQuery.data?.content])

    const submitSearch = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        setPage(0)
        setSearch(searchDraft.trim())
    }

    const clearFilters = (): void => {
        setPage(0)
        setSearchDraft('')
        setSearch('')
        setStatus('ALL')
        setPriority('ALL')
        setAssigneeUserId('ALL')
    }

    const changeSort = (nextSortBy: ProjectTaskSortField): void => {
        setPage(0)

        if (sortBy === nextSortBy) {
            setSortDir((current) => (current === 'asc' ? 'desc' : 'asc'))
            return
        }

        setSortBy(nextSortBy)
        setSortDir('asc')
    }

    const openTaskMenu = (event: MouseEvent<HTMLButtonElement>, task: ProjectTask): void => {
        setSelectedTask(task)
        setMenuAnchor(event.currentTarget)
    }

    const closeTaskMenu = (): void => {
        setMenuAnchor(null)
    }

    const openTaskDialog = (dialog: Exclude<TaskDialog, null>): void => {
        setActiveDialog(dialog)
        closeTaskMenu()
    }

    const closeTaskDialog = (): void => {
        setActiveDialog(null)
        setSelectedTask(null)
    }

    const canUpdateTaskStatus = (task: ProjectTask): boolean =>
        !projectArchived &&
        task.status !== 'CANCELLED' &&
        (canManageTasks || task.assigneeUserId === userId)

    const handleDragStart = (event: DragEvent<HTMLElement>, task: ProjectTask): void => {
        if (!canUpdateTaskStatus(task)) {
            event.preventDefault()
            return
        }

        setDraggedTaskId(task.id)
        setBoardError(null)
        event.dataTransfer.effectAllowed = 'move'
        event.dataTransfer.setData('text/plain', task.id)
    }

    const handleDrop = async (event: DragEvent<HTMLElement>, nextStatus: ActiveTaskStatus) => {
        event.preventDefault()
        const taskId = draggedTaskId || event.dataTransfer.getData('text/plain')
        const task = tasksQuery.data?.content.find((candidate) => candidate.id === taskId)
        setDraggedTaskId(null)

        if (!task || !canUpdateTaskStatus(task) || task.status === nextStatus) {
            return
        }

        setMovingTaskId(task.id)
        setBoardError(null)

        try {
            await updateStatusMutation.mutateAsync({ taskId: task.id, input: { status: nextStatus } })
            onFeedback(`Moved “${task.title}” to ${taskStatusLabels[nextStatus]}.`)
        } catch (error) {
            setBoardError(getErrorMessage(error, 'The task could not be moved.'))
        } finally {
            setMovingTaskId(null)
        }
    }

    const selectedTaskCanUpdateStatus = Boolean(selectedTask && canUpdateTaskStatus(selectedTask))

    if (!canReadTasks && taskAccessResolved) {
        return (
            <Alert severity="info" sx={{ marginTop: 4 }}>
                Project tasks are not available for your current access scope.
            </Alert>
        )
    }

    const renderTaskCard = (task: ProjectTask, readOnly = false) => {
        const canMove = !readOnly && canUpdateTaskStatus(task)
        const hasActions =
            !projectArchived &&
            task.status !== 'CANCELLED' &&
            (canManageTasks || canUpdateTaskStatus(task))
        const overdue = isOverdue(task)

        return (
            <Paper
                aria-label={`${task.title} task card`}
                draggable={canMove}
                key={task.id}
                onDragEnd={() => setDraggedTaskId(null)}
                onDragStart={(event) => handleDragStart(event, task)}
                variant="outlined"
                sx={{
                    cursor: canMove ? 'grab' : 'default',
                    opacity: movingTaskId === task.id ? 0.55 : 1,
                    padding: 1.5,
                    transition: 'box-shadow 120ms ease, opacity 120ms ease',
                    '&:hover': { boxShadow: 2 },
                    '&:active': canMove ? { cursor: 'grabbing' } : undefined,
                }}
            >
                <Stack spacing={1.25}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'flex-start' }}>
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                            <Typography sx={{ fontWeight: 700 }} variant="body2">
                                {task.title}
                            </Typography>
                            {task.description && (
                                <Typography
                                    color="text.secondary"
                                    variant="caption"
                                    sx={{
                                        display: '-webkit-box',
                                        overflow: 'hidden',
                                        WebkitBoxOrient: 'vertical',
                                        WebkitLineClamp: 2,
                                    }}
                                >
                                    {task.description}
                                </Typography>
                            )}
                        </Box>
                        {hasActions && (
                            <IconButton
                                aria-label={`Manage ${task.title}`}
                                onClick={(event) => openTaskMenu(event, task)}
                                size="small"
                            >
                                <MoreVertRoundedIcon fontSize="small" />
                            </IconButton>
                        )}
                    </Stack>

                    <Stack direction="row" spacing={0.75} sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                        <Chip
                            color={priorityColors[task.priority]}
                            label={taskPriorityLabels[task.priority]}
                            size="small"
                        />
                        {overdue && <Chip color="error" label="Overdue" size="small" variant="outlined" />}
                    </Stack>

                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <Avatar sx={{ height: 26, width: 26, fontSize: 11 }}>
                            {getInitials(task.assigneeName)}
                        </Avatar>
                        <Typography color="text.secondary" noWrap variant="caption" sx={{ flexGrow: 1 }}>
                            {task.assigneeName || 'Unassigned'}
                        </Typography>
                        <Tooltip title={formatDateTime(task.dueAt)}>
                            <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
                                <CalendarTodayOutlinedIcon color={overdue ? 'error' : 'inherit'} fontSize="inherit" />
                                <Typography color={overdue ? 'error' : 'text.secondary'} variant="caption">
                                    {task.dueAt
                                        ? new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric' }).format(
                                              new Date(task.dueAt),
                                          )
                                        : 'No due'}
                                </Typography>
                            </Stack>
                        </Tooltip>
                    </Stack>
                </Stack>
            </Paper>
        )
    }

    return (
        <Box sx={{ marginTop: 4 }}>
            <Stack
                direction={{ xs: 'column', md: 'row' }}
                spacing={2}
                sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
            >
                <Box>
                    <Typography component="h2" variant="h5">
                        Project tasks
                    </Typography>
                    <Typography color="text.secondary" variant="body2">
                        Move work across the board, manage assignments, and keep delivery visible.
                    </Typography>
                </Box>

                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <ToggleButtonGroup
                        aria-label="Task view"
                        exclusive
                        onChange={(_event, nextView: TaskView | null) => {
                            if (nextView) {
                                setView(nextView)
                            }
                        }}
                        size="small"
                        value={view}
                    >
                        <ToggleButton aria-label="Board view" value="board">
                            <ViewKanbanRoundedIcon fontSize="small" sx={{ marginRight: 0.75 }} />
                            Board
                        </ToggleButton>
                        <ToggleButton aria-label="Table view" value="table">
                            <TableRowsRoundedIcon fontSize="small" sx={{ marginRight: 0.75 }} />
                            Table
                        </ToggleButton>
                    </ToggleButtonGroup>
                    {canManageTasks && (
                        <Button
                            disabled={projectArchived}
                            onClick={() => setCreateDialogOpen(true)}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Create task
                        </Button>
                    )}
                    <Button
                        disabled={tasksQuery.isFetching}
                        onClick={() => void Promise.all([tasksQuery.refetch(), memberOptionsQuery.refetch()])}
                        startIcon={
                            tasksQuery.isFetching ? (
                                <CircularProgress color="inherit" size={16} />
                            ) : (
                                <RefreshRoundedIcon />
                            )
                        }
                        size="small"
                        variant="outlined"
                    >
                        Refresh
                    </Button>
                </Stack>
            </Stack>

            <Paper component="form" onSubmit={submitSearch} variant="outlined" sx={{ marginTop: 2, padding: 2 }}>
                <Stack direction={{ xs: 'column', lg: 'row' }} spacing={2} sx={{ alignItems: { lg: 'center' } }}>
                    <TextField
                        label="Search project tasks"
                        onChange={(event) => setSearchDraft(event.target.value)}
                        placeholder="Title or description"
                        size="small"
                        slotProps={{
                            input: {
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchRoundedIcon fontSize="small" />
                                    </InputAdornment>
                                ),
                            },
                        }}
                        sx={{ minWidth: 240, flexGrow: 1 }}
                        value={searchDraft}
                    />

                    <FormControl size="small" sx={{ minWidth: 150 }}>
                        <InputLabel id="task-status-filter-label">Status</InputLabel>
                        <Select
                            label="Status"
                            labelId="task-status-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setStatus(event.target.value as StatusFilter)
                            }}
                            value={status}
                        >
                            <MenuItem value="ALL">All statuses</MenuItem>
                            {Object.entries(taskStatusLabels).map(([value, label]) => (
                                <MenuItem key={value} value={value}>
                                    {label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <FormControl size="small" sx={{ minWidth: 140 }}>
                        <InputLabel id="task-priority-filter-label">Priority</InputLabel>
                        <Select
                            label="Priority"
                            labelId="task-priority-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setPriority(event.target.value as PriorityFilter)
                            }}
                            value={priority}
                        >
                            <MenuItem value="ALL">All priorities</MenuItem>
                            {Object.entries(taskPriorityLabels).map(([value, label]) => (
                                <MenuItem key={value} value={value}>
                                    {label}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <FormControl size="small" sx={{ minWidth: 190 }}>
                        <InputLabel id="task-assignee-filter-label">Assignee</InputLabel>
                        <Select
                            label="Assignee"
                            labelId="task-assignee-filter-label"
                            onChange={(event) => {
                                setPage(0)
                                setAssigneeUserId(event.target.value)
                            }}
                            value={assigneeUserId}
                        >
                            <MenuItem value="ALL">All assignees</MenuItem>
                            {members.map((member) => (
                                <MenuItem key={member.userId} value={member.userId}>
                                    {member.fullName}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>

                    <Button type="submit" variant="contained">
                        Search
                    </Button>
                    {hasFilters && <Button onClick={clearFilters}>Clear filters</Button>}
                </Stack>
            </Paper>

            {boardError && (
                <Alert onClose={() => setBoardError(null)} severity="error" sx={{ marginTop: 2 }}>
                    {boardError}
                </Alert>
            )}

            {tasksQuery.isFetching && !tasksQuery.isPending && <LinearProgress aria-label="Updating project tasks" sx={{ marginTop: 2 }} />}

            {tasksQuery.isError && (
                <Alert
                    action={
                        <Button color="inherit" onClick={() => void tasksQuery.refetch()} size="small">
                            Retry
                        </Button>
                    }
                    severity="error"
                    sx={{ marginTop: 2 }}
                >
                    {getErrorMessage(tasksQuery.error, 'The project tasks could not be loaded.')}
                </Alert>
            )}

            {view === 'board' && tasksQuery.isPending && <Box sx={{ marginTop: 2 }}><TaskBoardSkeleton /></Box>}
            {view === 'table' && tasksQuery.isPending && <Paper variant="outlined" sx={{ marginTop: 2 }}><TaskTableSkeleton /></Paper>}

            {view === 'board' && tasksQuery.isSuccess && (
                <Box sx={{ marginTop: 2 }}>
                    {tasksQuery.data.totalElements > tasksQuery.data.content.length && (
                        <Alert severity="info" sx={{ marginBottom: 2 }}>
                            This board shows the first 100 matching tasks. Refine the filters to narrow the workspace.
                        </Alert>
                    )}

                    <Box
                        aria-label="Project task Kanban board"
                        sx={{
                            display: 'flex',
                            gap: 2,
                            overflowX: 'auto',
                            paddingBottom: 1,
                            scrollSnapType: { xs: 'x proximity', md: 'none' },
                        }}
                    >
                        {activeStatuses.map((laneStatus) => {
                            const laneTasks = tasksByStatus[laneStatus]

                            return (
                                <Paper
                                    aria-label={`${taskStatusLabels[laneStatus]} lane`}
                                    key={laneStatus}
                                    onDragOver={(event) => {
                                        if (draggedTaskId) {
                                            event.preventDefault()
                                            event.dataTransfer.dropEffect = 'move'
                                        }
                                    }}
                                    onDrop={(event) => void handleDrop(event, laneStatus)}
                                    variant="outlined"
                                    sx={{
                                        backgroundColor: 'action.hover',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        flexShrink: 0,
                                        minHeight: 260,
                                        minWidth: 280,
                                        padding: 1.5,
                                        scrollSnapAlign: 'start',
                                        width: { xs: '82vw', sm: 320 },
                                    }}
                                >
                                    <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between', marginBottom: 1.5 }}>
                                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                            <Typography sx={{ fontWeight: 700 }} variant="subtitle2">
                                                {taskStatusLabels[laneStatus]}
                                            </Typography>
                                            <Chip label={laneTasks.length} size="small" />
                                        </Stack>
                                        <Chip color={statusColors[laneStatus]} label=" " size="small" sx={{ minWidth: 12, width: 12 }} />
                                    </Stack>

                                    <Stack spacing={1} sx={{ flexGrow: 1 }}>
                                        {laneTasks.map((task) => renderTaskCard(task))}
                                        {laneTasks.length === 0 && (
                                            <Box
                                                sx={{
                                                    alignItems: 'center',
                                                    border: 1,
                                                    borderColor: 'divider',
                                                    borderRadius: 1,
                                                    borderStyle: 'dashed',
                                                    display: 'flex',
                                                    flexGrow: 1,
                                                    justifyContent: 'center',
                                                    minHeight: 92,
                                                    padding: 2,
                                                }}
                                            >
                                                <Typography color="text.secondary" textAlign="center" variant="caption">
                                                    {draggedTaskId ? `Drop here to move to ${taskStatusLabels[laneStatus]}` : 'No tasks in this lane'}
                                                </Typography>
                                            </Box>
                                        )}
                                    </Stack>
                                </Paper>
                            )
                        })}
                    </Box>

                    <FormControlLabel
                        control={
                            <Switch
                                checked={showCancelled}
                                onChange={(event) => setShowCancelled(event.target.checked)}
                            />
                        }
                        label={`Show cancelled tasks (${tasksByStatus.CANCELLED.length})`}
                        sx={{ marginTop: 1 }}
                    />

                    {showCancelled && (
                        <Paper variant="outlined" sx={{ marginTop: 1.5, padding: 2 }}>
                            <Typography sx={{ fontWeight: 700, marginBottom: 1 }} variant="subtitle2">
                                Cancelled · read only
                            </Typography>
                            {tasksByStatus.CANCELLED.length === 0 ? (
                                <Typography color="text.secondary" variant="body2">No cancelled tasks match the current filters.</Typography>
                            ) : (
                                <Box sx={{ display: 'grid', gap: 1, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))', xl: 'repeat(3, minmax(0, 1fr))' } }}>
                                    {tasksByStatus.CANCELLED.map((task) => renderTaskCard(task, true))}
                                </Box>
                            )}
                        </Paper>
                    )}
                </Box>
            )}

            {view === 'table' && tasksQuery.isSuccess && (
                <Paper variant="outlined" sx={{ marginTop: 2, overflow: 'hidden' }}>
                    <TableContainer>
                        <Table aria-label="Project tasks">
                            <TableHead>
                                <TableRow>
                                    <TableCell sortDirection={sortBy === 'title' ? sortDir : false}>
                                        <TableSortLabel active={sortBy === 'title'} direction={sortBy === 'title' ? sortDir : 'asc'} onClick={() => changeSort('title')}>Task</TableSortLabel>
                                    </TableCell>
                                    <TableCell sortDirection={sortBy === 'status' ? sortDir : false}>
                                        <TableSortLabel active={sortBy === 'status'} direction={sortBy === 'status' ? sortDir : 'asc'} onClick={() => changeSort('status')}>Status</TableSortLabel>
                                    </TableCell>
                                    <TableCell sortDirection={sortBy === 'priority' ? sortDir : false}>
                                        <TableSortLabel active={sortBy === 'priority'} direction={sortBy === 'priority' ? sortDir : 'asc'} onClick={() => changeSort('priority')}>Priority</TableSortLabel>
                                    </TableCell>
                                    <TableCell>Assignee</TableCell>
                                    <TableCell sortDirection={sortBy === 'dueAt' ? sortDir : false}>
                                        <TableSortLabel active={sortBy === 'dueAt'} direction={sortBy === 'dueAt' ? sortDir : 'asc'} onClick={() => changeSort('dueAt')}>Due</TableSortLabel>
                                    </TableCell>
                                    <TableCell align="right">Actions</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {tasksQuery.data.content.map((task) => {
                                    const hasActions = !projectArchived && task.status !== 'CANCELLED' && (canManageTasks || canUpdateTaskStatus(task))

                                    return (
                                        <TableRow key={task.id}>
                                            <TableCell>
                                                <Typography sx={{ fontWeight: 600 }} variant="body2">{task.title}</Typography>
                                                <Typography color="text.secondary" variant="caption">{task.description || 'No description'}</Typography>
                                            </TableCell>
                                            <TableCell><Chip color={statusColors[task.status]} label={taskStatusLabels[task.status]} size="small" variant="outlined" /></TableCell>
                                            <TableCell><Chip color={priorityColors[task.priority]} label={taskPriorityLabels[task.priority]} size="small" /></TableCell>
                                            <TableCell>
                                                <Typography variant="body2">{task.assigneeName || 'Unassigned'}</Typography>
                                                {task.assigneeEmail && <Typography color="text.secondary" variant="caption">{task.assigneeEmail}</Typography>}
                                            </TableCell>
                                            <TableCell>
                                                <Stack spacing={0.5}>
                                                    <Typography variant="body2">{formatDateTime(task.dueAt)}</Typography>
                                                    {isOverdue(task) && <Chip color="error" label="Overdue" size="small" sx={{ alignSelf: 'flex-start' }} />}
                                                </Stack>
                                            </TableCell>
                                            <TableCell align="right">
                                                {hasActions && (
                                                    <IconButton aria-label={`Manage ${task.title}`} onClick={(event) => openTaskMenu(event, task)} size="small">
                                                        <MoreVertRoundedIcon />
                                                    </IconButton>
                                                )}
                                            </TableCell>
                                        </TableRow>
                                    )
                                })}
                            </TableBody>
                        </Table>
                    </TableContainer>

                    {tasksQuery.data.content.length === 0 && (
                        <Box sx={{ padding: 4, textAlign: 'center' }}>
                            <Typography variant="h6">No project tasks found</Typography>
                            <Typography color="text.secondary" variant="body2">
                                {hasFilters ? 'Try changing or clearing the current filters.' : 'This project does not have any tasks.'}
                            </Typography>
                        </Box>
                    )}

                    <Divider />
                    <TablePagination
                        component="div"
                        count={tasksQuery.data.totalElements}
                        onPageChange={(_event, nextPage) => setPage(nextPage)}
                        onRowsPerPageChange={(event) => {
                            setPage(0)
                            setSize(Number(event.target.value))
                        }}
                        page={page}
                        rowsPerPage={size}
                        rowsPerPageOptions={[10, 25, 50]}
                    />
                </Paper>
            )}

            <Menu anchorEl={menuAnchor} onClose={closeTaskMenu} open={Boolean(menuAnchor)}>
                {selectedTaskCanUpdateStatus && (
                    <MenuItem onClick={() => openTaskDialog('status')}>
                        <ListItemIcon><SyncAltRoundedIcon fontSize="small" /></ListItemIcon>
                        Change status
                    </MenuItem>
                )}
                {canManageTasks && selectedTask?.status !== 'CANCELLED' && (
                    <>
                        <MenuItem onClick={() => openTaskDialog('edit')}>
                            <ListItemIcon><EditOutlinedIcon fontSize="small" /></ListItemIcon>
                            Edit task
                        </MenuItem>
                        <MenuItem onClick={() => openTaskDialog('assignee')}>
                            <ListItemIcon><AssignmentIndOutlinedIcon fontSize="small" /></ListItemIcon>
                            Change assignee
                        </MenuItem>
                        <MenuItem onClick={() => openTaskDialog('cancel')}>
                            <ListItemIcon><BlockOutlinedIcon fontSize="small" /></ListItemIcon>
                            Cancel task
                        </MenuItem>
                    </>
                )}
            </Menu>

            {createDialogOpen && (
                <CreateProjectTaskDialog
                    members={members}
                    onClose={() => setCreateDialogOpen(false)}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    tenantId={tenantId}
                />
            )}
            {activeDialog === 'edit' && selectedTask && <EditProjectTaskDialog onClose={closeTaskDialog} onSuccess={onFeedback} projectId={projectId} task={selectedTask} tenantId={tenantId} />}
            {activeDialog === 'status' && selectedTask && <ChangeProjectTaskStatusDialog onClose={closeTaskDialog} onSuccess={onFeedback} projectId={projectId} task={selectedTask} tenantId={tenantId} />}
            {activeDialog === 'assignee' && selectedTask && <AssignProjectTaskDialog members={members} onClose={closeTaskDialog} onSuccess={onFeedback} projectId={projectId} task={selectedTask} tenantId={tenantId} />}
            {activeDialog === 'cancel' && selectedTask && <CancelProjectTaskDialog onClose={closeTaskDialog} onSuccess={onFeedback} projectId={projectId} task={selectedTask} tenantId={tenantId} />}
        </Box>
    )
}
