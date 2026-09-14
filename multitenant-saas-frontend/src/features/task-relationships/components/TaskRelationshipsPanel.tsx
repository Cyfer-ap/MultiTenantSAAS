import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import AddRoundedIcon from '@mui/icons-material/AddRounded'
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined'
import LinkRoundedIcon from '@mui/icons-material/LinkRounded'
import SettingsOutlinedIcon from '@mui/icons-material/SettingsOutlined'
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
    Divider,
    IconButton,
    List,
    ListItemButton,
    ListItemText,
    MenuItem,
    Paper,
    Select,
    Skeleton,
    Stack,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'

import { useProjectTasks } from '../../projects/hooks/useProjectTasks'
import type { ProjectTask, ProjectTasksQueryParams } from '../../projects/types/projectTasks'
import {
    useAddTaskDependency,
    useAssignTaskLabel,
    useProjectTaskLabels,
    useRemoveTaskDependency,
    useTaskRelationships,
    useUnassignTaskLabel,
    useUpdateTaskParent,
} from '../hooks/useTaskRelationships'
import type { TaskReference } from '../types/taskRelationships'
import { TaskLabelManagerDialog } from './TaskLabelManagerDialog'

interface TaskRelationshipsPanelProps {
    tenantId: string
    projectId: string
    task: ProjectTask
    canManage: boolean
    onOpenTask: (taskId: string) => void
    onFeedback: (message: string) => void
}

type PickerMode = 'parent' | 'blocker'

const statusLabels: Record<ProjectTask['status'], string> = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
}

function getErrorMessage(error: unknown, fallback: string): string {
    return error instanceof Error ? error.message : fallback
}

function RelatedTaskRow({
    task,
    onOpenTask,
    action,
}: {
    task: TaskReference
    onOpenTask: (taskId: string) => void
    action?: React.ReactNode
}) {
    return (
        <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', borderTop: 1, borderColor: 'divider', py: 1 }}
        >
            <Button
                color="inherit"
                onClick={() => onOpenTask(task.id)}
                sx={{ flexGrow: 1, justifyContent: 'flex-start', minWidth: 0, textAlign: 'left' }}
            >
                <Box sx={{ minWidth: 0 }}>
                    <Typography noWrap variant="body2">
                        {task.title}
                    </Typography>
                    <Typography color="text.secondary" variant="caption">
                        {statusLabels[task.status]} · {task.priority.toLowerCase()}
                    </Typography>
                </Box>
            </Button>
            {action}
        </Stack>
    )
}

