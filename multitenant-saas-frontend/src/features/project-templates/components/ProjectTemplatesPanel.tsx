import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
    Alert,
    Button,
    Chip,
    IconButton,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'

import type { ProjectStatus } from '../../projects/types/projects'
import type { ProjectTaskPriority } from '../../projects/types/projectTasks'
import { projectTemplatesApi } from '../api/projectTemplatesApi'
import type {
    ProjectTemplate,
    ProjectTemplateInput,
    ProjectTemplateTaskInput,
} from '../types/projectTemplates'

interface ProjectTemplatesPanelProps {
    tenantId: string
    canRead: boolean
    canManage: boolean
}

interface TaskRowState {
    key: string
    title: string
    description: string
    priority: ProjectTaskPriority
    dueOffsetMinutes: string
}

interface TemplateFormState {
    name: string
    projectNameSeed: string
    projectDescription: string
    initialStatus: Exclude<ProjectStatus, 'ARCHIVED'>
    tasks: TaskRowState[]
}

function emptyTask(): TaskRowState {
    return {
        key: crypto.randomUUID(),
        title: '',
        description: '',
        priority: 'MEDIUM',
        dueOffsetMinutes: '',
    }
}

function defaultForm(): TemplateFormState {
    return {
        name: '',
        projectNameSeed: '',
        projectDescription: '',
        initialStatus: 'PLANNING',
        tasks: [],
    }
}

function taskToInput(task: TaskRowState): ProjectTemplateTaskInput {
    return {
        title: task.title.trim(),
        description: task.description.trim() || null,
        priority: task.priority,
        dueOffsetMinutes: task.dueOffsetMinutes ? Number(task.dueOffsetMinutes) : null,
    }
}

function toInput(form: TemplateFormState): ProjectTemplateInput {
    return {
        name: form.name.trim(),
        projectNameSeed: form.projectNameSeed.trim(),
        projectDescription: form.projectDescription.trim() || null,
        initialStatus: form.initialStatus,
        tasks: form.tasks.map(taskToInput),
    }
}

function formFromTemplate(template: ProjectTemplate): TemplateFormState {
    return {
        name: template.name,
        projectNameSeed: template.projectNameSeed,
        projectDescription: template.projectDescription ?? '',
        initialStatus:
            template.initialStatus === 'ARCHIVED' ? 'PLANNING' : template.initialStatus,
        tasks: template.tasks.map((task) => ({
            key: task.id,
            title: task.title,
            description: task.description ?? '',
            priority: task.priority,
            dueOffsetMinutes:
                task.dueOffsetMinutes === null ? '' : String(task.dueOffsetMinutes),
        })),
    }
}

