import {
    Alert,
    Box,
    Button,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    FormControl,
    InputLabel,
    MenuItem,
    Select,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import type { FormEvent } from 'react'
import { useState } from 'react'

import {
    useCancelProjectTask,
    useCreateProjectTask,
    useUpdateProjectTask,
    useUpdateProjectTaskAssignee,
    useUpdateProjectTaskStatus,
} from '../hooks/useProjectTasks'
import type {
    ProjectTask,
    ProjectTaskPriority,
    ProjectTaskStatus,
} from '../types/projectTasks'
import type { ProjectMember } from '../types/projects'

interface DialogBaseProps {
    tenantId: string
    projectId: string
    onClose: () => void
    onSuccess: (message: string) => void
}

interface TaskDialogProps extends DialogBaseProps {
    task: ProjectTask
}

interface AssigneeOption {
    userId: string
    fullName: string
    email: string
}

const priorityLabels: Record<ProjectTaskPriority, string> = {
    LOW: 'Low',
    MEDIUM: 'Medium',
    HIGH: 'High',
    URGENT: 'Urgent',
}

const statusLabels: Record<
    Exclude<ProjectTaskStatus, 'CANCELLED'>,
    string
> = {
    TODO: 'To do',
    IN_PROGRESS: 'In progress',
    BLOCKED: 'Blocked',
    COMPLETED: 'Completed',
}

function getErrorMessage(
    error: unknown,
    fallback: string,
): string {
    return error instanceof Error ? error.message : fallback
}

function toLocalDateTime(value: string | null): string {
    if (!value) {
        return ''
    }

    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
        return ''
    }

    const offset = date.getTimezoneOffset() * 60_000
    return new Date(date.getTime() - offset)
        .toISOString()
        .slice(0, 16)
}

function toApiDateTime(value: string): string | null {
    return value ? new Date(value).toISOString() : null
}

function activeAssignees(
    members: ProjectMember[],
): AssigneeOption[] {
    return members
        .filter((member) => member.userStatus === 'ACTIVE')
        .map(({ userId, fullName, email }) => ({
            userId,
            fullName,
            email,
        }))
}

interface TaskFieldsProps {
    title: string
    description: string
    priority: ProjectTaskPriority
    dueAt: string
    onTitleChange: (value: string) => void
    onDescriptionChange: (value: string) => void
    onPriorityChange: (value: ProjectTaskPriority) => void
    onDueAtChange: (value: string) => void
}

function TaskFields({
    title,
    description,
    priority,
    dueAt,
    onTitleChange,
    onDescriptionChange,
    onPriorityChange,
    onDueAtChange,
}: TaskFieldsProps) {
    return (
        <Stack spacing={2} sx={{ paddingTop: 1 }}>
            <TextField
                autoFocus
                fullWidth
                label="Task title"
                onChange={(event) => {
                    onTitleChange(event.target.value)
                }}
                required
                slotProps={{
                    htmlInput: { maxLength: 200, minLength: 2 },
                }}
                value={title}
            />
            <TextField
                fullWidth
                label="Description"
                minRows={3}
                multiline
                onChange={(event) => {
                    onDescriptionChange(event.target.value)
                }}
                slotProps={{ htmlInput: { maxLength: 4000 } }}
                value={description}
            />
            <FormControl fullWidth required>
                <InputLabel id="task-priority-label">
                    Priority
                </InputLabel>
                <Select
                    label="Priority"
                    labelId="task-priority-label"
                    onChange={(event) => {
                        onPriorityChange(
                            event.target.value as ProjectTaskPriority,
                        )
                    }}
                    value={priority}
                >
                    {Object.entries(priorityLabels).map(
                        ([value, label]) => (
                            <MenuItem key={value} value={value}>
                                {label}
                            </MenuItem>
                        ),
                    )}
                </Select>
            </FormControl>
            <TextField
                fullWidth
                label="Due date"
                onChange={(event) => {
                    onDueAtChange(event.target.value)
                }}
                slotProps={{ inputLabel: { shrink: true } }}
                type="datetime-local"
                value={dueAt}
            />
        </Stack>
    )
}

interface CreateTaskDialogProps extends DialogBaseProps {
    members: ProjectMember[]
}