export function TaskRelationshipsPanel({
    tenantId,
    projectId,
    task,
    canManage,
    onOpenTask,
    onFeedback,
}: TaskRelationshipsPanelProps) {
    const relationshipsQuery = useTaskRelationships(tenantId, projectId, task.id)
    const labelsQuery = useProjectTaskLabels(tenantId, projectId)
    const updateParentMutation = useUpdateTaskParent(tenantId, projectId, task.id)
    const addDependencyMutation = useAddTaskDependency(tenantId, projectId, task.id)
    const removeDependencyMutation = useRemoveTaskDependency(tenantId, projectId, task.id)
    const assignLabelMutation = useAssignTaskLabel(tenantId, projectId, task.id)
    const unassignLabelMutation = useUnassignTaskLabel(tenantId, projectId, task.id)

    const [pickerMode, setPickerMode] = useState<PickerMode | null>(null)
    const [pickerSearch, setPickerSearch] = useState('')
    const [selectedTaskId, setSelectedTaskId] = useState('')
    const [labelManagerOpen, setLabelManagerOpen] = useState(false)
    const [selectedLabelId, setSelectedLabelId] = useState('')
    const [mutationError, setMutationError] = useState<string | null>(null)

    const pickerParams: ProjectTasksQueryParams = {
        page: 0,
        size: 50,
        sortBy: 'title',
        sortDir: 'asc',
        ...(pickerSearch.trim() ? { search: pickerSearch.trim() } : {}),
    }
    const candidateQuery = useProjectTasks(
        tenantId,
        projectId,
        pickerParams,
        Boolean(pickerMode),
    )

    const candidateTasks = useMemo(
        () =>
            (candidateQuery.data?.content ?? []).filter(
                (candidate) => candidate.id !== task.id && candidate.status !== 'CANCELLED',
            ),
        [candidateQuery.data?.content, task.id],
    )
    const assignedLabelIds = useMemo(
        () => new Set((relationshipsQuery.data?.labels ?? []).map((label) => label.id)),
        [relationshipsQuery.data?.labels],
    )
    const availableLabels = (labelsQuery.data ?? []).filter((label) => !assignedLabelIds.has(label.id))

    const closePicker = () => {
        setPickerMode(null)
        setPickerSearch('')
        setSelectedTaskId('')
        setMutationError(null)
    }

    const submitPicker = async () => {
        if (!pickerMode || !selectedTaskId) return
        setMutationError(null)
        try {
            if (pickerMode === 'parent') {
                await updateParentMutation.mutateAsync(selectedTaskId)
                onFeedback('Parent task updated.')
            } else {
                await addDependencyMutation.mutateAsync(selectedTaskId)
                onFeedback('Task blocker added.')
            }
            closePicker()
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The task relationship could not be updated.'))
        }
    }

    const removeParent = async () => {
        setMutationError(null)
        try {
            await updateParentMutation.mutateAsync(null)
            onFeedback('Parent task removed.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The parent task could not be removed.'))
        }
    }

    const removeBlocker = async (blockingTaskId: string) => {
        setMutationError(null)
        try {
            await removeDependencyMutation.mutateAsync(blockingTaskId)
            onFeedback('Task blocker removed.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The blocker could not be removed.'))
        }
    }

    const assignLabel = async (labelId: string) => {
        if (!labelId) return
        setMutationError(null)
        try {
            await assignLabelMutation.mutateAsync(labelId)
            setSelectedLabelId('')
            onFeedback('Label added to task.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The label could not be added.'))
        }
    }

    const unassignLabel = async (labelId: string) => {
        setMutationError(null)
        try {
            await unassignLabelMutation.mutateAsync(labelId)
            onFeedback('Label removed from task.')
        } catch (error) {
            setMutationError(getErrorMessage(error, 'The label could not be removed.'))
        }
    }

    if (relationshipsQuery.isPending) {
        return (
            <Stack aria-label="Loading task planning details" role="status" spacing={1.5}>
                <Skeleton height={120} variant="rounded" />
                <Skeleton height={160} variant="rounded" />
                <Skeleton height={110} variant="rounded" />
            </Stack>
        )
    }

    if (relationshipsQuery.isError || !relationshipsQuery.data) {
        return (
            <Alert severity="error">
                {getErrorMessage(
                    relationshipsQuery.error,
                    'Task planning relationships could not be loaded.',
                )}
            </Alert>
        )
    }

    const relationships = relationshipsQuery.data
    const mutationPending =
        updateParentMutation.isPending ||
        addDependencyMutation.isPending ||
        removeDependencyMutation.isPending ||
        assignLabelMutation.isPending ||
        unassignLabelMutation.isPending

    return (
        <Stack spacing={2}>
            {mutationError && (
                <Alert onClose={() => setMutationError(null)} severity="error">
                    {mutationError}
                </Alert>
            )}

            <Paper sx={{ padding: 2 }} variant="outlined">
                <Stack spacing={1.25}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <AccountTreeOutlinedIcon color="primary" />
                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="subtitle1">Hierarchy</Typography>
                            <Typography color="text.secondary" variant="caption">
                                One parent task with bounded direct subtasks.
                            </Typography>
                        </Box>
                        {canManage && (
                            <Button
                                onClick={() => setPickerMode('parent')}
                                size="small"
                                startIcon={<AddRoundedIcon />}
                            >
                                {relationships.parent ? 'Change parent' : 'Set parent'}
                            </Button>
                        )}
                    </Stack>

                    <Box>
                        <Typography color="text.secondary" variant="overline">
                            Parent
                        </Typography>
                        {relationships.parent ? (
                            <RelatedTaskRow
                                action={
                                    canManage ? (
                                        <Button
                                            disabled={updateParentMutation.isPending}
                                            onClick={() => void removeParent()}
                                            size="small"
                                        >
                                            Remove
                                        </Button>
                                    ) : undefined
                                }
                                onOpenTask={onOpenTask}
                                task={relationships.parent}
                            />
                        ) : (
                            <Typography color="text.secondary" variant="body2">
                                No parent task.
                            </Typography>
                        )}
                    </Box>

                    <Box>
                        <Typography color="text.secondary" variant="overline">
                            Subtasks ({relationships.children.length})
                        </Typography>
                        {relationships.children.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                No direct subtasks.
                            </Typography>
                        ) : (
                            relationships.children.map((child) => (
                                <RelatedTaskRow key={child.id} onOpenTask={onOpenTask} task={child} />
                            ))
                        )}
                        {relationships.childrenTruncated && (
                            <Alert severity="info" sx={{ marginTop: 1 }}>
                                Only the first 200 subtasks are shown.
                            </Alert>
                        )}
                    </Box>
                </Stack>
            </Paper>

            <Paper sx={{ padding: 2 }} variant="outlined">
                <Stack spacing={1.25}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <LinkRoundedIcon color="primary" />
                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="subtitle1">Dependencies</Typography>
                            <Typography color="text.secondary" variant="caption">
                                Blockers are tasks that must be resolved before this task.
                            </Typography>
                        </Box>
                        {canManage && (
                            <Button
                                onClick={() => setPickerMode('blocker')}
                                size="small"
                                startIcon={<AddRoundedIcon />}
                            >
                                Add blocker
                            </Button>
                        )}
                    </Stack>

                    <Box>
                        <Typography color="text.secondary" variant="overline">
                            Blocked by ({relationships.blockers.length})
                        </Typography>
                        {relationships.blockers.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                No blockers.
                            </Typography>
                        ) : (
                            relationships.blockers.map((blocker) => (
                                <RelatedTaskRow
                                    action={
                                        canManage ? (
                                            <Button
                                                disabled={removeDependencyMutation.isPending}
                                                onClick={() => void removeBlocker(blocker.id)}
                                                size="small"
                                            >
                                                Remove
                                            </Button>
                                        ) : undefined
                                    }
                                    key={blocker.id}
                                    onOpenTask={onOpenTask}
                                    task={blocker}
                                />
                            ))
                        )}
                        {relationships.blockersTruncated && (
                            <Alert severity="info" sx={{ marginTop: 1 }}>
                                Only the first 200 blockers are shown.
                            </Alert>
                        )}
                    </Box>

                    <Divider />
                    <Box>
                        <Typography color="text.secondary" variant="overline">
                            Blocking ({relationships.dependents.length})
                        </Typography>
                        {relationships.dependents.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                This task is not blocking another task.
                            </Typography>
                        ) : (
                            relationships.dependents.map((dependent) => (
                                <RelatedTaskRow
                                    key={dependent.id}
                                    onOpenTask={onOpenTask}
                                    task={dependent}
                                />
                            ))
                        )}
                        {relationships.dependentsTruncated && (
                            <Alert severity="info" sx={{ marginTop: 1 }}>
                                Only the first 200 dependent tasks are shown.
                            </Alert>
                        )}
                    </Box>
                </Stack>
            </Paper>

            <Paper sx={{ padding: 2 }} variant="outlined">
                <Stack spacing={1.25}>
                    <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                        <LabelOutlinedIcon color="primary" />
                        <Box sx={{ flexGrow: 1 }}>
                            <Typography variant="subtitle1">Labels</Typography>
                            <Typography color="text.secondary" variant="caption">
                                Project-scoped metadata for grouping and future filtering.
                            </Typography>
                        </Box>
                        {canManage && (
                            <Tooltip title="Manage project labels">
                                <IconButton
                                    aria-label="Manage project labels"
                                    onClick={() => setLabelManagerOpen(true)}
                                    size="small"
                                >
                                    <SettingsOutlinedIcon fontSize="small" />
                                </IconButton>
                            </Tooltip>
                        )}
                    </Stack>

                    <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 0.75 }}>
                        {relationships.labels.length === 0 && (
                            <Typography color="text.secondary" variant="body2">
                                No labels assigned.
                            </Typography>
                        )}
                        {relationships.labels.map((label) => (
                            <Chip
                                key={label.id}
                                label={label.name}
                                onDelete={canManage ? () => void unassignLabel(label.id) : undefined}
                                size="small"
                                sx={{
                                    backgroundColor: label.color ? `${label.color}18` : undefined,
                                    borderColor: label.color ?? undefined,
                                }}
                                variant="outlined"
                            />
                        ))}
                    </Stack>

                    {canManage && (
                        <Select
                            displayEmpty
                            disabled={
                                labelsQuery.isPending ||
                                mutationPending ||
                                availableLabels.length === 0 ||
                                relationships.labels.length >= 20
                            }
                            onChange={(event) => void assignLabel(event.target.value)}
                            size="small"
                            value={selectedLabelId}
                        >
                            <MenuItem disabled value="">
                                {relationships.labels.length >= 20
                                    ? 'Task label limit reached'
                                    : availableLabels.length === 0
                                      ? 'No unassigned labels'
                                      : 'Add a label…'}
                            </MenuItem>
                            {availableLabels.map((label) => (
                                <MenuItem key={label.id} value={label.id}>
                                    {label.name}
                                </MenuItem>
                            ))}
                        </Select>
                    )}
                    {labelsQuery.isError && (
                        <Alert severity="warning">
                            Project labels could not be loaded. Existing task labels remain visible.
                        </Alert>
                    )}
                </Stack>
            </Paper>

            <Dialog fullWidth maxWidth="sm" onClose={closePicker} open={Boolean(pickerMode)}>
                <DialogTitle>
                    {pickerMode === 'parent' ? 'Choose parent task' : 'Add blocking task'}
                </DialogTitle>
                <DialogContent>
                    <Stack spacing={1.5} sx={{ paddingTop: 1 }}>
                        <TextField
                            autoFocus
                            label="Search project tasks"
                            onChange={(event) => {
                                setPickerSearch(event.target.value)
                                setSelectedTaskId('')
                            }}
                            value={pickerSearch}
                        />
                        {candidateQuery.isPending && (
                            <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                                <CircularProgress size={28} />
                            </Box>
                        )}
                        {candidateQuery.isError && (
                            <Alert severity="error">Project tasks could not be searched.</Alert>
                        )}
                        {candidateQuery.isSuccess && candidateTasks.length === 0 && (
                            <Typography color="text.secondary" variant="body2">
                                No eligible tasks match this search.
                            </Typography>
                        )}
                        {candidateTasks.length > 0 && (
                            <List disablePadding>
                                {candidateTasks.map((candidate) => (
                                    <ListItemButton
                                        key={candidate.id}
                                        onClick={() => setSelectedTaskId(candidate.id)}
                                        selected={selectedTaskId === candidate.id}
                                    >
                                        <ListItemText
                                            primary={candidate.title}
                                            secondary={`${statusLabels[candidate.status]} · ${candidate.priority.toLowerCase()}`}
                                        />
                                    </ListItemButton>
                                ))}
                            </List>
                        )}
                        {(candidateQuery.data?.totalElements ?? 0) > 50 && (
                            <Alert severity="info">
                                Showing the first 50 matches. Refine the search to find another task.
                            </Alert>
                        )}
                        {mutationError && <Alert severity="error">{mutationError}</Alert>}
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={closePicker}>Cancel</Button>
                    <Button
                        disabled={!selectedTaskId || mutationPending}
                        onClick={() => void submitPicker()}
                        variant="contained"
                    >
                        {pickerMode === 'parent' ? 'Set parent' : 'Add blocker'}
                    </Button>
                </DialogActions>
            </Dialog>

            <TaskLabelManagerDialog
                labels={labelsQuery.data ?? []}
                onClose={() => setLabelManagerOpen(false)}
                onFeedback={onFeedback}
                open={labelManagerOpen}
                projectId={projectId}
                tenantId={tenantId}
            />
        </Stack>
    )
}
