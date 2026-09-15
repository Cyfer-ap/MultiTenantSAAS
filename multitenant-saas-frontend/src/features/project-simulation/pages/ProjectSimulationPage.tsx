import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded'
import RestartAltRoundedIcon from '@mui/icons-material/RestartAltRounded'
import ScienceRoundedIcon from '@mui/icons-material/ScienceRounded'
import {
    Alert,
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
    InputLabel,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router'

import { useAuth } from '../../auth/hooks/useAuth'
import { useProjectDetails } from '../../projects/hooks/useProjectDetails'
import { useProjectMembers } from '../../projects/hooks/useProjectMembers'
import {
    useProjectSimulationBaseline,
    useRunProjectSimulation,
} from '../hooks/useProjectSimulation'
import type {
    ProjectSimulationDependencyChangeType,
    ProjectSimulationRequest,
} from '../types/projectSimulation'

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function formatDateTime(value: string | null): string {
    if (!value) return 'No due date'
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return value
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: 'medium',
        timeStyle: 'short',
    }).format(date)
}

function toDateTimeLocal(value: string | null): string {
    if (!value) return ''
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000)
    return local.toISOString().slice(0, 16)
}

export function ProjectSimulationPage() {
    const { session } = useAuth()
    const { projectId = '' } = useParams()
    const tenantId = session?.tenantId ?? ''

    const [selectedTaskId, setSelectedTaskId] = useState('')
    const [dueAtInput, setDueAtInput] = useState('')
    const [assigneeUserId, setAssigneeUserId] = useState('')
    const [dependencyType, setDependencyType] =
        useState<ProjectSimulationDependencyChangeType>('ADD')
    const [blockingTaskId, setBlockingTaskId] = useState('')
    const [dependentTaskId, setDependentTaskId] = useState('')

    const projectQuery = useProjectDetails(tenantId, projectId)
    const baselineQuery = useProjectSimulationBaseline(tenantId, projectId)
    const membersQuery = useProjectMembers(
        tenantId,
        projectId,
        { page: 0, size: 100, sortBy: 'assignedAt', sortDir: 'asc' },
        Boolean(tenantId && projectId),
    )
    const simulationMutation = useRunProjectSimulation(tenantId, projectId)

    const selectedTask = useMemo(
        () => baselineQuery.data?.tasks.find((task) => task.taskId === selectedTaskId),
        [baselineQuery.data?.tasks, selectedTaskId],
    )

    const resetScenario = (): void => {
        setSelectedTaskId('')
        setDueAtInput('')
        setAssigneeUserId('')
        setDependencyType('ADD')
        setBlockingTaskId('')
        setDependentTaskId('')
        simulationMutation.reset()
    }

    const runSimulation = (): void => {
        const taskOverrides = selectedTaskId
            ? [
                  {
                      taskId: selectedTaskId,
                      dueAt: dueAtInput ? new Date(dueAtInput).toISOString() : null,
                      clearDueAt: !dueAtInput && Boolean(selectedTask?.dueAt),
                      assigneeUserId: assigneeUserId || null,
                      clearAssignee: !assigneeUserId && Boolean(selectedTask?.assigneeUserId),
                  },
              ]
            : []

        const dependencyChanges =
            blockingTaskId && dependentTaskId
                ? [
                      {
                          type: dependencyType,
                          blockingTaskId,
                          dependentTaskId,
                      },
                  ]
                : []

        const request: ProjectSimulationRequest = {
            taskOverrides,
            dependencyChanges,
        }
        simulationMutation.mutate(request)
    }

    if (projectQuery.isPending || baselineQuery.isPending) {
        return (
            <Stack aria-label="Loading project simulator" role="status" spacing={2}>
                <Skeleton height={48} width="45%" />
                <Skeleton height={240} variant="rounded" />
                <Skeleton height={280} variant="rounded" />
            </Stack>
        )
    }

    if (projectQuery.isError || baselineQuery.isError) {
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
                        projectQuery.error ?? baselineQuery.error,
                        'The project simulation workspace could not be loaded.',
                    )}
                </Alert>
            </Stack>
        )
    }

    const baseline = baselineQuery.data
    const result = simulationMutation.data
    const canRun =
        Boolean(selectedTaskId || (blockingTaskId && dependentTaskId)) &&
        blockingTaskId !== dependentTaskId

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
                    {projectQuery.data?.name ?? 'Project'} · Advisory planning
                </Typography>
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                    <ScienceRoundedIcon color="primary" />
                    <Typography component="h1" variant="h4">
                        What-If Simulator
                    </Typography>
                </Stack>
                <Typography color="text.secondary" sx={{ marginTop: 0.75 }} variant="body2">
                    Test schedule, reassignment and dependency changes without mutating real project
                    data.
                </Typography>
            </Box>

            <Alert severity="info">
                This workspace is read-only. Running a scenario calculates impact only; it never
                changes tasks, members or dependencies.
            </Alert>

            <Box
                sx={{
                    display: 'grid',
                    gap: 2,
                    gridTemplateColumns: {
                        xs: '1fr',
                        xl: 'minmax(340px, 0.85fr) minmax(0, 1.6fr)',
                    },
                }}
            >
                <Paper sx={{ alignSelf: 'start', padding: 2.5 }} variant="outlined">
                    <Stack spacing={2}>
                        <Box>
                            <Typography variant="h6">Scenario</Typography>
                            <Typography color="text.secondary" variant="body2">
                                Baseline: {baseline.tasks.length} tasks ·{' '}
                                {baseline.dependencies.length} dependencies
                            </Typography>
                        </Box>

                        <Divider />

                        <Typography variant="subtitle2">Task change</Typography>
                        <FormControl fullWidth size="small">
                            <InputLabel id="simulation-task-label">Task</InputLabel>
                            <Select
                                label="Task"
                                labelId="simulation-task-label"
                                onChange={(event) => {
                                    const nextTaskId = event.target.value
                                    const task = baseline.tasks.find(
                                        (candidate) => candidate.taskId === nextTaskId,
                                    )
                                    setSelectedTaskId(nextTaskId)
                                    setDueAtInput(toDateTimeLocal(task?.dueAt ?? null))
                                    setAssigneeUserId(task?.assigneeUserId ?? '')
                                    simulationMutation.reset()
                                }}
                                value={selectedTaskId}
                            >
                                <MenuItem value="">No task override</MenuItem>
                                {baseline.tasks.map((task) => (
                                    <MenuItem key={task.taskId} value={task.taskId}>
                                        {task.title}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        <TextField
                            disabled={!selectedTaskId}
                            fullWidth
                            label="Simulated due date"
                            onChange={(event) => {
                                setDueAtInput(event.target.value)
                                simulationMutation.reset()
                            }}
                            size="small"
                            slotProps={{ inputLabel: { shrink: true } }}
                            type="datetime-local"
                            value={dueAtInput}
                        />

                        <FormControl disabled={!selectedTaskId} fullWidth size="small">
                            <InputLabel id="simulation-assignee-label">
                                Simulated assignee
                            </InputLabel>
                            <Select
                                label="Simulated assignee"
                                labelId="simulation-assignee-label"
                                onChange={(event) => {
                                    setAssigneeUserId(event.target.value)
                                    simulationMutation.reset()
                                }}
                                value={assigneeUserId}
                            >
                                <MenuItem value="">Unassigned</MenuItem>
                                {(membersQuery.data?.content ?? [])
                                    .filter((member) => member.userStatus === 'ACTIVE')
                                    .map((member) => (
                                        <MenuItem key={member.userId} value={member.userId}>
                                            {member.fullName}
                                        </MenuItem>
                                    ))}
                            </Select>
                        </FormControl>

                        {selectedTask && (
                            <Typography color="text.secondary" variant="caption">
                                Current due: {formatDateTime(selectedTask.dueAt)} · Current
                                assignee: {selectedTask.assigneeName ?? 'Unassigned'}
                            </Typography>
                        )}

                        <Divider />

                        <Typography variant="subtitle2">Dependency change</Typography>
                        <FormControl fullWidth size="small">
                            <InputLabel id="simulation-dependency-type-label">Change</InputLabel>
                            <Select
                                label="Change"
                                labelId="simulation-dependency-type-label"
                                onChange={(event) => {
                                    setDependencyType(
                                        event.target.value as ProjectSimulationDependencyChangeType,
                                    )
                                    simulationMutation.reset()
                                }}
                                value={dependencyType}
                            >
                                <MenuItem value="ADD">Add dependency</MenuItem>
                                <MenuItem value="REMOVE">Remove dependency</MenuItem>
                            </Select>
                        </FormControl>

                        <FormControl fullWidth size="small">
                            <InputLabel id="simulation-blocker-label">Blocking task</InputLabel>
                            <Select
                                label="Blocking task"
                                labelId="simulation-blocker-label"
                                onChange={(event) => {
                                    setBlockingTaskId(event.target.value)
                                    simulationMutation.reset()
                                }}
                                value={blockingTaskId}
                            >
                                <MenuItem value="">None</MenuItem>
                                {baseline.tasks.map((task) => (
                                    <MenuItem key={task.taskId} value={task.taskId}>
                                        {task.title}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        <FormControl fullWidth size="small">
                            <InputLabel id="simulation-dependent-label">Dependent task</InputLabel>
                            <Select
                                label="Dependent task"
                                labelId="simulation-dependent-label"
                                onChange={(event) => {
                                    setDependentTaskId(event.target.value)
                                    simulationMutation.reset()
                                }}
                                value={dependentTaskId}
                            >
                                <MenuItem value="">None</MenuItem>
                                {baseline.tasks.map((task) => (
                                    <MenuItem key={task.taskId} value={task.taskId}>
                                        {task.title}
                                    </MenuItem>
                                ))}
                            </Select>
                        </FormControl>

                        {blockingTaskId &&
                            dependentTaskId &&
                            blockingTaskId === dependentTaskId && (
                                <Alert severity="warning">A task cannot depend on itself.</Alert>
                            )}

                        <Stack direction="row" spacing={1}>
                            <Button
                                disabled={!canRun || simulationMutation.isPending}
                                onClick={runSimulation}
                                startIcon={
                                    simulationMutation.isPending ? (
                                        <CircularProgress color="inherit" size={16} />
                                    ) : (
                                        <ScienceRoundedIcon />
                                    )
                                }
                                variant="contained"
                            >
                                Run simulation
                            </Button>
                            <Button
                                onClick={resetScenario}
                                startIcon={<RestartAltRoundedIcon />}
                                variant="outlined"
                            >
                                Reset
                            </Button>
                        </Stack>
                    </Stack>
                </Paper>

                <Stack spacing={2}>
                    {!result && !simulationMutation.isError && (
                        <Paper
                            sx={{ padding: { xs: 3, sm: 5 }, textAlign: 'center' }}
                            variant="outlined"
                        >
                            <ScienceRoundedIcon color="disabled" sx={{ fontSize: 44 }} />
                            <Typography sx={{ marginTop: 1 }} variant="h6">
                                Build a scenario
                            </Typography>
                            <Typography color="text.secondary" variant="body2">
                                Change a task, a dependency, or both, then run the simulation to see
                                direct and downstream effects.
                            </Typography>
                        </Paper>
                    )}

                    {simulationMutation.isError && (
                        <Alert severity="error">
                            {getErrorMessage(
                                simulationMutation.error,
                                'The scenario could not be simulated.',
                            )}
                        </Alert>
                    )}

                    {result && (
                        <>
                            <Paper sx={{ padding: 2.5 }} variant="outlined">
                                <Typography variant="h6">Impact summary</Typography>
                                <Box
                                    sx={{
                                        display: 'grid',
                                        gap: 1.5,
                                        gridTemplateColumns: {
                                            xs: 'repeat(2, minmax(0, 1fr))',
                                            md: 'repeat(4, minmax(0, 1fr))',
                                        },
                                        marginTop: 2,
                                    }}
                                >
                                    {[
                                        ['Direct changes', result.summary.directTaskChanges],
                                        ['Downstream', result.summary.downstreamAffectedTasks],
                                        ['New conflicts', result.summary.newDependencyConflicts],
                                        ['Reassignments', result.summary.reassignedTasks],
                                    ].map(([label, value]) => (
                                        <Box key={label}>
                                            <Typography color="text.secondary" variant="caption">
                                                {label}
                                            </Typography>
                                            <Typography variant="h5">{value}</Typography>
                                        </Box>
                                    ))}
                                </Box>
                            </Paper>

                            <Paper sx={{ padding: 2.5 }} variant="outlined">
                                <Stack
                                    direction="row"
                                    spacing={1}
                                    sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                                >
                                    <Typography variant="h6">Task impact</Typography>
                                    <Chip
                                        label={`${result.taskImpacts.length} affected`}
                                        size="small"
                                        variant="outlined"
                                    />
                                </Stack>
                                {result.taskImpacts.length === 0 ? (
                                    <Typography
                                        color="text.secondary"
                                        sx={{ marginTop: 1 }}
                                        variant="body2"
                                    >
                                        This scenario does not change the evaluated project state.
                                    </Typography>
                                ) : (
                                    <Stack divider={<Divider flexItem />} sx={{ marginTop: 1.5 }}>
                                        {result.taskImpacts.map((impact) => (
                                            <Box key={impact.taskId} sx={{ paddingBlock: 1.25 }}>
                                                <Stack
                                                    direction={{ xs: 'column', sm: 'row' }}
                                                    spacing={1}
                                                    sx={{ justifyContent: 'space-between' }}
                                                >
                                                    <Box>
                                                        <Typography variant="subtitle2">
                                                            {impact.title}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="body2"
                                                        >
                                                            {formatDateTime(impact.currentDueAt)} →{' '}
                                                            {formatDateTime(impact.simulatedDueAt)}
                                                        </Typography>
                                                        <Typography
                                                            color="text.secondary"
                                                            variant="body2"
                                                        >
                                                            {impact.currentAssigneeName ??
                                                                'Unassigned'}{' '}
                                                            →{' '}
                                                            {impact.simulatedAssigneeName ??
                                                                'Unassigned'}
                                                        </Typography>
                                                    </Box>
                                                    <Stack
                                                        direction="row"
                                                        spacing={0.75}
                                                        sx={{
                                                            alignItems: 'flex-start',
                                                            flexWrap: 'wrap',
                                                        }}
                                                    >
                                                        {impact.directChange && (
                                                            <Chip label="Direct" size="small" />
                                                        )}
                                                        {impact.downstreamAffected && (
                                                            <Chip
                                                                label="Downstream"
                                                                size="small"
                                                                variant="outlined"
                                                            />
                                                        )}
                                                        {impact.hasSimulatedDependencyConflict && (
                                                            <Chip
                                                                color="warning"
                                                                label="Conflict"
                                                                size="small"
                                                            />
                                                        )}
                                                    </Stack>
                                                </Stack>
                                            </Box>
                                        ))}
                                    </Stack>
                                )}
                            </Paper>

                            {result.dependencyConflicts.length > 0 && (
                                <Paper sx={{ padding: 2.5 }} variant="outlined">
                                    <Typography variant="h6">Dependency conflicts</Typography>
                                    <Stack divider={<Divider flexItem />} sx={{ marginTop: 1 }}>
                                        {result.dependencyConflicts.map((conflict) => (
                                            <Box
                                                key={`${conflict.blockingTaskId}:${conflict.dependentTaskId}`}
                                                sx={{ paddingBlock: 1.25 }}
                                            >
                                                <Stack
                                                    direction="row"
                                                    spacing={1}
                                                    sx={{
                                                        alignItems: 'center',
                                                        justifyContent: 'space-between',
                                                    }}
                                                >
                                                    <Typography variant="body2">
                                                        {conflict.blockingTaskTitle} →{' '}
                                                        {conflict.dependentTaskTitle}
                                                    </Typography>
                                                    <Chip
                                                        color={
                                                            conflict.changeType === 'NEW'
                                                                ? 'warning'
                                                                : conflict.changeType === 'RESOLVED'
                                                                  ? 'success'
                                                                  : 'default'
                                                        }
                                                        label={conflict.changeType.toLowerCase()}
                                                        size="small"
                                                    />
                                                </Stack>
                                            </Box>
                                        ))}
                                    </Stack>
                                </Paper>
                            )}

                            {result.workloadImpacts.length > 0 && (
                                <Paper sx={{ padding: 2.5 }} variant="outlined">
                                    <Typography variant="h6">Workload delta</Typography>
                                    <Stack divider={<Divider flexItem />} sx={{ marginTop: 1 }}>
                                        {result.workloadImpacts.map((impact) => (
                                            <Stack
                                                direction="row"
                                                key={impact.userId ?? 'unassigned'}
                                                sx={{
                                                    justifyContent: 'space-between',
                                                    paddingBlock: 1.25,
                                                }}
                                            >
                                                <Typography variant="body2">
                                                    {impact.displayName}
                                                </Typography>
                                                <Typography variant="body2">
                                                    {impact.baselineOpenTasks} →{' '}
                                                    {impact.simulatedOpenTasks} (
                                                    {impact.delta > 0 ? '+' : ''}
                                                    {impact.delta})
                                                </Typography>
                                            </Stack>
                                        ))}
                                    </Stack>
                                </Paper>
                            )}

                            <Alert severity="success">
                                Scenario calculated. No project data was changed.
                            </Alert>
                        </>
                    )}
                </Stack>
            </Box>
        </Stack>
    )
}