export function CreateProjectTaskDialog({
    tenantId,
    projectId,
    members,
    onClose,
    onSuccess,
}: CreateTaskDialogProps) {
    const [title, setTitle] = useState('')
    const [description, setDescription] = useState('')
    const [priority, setPriority] =
        useState<ProjectTaskPriority>('MEDIUM')
    const [dueAt, setDueAt] = useState('')
    const [assigneeUserId, setAssigneeUserId] = useState('')
    const mutation = useCreateProjectTask(tenantId, projectId)
    const assignees = activeAssignees(members)

    const submit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()

        mutation.mutate(
            {
                title: title.trim(),
                description: description.trim() || null,
                priority,
                dueAt: toApiDateTime(dueAt),
                assigneeUserId: assigneeUserId || null,
            },
            {
                onSuccess: () => {
                    onSuccess('Task created successfully.')
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <Box component="form" onSubmit={submit}>
                <DialogTitle>Create task</DialogTitle>
                <DialogContent>
                {mutation.isError && (
                    <Alert severity="error" sx={{ marginBottom: 2 }}>
                        {getErrorMessage(
                            mutation.error,
                            'The task could not be created.',
                        )}
                    </Alert>
                )}
                <TaskFields
                    description={description}
                    dueAt={dueAt}
                    onDescriptionChange={setDescription}
                    onDueAtChange={setDueAt}
                    onPriorityChange={setPriority}
                    onTitleChange={setTitle}
                    priority={priority}
                    title={title}
                />
                <FormControl fullWidth sx={{ marginTop: 2 }}>
                    <InputLabel id="create-task-assignee-label">
                        Assignee
                    </InputLabel>
                    <Select
                        label="Assignee"
                        labelId="create-task-assignee-label"
                        onChange={(event) => {
                            setAssigneeUserId(event.target.value)
                        }}
                        value={assigneeUserId}
                    >
                        <MenuItem value="">Unassigned</MenuItem>
                        {assignees.map((member) => (
                            <MenuItem
                                key={member.userId}
                                value={member.userId}
                            >
                                {member.fullName} ({member.email})
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancel</Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        Create task
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function EditProjectTaskDialog({
    tenantId,
    projectId,
    task,
    onClose,
    onSuccess,
}: TaskDialogProps) {
    const [title, setTitle] = useState(task.title)
    const [description, setDescription] =
        useState(task.description ?? '')
    const [priority, setPriority] =
        useState<ProjectTaskPriority>(task.priority)
    const [dueAt, setDueAt] =
        useState(toLocalDateTime(task.dueAt))
    const mutation = useUpdateProjectTask(tenantId, projectId)

    const submit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()

        mutation.mutate(
            {
                taskId: task.id,
                input: {
                    title: title.trim(),
                    description: description.trim() || null,
                    priority,
                    dueAt: toApiDateTime(dueAt),
                },
            },
            {
                onSuccess: () => {
                    onSuccess('Task updated successfully.')
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <Box component="form" onSubmit={submit}>
                <DialogTitle>Edit task</DialogTitle>
                <DialogContent>
                {mutation.isError && (
                    <Alert severity="error" sx={{ marginBottom: 2 }}>
                        {getErrorMessage(
                            mutation.error,
                            'The task could not be updated.',
                        )}
                    </Alert>
                )}
                <TaskFields
                    description={description}
                    dueAt={dueAt}
                    onDescriptionChange={setDescription}
                    onDueAtChange={setDueAt}
                    onPriorityChange={setPriority}
                    onTitleChange={setTitle}
                    priority={priority}
                    title={title}
                />
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancel</Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        Save changes
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function ChangeProjectTaskStatusDialog({
    tenantId,
    projectId,
    task,
    onClose,
    onSuccess,
}: TaskDialogProps) {
    const initialStatus = task.status === 'CANCELLED'
        ? 'TODO'
        : task.status
    const [status, setStatus] = useState<
        Exclude<ProjectTaskStatus, 'CANCELLED'>
    >(initialStatus)
    const mutation = useUpdateProjectTaskStatus(
        tenantId,
        projectId,
    )

    const submit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        mutation.mutate(
            { taskId: task.id, input: { status } },
            {
                onSuccess: () => {
                    onSuccess('Task status updated successfully.')
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="xs"
            onClose={onClose}
            open
        >
            <Box component="form" onSubmit={submit}>
                <DialogTitle>Change task status</DialogTitle>
                <DialogContent>
                {mutation.isError && (
                    <Alert severity="error" sx={{ marginBottom: 2 }}>
                        {getErrorMessage(
                            mutation.error,
                            'The task status could not be updated.',
                        )}
                    </Alert>
                )}
                <FormControl fullWidth sx={{ marginTop: 1 }}>
                    <InputLabel id="task-status-label">
                        Status
                    </InputLabel>
                    <Select
                        label="Status"
                        labelId="task-status-label"
                        onChange={(event) => {
                            setStatus(
                                event.target.value as Exclude<
                                    ProjectTaskStatus,
                                    'CANCELLED'
                                >,
                            )
                        }}
                        value={status}
                    >
                        {Object.entries(statusLabels).map(
                            ([value, label]) => (
                                <MenuItem key={value} value={value}>
                                    {label}
                                </MenuItem>
                            ),
                        )}
                    </Select>
                </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancel</Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        Change status
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

interface AssignProjectTaskDialogProps extends TaskDialogProps {
    members: ProjectMember[]
}

export function AssignProjectTaskDialog({
    tenantId,
    projectId,
    task,
    members,
    onClose,
    onSuccess,
}: AssignProjectTaskDialogProps) {
    const [assigneeUserId, setAssigneeUserId] =
        useState(task.assigneeUserId ?? '')
    const mutation = useUpdateProjectTaskAssignee(
        tenantId,
        projectId,
    )
    const assignees = activeAssignees(members)

    const submit = (event: FormEvent<HTMLFormElement>): void => {
        event.preventDefault()
        mutation.mutate(
            {
                taskId: task.id,
                input: {
                    assigneeUserId: assigneeUserId || null,
                },
            },
            {
                onSuccess: () => {
                    onSuccess('Task assignee updated successfully.')
                    onClose()
                },
            },
        )
    }

    return (
        <Dialog
            fullWidth
            maxWidth="sm"
            onClose={onClose}
            open
        >
            <Box component="form" onSubmit={submit}>
                <DialogTitle>Assign task</DialogTitle>
                <DialogContent>
                {mutation.isError && (
                    <Alert severity="error" sx={{ marginBottom: 2 }}>
                        {getErrorMessage(
                            mutation.error,
                            'The task assignee could not be updated.',
                        )}
                    </Alert>
                )}
                <FormControl fullWidth sx={{ marginTop: 1 }}>
                    <InputLabel id="update-task-assignee-label">
                        Assignee
                    </InputLabel>
                    <Select
                        label="Assignee"
                        labelId="update-task-assignee-label"
                        onChange={(event) => {
                            setAssigneeUserId(event.target.value)
                        }}
                        value={assigneeUserId}
                    >
                        <MenuItem value="">Unassigned</MenuItem>
                        {assignees.map((member) => (
                            <MenuItem
                                key={member.userId}
                                value={member.userId}
                            >
                                {member.fullName} ({member.email})
                            </MenuItem>
                        ))}
                    </Select>
                </FormControl>
                </DialogContent>
                <DialogActions>
                    <Button onClick={onClose}>Cancel</Button>
                    <Button
                        disabled={mutation.isPending}
                        type="submit"
                        variant="contained"
                    >
                        Update assignee
                    </Button>
                </DialogActions>
            </Box>
        </Dialog>
    )
}

export function CancelProjectTaskDialog({
    tenantId,
    projectId,
    task,
    onClose,
    onSuccess,
}: TaskDialogProps) {
    const mutation = useCancelProjectTask(tenantId, projectId)

    return (
        <Dialog onClose={onClose} open>
            <DialogTitle>Cancel task</DialogTitle>
            <DialogContent>
                {mutation.isError && (
                    <Alert severity="error" sx={{ marginBottom: 2 }}>
                        {getErrorMessage(
                            mutation.error,
                            'The task could not be cancelled.',
                        )}
                    </Alert>
                )}
                <Typography>
                    Cancel “{task.title}”? Cancelled tasks remain visible and cannot be modified.
                </Typography>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Keep task</Button>
                <Button
                    color="error"
                    disabled={mutation.isPending}
                    onClick={() => {
                        mutation.mutate(task.id, {
                            onSuccess: () => {
                                onSuccess('Task cancelled successfully.')
                                onClose()
                            },
                        })
                    }}
                    variant="contained"
                >
                    Cancel task
                </Button>
            </DialogActions>
        </Dialog>
    )
}