export function ProjectTemplatesPanel({
    tenantId,
    canRead,
    canManage,
}: ProjectTemplatesPanelProps) {
    const queryClient = useQueryClient()
    const [form, setForm] = useState<TemplateFormState>(defaultForm)
    const [editingId, setEditingId] = useState<string | null>(null)
    const [nameOverrides, setNameOverrides] = useState<Record<string, string>>({})
    const [lastInstantiation, setLastInstantiation] = useState<string | null>(null)
    const templatesKey = useMemo(() => ['project-templates', tenantId] as const, [tenantId])

    const templatesQuery = useQuery({
        queryKey: templatesKey,
        queryFn: () => projectTemplatesApi.list(tenantId),
        enabled: canRead,
    })
    const invalidate = async () => queryClient.invalidateQueries({ queryKey: templatesKey })
    const saveMutation = useMutation({
        mutationFn: (input: ProjectTemplateInput) =>
            editingId
                ? projectTemplatesApi.update(tenantId, editingId, input)
                : projectTemplatesApi.create(tenantId, input),
        onSuccess: async () => {
            setEditingId(null)
            setForm(defaultForm())
            await invalidate()
        },
    })
    const deleteMutation = useMutation({
        mutationFn: (templateId: string) => projectTemplatesApi.remove(tenantId, templateId),
        onSuccess: invalidate,
    })
    const instantiateMutation = useMutation({
        mutationFn: (templateId: string) =>
            projectTemplatesApi.instantiate(
                tenantId,
                templateId,
                nameOverrides[templateId] ?? null,
            ),
        onSuccess: (result) =>
            setLastInstantiation(
                `Created project ${result.projectId} with ${result.tasksCreated} starter tasks.`,
            ),
    })

    if (!canRead) {
        return (
            <Alert severity="info">
                Project templates are tenant-wide. Tenant-level project.read permission is required to
                view this section.
            </Alert>
        )
    }

    const updateTask = (key: string, patch: Partial<TaskRowState>) => {
        setForm({
            ...form,
            tasks: form.tasks.map((task) => (task.key === key ? { ...task, ...patch } : task)),
        })
    }

    const submit = () => {
        if (!form.name.trim() || !form.projectNameSeed.trim()) return
        if (form.tasks.some((task) => !task.title.trim())) return
        saveMutation.mutate(toInput(form))
    }

    return (
        <Stack spacing={2}>
            <Stack spacing={0.5}>
                <Typography variant="h6">Project templates</Typography>
                <Typography color="text.secondary" variant="body2">
                    Capture a project shell plus up to 50 ordered starter-task snapshots. Instantiation
                    still passes through the normal project and task creation boundaries.
                </Typography>
            </Stack>

            {canManage ? (
                <Paper variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={2}>
                        <Typography fontWeight={600}>
                            {editingId ? 'Edit project template' : 'New project template'}
                        </Typography>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                            <TextField
                                fullWidth
                                label="Template name"
                                value={form.name}
                                onChange={(event) => setForm({ ...form, name: event.target.value })}
                                inputProps={{ maxLength: 80 }}
                                required
                            />
                            <TextField
                                fullWidth
                                label="Default project name"
                                value={form.projectNameSeed}
                                onChange={(event) =>
                                    setForm({ ...form, projectNameSeed: event.target.value })
                                }
                                inputProps={{ maxLength: 150 }}
                                required
                            />
                        </Stack>
                        <TextField
                            label="Project description"
                            value={form.projectDescription}
                            onChange={(event) =>
                                setForm({ ...form, projectDescription: event.target.value })
                            }
                            multiline
                            minRows={2}
                            inputProps={{ maxLength: 2000 }}
                        />
                        <TextField
                            select
                            label="Initial project status"
                            value={form.initialStatus}
                            onChange={(event) =>
                                setForm({
                                    ...form,
                                    initialStatus: event.target.value as Exclude<
                                        ProjectStatus,
                                        'ARCHIVED'
                                    >,
                                })
                            }
                        >
                            {['PLANNING', 'ACTIVE', 'ON_HOLD', 'COMPLETED'].map((status) => (
                                <MenuItem key={status} value={status}>
                                    {status}
                                </MenuItem>
                            ))}
                        </TextField>

                        <Stack direction="row" sx={{ alignItems: 'center', justifyContent: 'space-between' }}>
                            <Typography fontWeight={600}>Starter tasks ({form.tasks.length}/50)</Typography>
                            <Button
                                startIcon={<AddRoundedIcon />}
                                onClick={() =>
                                    setForm({ ...form, tasks: [...form.tasks, emptyTask()] })
                                }
                                disabled={form.tasks.length >= 50}
                            >
                                Add task
                            </Button>
                        </Stack>
                        {form.tasks.map((task, index) => (
                            <Paper key={task.key} variant="outlined" sx={{ p: 1.5 }}>
                                <Stack spacing={1.5}>
                                    <Stack direction="row" sx={{ alignItems: 'center' }} spacing={1}>
                                        <Typography fontWeight={600} sx={{ flexGrow: 1 }}>
                                            Task {index + 1}
                                        </Typography>
                                        <IconButton
                                            aria-label={`Remove task ${index + 1}`}
                                            onClick={() =>
                                                setForm({
                                                    ...form,
                                                    tasks: form.tasks.filter(
                                                        (candidate) => candidate.key !== task.key,
                                                    ),
                                                })
                                            }
                                        >
                                            <DeleteOutlineRoundedIcon />
                                        </IconButton>
                                    </Stack>
                                    <TextField
                                        label="Title"
                                        value={task.title}
                                        onChange={(event) =>
                                            updateTask(task.key, { title: event.target.value })
                                        }
                                        inputProps={{ maxLength: 200 }}
                                        required
                                    />
                                    <TextField
                                        label="Description"
                                        value={task.description}
                                        onChange={(event) =>
                                            updateTask(task.key, { description: event.target.value })
                                        }
                                        multiline
                                        minRows={2}
                                        inputProps={{ maxLength: 4000 }}
                                    />
                                    <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                                        <TextField
                                            select
                                            fullWidth
                                            label="Priority"
                                            value={task.priority}
                                            onChange={(event) =>
                                                updateTask(task.key, {
                                                    priority: event.target.value as ProjectTaskPriority,
                                                })
                                            }
                                        >
                                            {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((priority) => (
                                                <MenuItem key={priority} value={priority}>
                                                    {priority}
                                                </MenuItem>
                                            ))}
                                        </TextField>
                                        <TextField
                                            fullWidth
                                            label="Due offset (minutes)"
                                            type="number"
                                            value={task.dueOffsetMinutes}
                                            onChange={(event) =>
                                                updateTask(task.key, {
                                                    dueOffsetMinutes: event.target.value,
                                                })
                                            }
                                            inputProps={{ min: 0, max: 525600 }}
                                        />
                                    </Stack>
                                </Stack>
                            </Paper>
                        ))}
                        {saveMutation.isError ? (
                            <Alert severity="error">Unable to save project template.</Alert>
                        ) : null}
                        <Stack direction="row" spacing={1}>
                            <Button
                                variant="contained"
                                onClick={submit}
                                disabled={saveMutation.isPending}
                            >
                                {editingId ? 'Save changes' : 'Create template'}
                            </Button>
                            {editingId ? (
                                <Button
                                    onClick={() => {
                                        setEditingId(null)
                                        setForm(defaultForm())
                                    }}
                                >
                                    Cancel
                                </Button>
                            ) : null}
                        </Stack>
                    </Stack>
                </Paper>
            ) : (
                <Alert severity="info">
                    You can view project templates but project.create permission is required to modify
                    or instantiate them.
                </Alert>
            )}

            {lastInstantiation ? <Alert severity="success">{lastInstantiation}</Alert> : null}
            {templatesQuery.isError ? (
                <Alert severity="error">Unable to load project templates.</Alert>
            ) : null}
            {templatesQuery.data?.content.length === 0 ? (
                <Alert severity="info">No tenant project templates exist yet.</Alert>
            ) : null}

            {templatesQuery.data?.content.map((template) => (
                <Paper key={template.id} variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={1.5}>
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={1}
                            sx={{ justifyContent: 'space-between' }}
                        >
                            <Stack>
                                <Typography fontWeight={600}>{template.name}</Typography>
                                <Typography variant="body2">{template.projectNameSeed}</Typography>
                            </Stack>
                            <Stack direction="row" spacing={1}>
                                <Chip label={template.initialStatus} size="small" />
                                <Chip
                                    label={`${template.tasks.length} starter tasks`}
                                    size="small"
                                    variant="outlined"
                                />
                            </Stack>
                        </Stack>
                        {template.projectDescription ? (
                            <Typography color="text.secondary" variant="body2">
                                {template.projectDescription}
                            </Typography>
                        ) : null}
                        {template.tasks.length > 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                {template.tasks.map((task) => task.title).join(' · ')}
                            </Typography>
                        ) : null}
                        {canManage ? (
                            <Stack spacing={1.25}>
                                <TextField
                                    label="Project name override (optional)"
                                    value={nameOverrides[template.id] ?? ''}
                                    onChange={(event) =>
                                        setNameOverrides({
                                            ...nameOverrides,
                                            [template.id]: event.target.value,
                                        })
                                    }
                                    inputProps={{ maxLength: 150 }}
                                />
                                <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
                                    <Button
                                        size="small"
                                        variant="contained"
                                        onClick={() => instantiateMutation.mutate(template.id)}
                                        disabled={instantiateMutation.isPending}
                                    >
                                        Create project
                                    </Button>
                                    <Button
                                        size="small"
                                        onClick={() => {
                                            setEditingId(template.id)
                                            setForm(formFromTemplate(template))
                                        }}
                                    >
                                        Edit
                                    </Button>
                                    <Button
                                        color="error"
                                        size="small"
                                        onClick={() => deleteMutation.mutate(template.id)}
                                        disabled={deleteMutation.isPending}
                                    >
                                        Delete
                                    </Button>
                                </Stack>
                            </Stack>
                        ) : null}
                    </Stack>
                </Paper>
            ))}
        </Stack>
    )
}
