import AddRoundedIcon from '@mui/icons-material/AddRounded'
import AssignmentIndOutlinedIcon from '@mui/icons-material/AssignmentIndOutlined'
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded'
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded'
import SearchRoundedIcon from '@mui/icons-material/SearchRounded'
import SyncAltRoundedIcon from '@mui/icons-material/SyncAltRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
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
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
    TableSortLabel,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent, MouseEvent } from 'react'
import { useState } from 'react'

import { useProjectMember, useProjectMembers } from '../hooks/useProjectMembers'
import { useProjectTasks } from '../hooks/useProjectTasks'
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

export function ProjectTasksSection({
    tenantId,
    projectId,
    userId,
    canReadTasksByPermission,
    canManageTasksByPermission,
    projectArchived,
    onFeedback,
}: ProjectTasksSectionProps) {
    const [page, setPage] = useState(0)
    const [size, setSize] = useState(10)
    const [searchDraft, setSearchDraft] = useState('')
    const [search, setSearch] = useState('')
    const [status, setStatus] = useState<StatusFilter>('ALL')
    const [priority, setPriority] = useState<PriorityFilter>('ALL')
    const [assigneeUserId, setAssigneeUserId] = useState('ALL')
    const [sortBy, setSortBy] = useState<ProjectTaskSortField>('createdAt')
    const [sortDir, setSortDir] = useState<SortDirection>('desc')
    const [createDialogOpen, setCreateDialogOpen] = useState(false)
    const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null)
    const [selectedTask, setSelectedTask] = useState<ProjectTask | null>(null)
    const [activeDialog, setActiveDialog] = useState<TaskDialog>(null)

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
        page,
        size,
        sortBy,
        sortDir,
        ...(status === 'ALL' ? {} : { status }),
        ...(priority === 'ALL' ? {} : { priority }),
        ...(assigneeUserId === 'ALL' ? {} : { assigneeUserId }),
        ...(search ? { search } : {}),
    }

    const tasksQuery = useProjectTasks(tenantId, projectId, queryParams, canReadTasks)

    const members = memberOptionsQuery.data?.content ?? []
    const hasFilters =
        search.length > 0 || status !== 'ALL' || priority !== 'ALL' || assigneeUserId !== 'ALL'

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

    const selectedTaskCanUpdateStatus = Boolean(
        selectedTask && (canManageTasks || selectedTask.assigneeUserId === userId),
    )

    if (!canReadTasks && taskAccessResolved) {
        return (
            <Alert severity="info" sx={{ marginTop: 4 }}>
                Project tasks are not available for your current access scope.
            </Alert>
        )
    }

    return (
        <Box sx={{ marginTop: 4 }}>
            <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={2}
                sx={{
                    alignItems: { sm: 'center' },
                    justifyContent: 'space-between',
                }}
            >
                <Box>
                    <Typography component="h2" variant="h5">
                        Project tasks
                    </Typography>
                    <Typography color="text.secondary" variant="body2">
                        Track assignments, priorities, due dates, and progress.
                    </Typography>
                </Box>

                <Stack direction="row" spacing={1}>
                    {canManageTasks && (
                        <Button
                            disabled={projectArchived}
                            onClick={() => {
                                setCreateDialogOpen(true)
                            }}
                            startIcon={<AddRoundedIcon />}
                            variant="contained"
                        >
                            Create task
                        </Button>
                    )}
                    <Button
                        disabled={tasksQuery.isFetching}
                        onClick={() => {
                            void Promise.all([tasksQuery.refetch(), memberOptionsQuery.refetch()])
                        }}
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
                        Refresh tasks
                    </Button>
                </Stack>
            </Stack>

            <Paper
                component="form"
                onSubmit={submitSearch}
                variant="outlined"
                sx={{ marginTop: 2, padding: 2 }}
            >
                <Stack
                    direction={{ xs: 'column', lg: 'row' }}
                    spacing={2}
                    sx={{ alignItems: { lg: 'center' } }}
                >
                    <TextField
                        label="Search project tasks"
                        onChange={(event) => {
                            setSearchDraft(event.target.value)
                        }}
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

            <Paper variant="outlined" sx={{ marginTop: 2, overflow: 'hidden' }}>
                {tasksQuery.isFetching && !tasksQuery.isPending && (
                    <LinearProgress aria-label="Updating project tasks" />
                )}

                {tasksQuery.isPending && <TaskTableSkeleton />}

                {tasksQuery.isError && (
                    <Alert
                        action={
                            <Button
                                color="inherit"
                                onClick={() => {
                                    void tasksQuery.refetch()
                                }}
                                size="small"
                            >
                                Retry
                            </Button>
                        }
                        severity="error"
                        sx={{ margin: 2 }}
                    >
                        {getErrorMessage(
                            tasksQuery.error,
                            'The project tasks could not be loaded.',
                        )}
                    </Alert>
                )}

                {tasksQuery.isSuccess && (
                    <>
                        <TableContainer>
                            <Table aria-label="Project tasks">
                                <TableHead>
                                    <TableRow>
                                        <TableCell
                                            sortDirection={sortBy === 'title' ? sortDir : false}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'title'}
                                                direction={sortBy === 'title' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('title')
                                                }}
                                            >
                                                Task
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={sortBy === 'status' ? sortDir : false}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'status'}
                                                direction={sortBy === 'status' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('status')
                                                }}
                                            >
                                                Status
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell
                                            sortDirection={sortBy === 'priority' ? sortDir : false}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'priority'}
                                                direction={sortBy === 'priority' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('priority')
                                                }}
                                            >
                                                Priority
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell>Assignee</TableCell>
                                        <TableCell
                                            sortDirection={sortBy === 'dueAt' ? sortDir : false}
                                        >
                                            <TableSortLabel
                                                active={sortBy === 'dueAt'}
                                                direction={sortBy === 'dueAt' ? sortDir : 'asc'}
                                                onClick={() => {
                                                    changeSort('dueAt')
                                                }}
                                            >
                                                Due
                                            </TableSortLabel>
                                        </TableCell>
                                        <TableCell align="right">Actions</TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {tasksQuery.data.content.map((task) => {
                                        const canUpdateStatus =
                                            canManageTasks || task.assigneeUserId === userId
                                        const hasActions =
                                            !projectArchived &&
                                            task.status !== 'CANCELLED' &&
                                            (canManageTasks || canUpdateStatus)

                                        return (
                                            <TableRow key={task.id}>
                                                <TableCell>
                                                    <Typography
                                                        variant="body2"
                                                        sx={{ fontWeight: 600 }}
                                                    >
                                                        {task.title}
                                                    </Typography>
                                                    <Typography
                                                        color="text.secondary"
                                                        variant="caption"
                                                    >
                                                        {task.description || 'No description'}
                                                    </Typography>
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={statusColors[task.status]}
                                                        label={taskStatusLabels[task.status]}
                                                        size="small"
                                                        variant="outlined"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Chip
                                                        color={priorityColors[task.priority]}
                                                        label={taskPriorityLabels[task.priority]}
                                                        size="small"
                                                    />
                                                </TableCell>
                                                <TableCell>
                                                    <Typography variant="body2">
                                                        {task.assigneeName || 'Unassigned'}
                                                    </Typography>
                                                    {task.assigneeEmail && (
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="caption"
                                                        >
                                                            {task.assigneeEmail}
                                                        </Typography>
                                                    )}
                                                </TableCell>
                                                <TableCell>
                                                    <Stack spacing={0.5}>
                                                        <Typography variant="body2">
                                                            {formatDateTime(task.dueAt)}
                                                        </Typography>
                                                        {isOverdue(task) && (
                                                            <Chip
                                                                color="error"
                                                                label="Overdue"
                                                                size="small"
                                                                sx={{ alignSelf: 'flex-start' }}
                                                            />
                                                        )}
                                                    </Stack>
                                                </TableCell>
                                                <TableCell align="right">
                                                    {hasActions && (
                                                        <IconButton
                                                            aria-label={`Manage ${task.title}`}
                                                            onClick={(event) => {
                                                                openTaskMenu(event, task)
                                                            }}
                                                            size="small"
                                                        >
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
                                    {hasFilters
                                        ? 'Try changing or clearing the current filters.'
                                        : 'This project does not have any tasks.'}
                                </Typography>
                            </Box>
                        )}

                        <Divider />
                        <TablePagination
                            component="div"
                            count={tasksQuery.data.totalElements}
                            onPageChange={(_event, nextPage) => {
                                setPage(nextPage)
                            }}
                            onRowsPerPageChange={(event) => {
                                setPage(0)
                                setSize(Number(event.target.value))
                            }}
                            page={page}
                            rowsPerPage={size}
                            rowsPerPageOptions={[10, 25, 50]}
                        />
                    </>
                )}
            </Paper>

            <Menu anchorEl={menuAnchor} onClose={closeTaskMenu} open={Boolean(menuAnchor)}>
                {selectedTaskCanUpdateStatus && (
                    <MenuItem
                        onClick={() => {
                            openTaskDialog('status')
                        }}
                    >
                        <ListItemIcon>
                            <SyncAltRoundedIcon fontSize="small" />
                        </ListItemIcon>
                        Change status
                    </MenuItem>
                )}
                {canManageTasks && (
                    <>
                        <MenuItem
                            onClick={() => {
                                openTaskDialog('edit')
                            }}
                        >
                            <ListItemIcon>
                                <EditOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Edit task
                        </MenuItem>
                        <MenuItem
                            onClick={() => {
                                openTaskDialog('assignee')
                            }}
                        >
                            <ListItemIcon>
                                <AssignmentIndOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Change assignee
                        </MenuItem>
                        <MenuItem
                            onClick={() => {
                                openTaskDialog('cancel')
                            }}
                        >
                            <ListItemIcon>
                                <BlockOutlinedIcon fontSize="small" />
                            </ListItemIcon>
                            Cancel task
                        </MenuItem>
                    </>
                )}
            </Menu>

            {createDialogOpen && (
                <CreateProjectTaskDialog
                    members={members}
                    onClose={() => {
                        setCreateDialogOpen(false)
                    }}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    tenantId={tenantId}
                />
            )}

            {activeDialog === 'edit' && selectedTask && (
                <EditProjectTaskDialog
                    onClose={closeTaskDialog}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    task={selectedTask}
                    tenantId={tenantId}
                />
            )}

            {activeDialog === 'status' && selectedTask && (
                <ChangeProjectTaskStatusDialog
                    onClose={closeTaskDialog}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    task={selectedTask}
                    tenantId={tenantId}
                />
            )}

            {activeDialog === 'assignee' && selectedTask && (
                <AssignProjectTaskDialog
                    members={members}
                    onClose={closeTaskDialog}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    task={selectedTask}
                    tenantId={tenantId}
                />
            )}

            {activeDialog === 'cancel' && selectedTask && (
                <CancelProjectTaskDialog
                    onClose={closeTaskDialog}
                    onSuccess={onFeedback}
                    projectId={projectId}
                    task={selectedTask}
                    tenantId={tenantId}
                />
            )}
        </Box>
    )
}
