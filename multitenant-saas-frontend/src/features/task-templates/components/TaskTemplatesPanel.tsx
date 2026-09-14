import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Alert, Button, Chip, MenuItem, Paper, Stack, TextField, Typography } from '@mui/material'
import { useMemo, useState } from 'react'

import type { ProjectTaskPriority } from '../../projects/types/projectTasks'
import { taskTemplatesApi } from '../api/taskTemplatesApi'
import type { TaskTemplate, TaskTemplateInput } from '../types/taskTemplates'

interface TaskTemplatesPanelProps {
    tenantId: string
    projectId: string
    canManage: boolean
}

interface TemplateFormState {
    name: string
    taskTitle: string
    taskDescription: string
    priority: ProjectTaskPriority
    dueOffsetMinutes: string
}

const defaultForm = (): TemplateFormState => ({
    name: '',
    taskTitle: '',
    taskDescription: '',
    priority: 'MEDIUM',
    dueOffsetMinutes: '',
})

function toInput(form: TemplateFormState): TaskTemplateInput {
    return {
        name: form.name.trim(),
        taskTitle: form.taskTitle.trim(),
        taskDescription: form.taskDescription.trim() || null,
        priority: form.priority,
        assigneeUserId: null,
        dueOffsetMinutes: form.dueOffsetMinutes ? Number(form.dueOffsetMinutes) : null,
    }
}

function formFromTemplate(template: TaskTemplate): TemplateFormState {
    return {
        name: template.name,
        taskTitle: template.taskTitle,
        taskDescription: template.taskDescription ?? '',
        priority: template.priority,
        dueOffsetMinutes:
            template.dueOffsetMinutes === null ? '' : String(template.dueOffsetMinutes),
    }
}

export function TaskTemplatesPanel({ tenantId, projectId, canManage }: TaskTemplatesPanelProps) {
    const queryClient = useQueryClient()
    const [form, setForm] = useState<TemplateFormState>(defaultForm)
    const [editingId, setEditingId] = useState<string | null>(null)
    const [lastCreatedTaskId, setLastCreatedTaskId] = useState<string | null>(null)
    const templatesKey = useMemo(
        () => ['task-templates', tenantId, projectId] as const,
        [tenantId, projectId],
    )

    const templatesQuery = useQuery({
        queryKey: templatesKey,
        queryFn: () => taskTemplatesApi.list(tenantId, projectId),
        enabled: projectId.length > 0,
    })
    const invalidate = async () => queryClient.invalidateQueries({ queryKey: templatesKey })
    const saveMutation = useMutation({
        mutationFn: (input: TaskTemplateInput) =>
            editingId
                ? taskTemplatesApi.update(tenantId, projectId, editingId, input)
                : taskTemplatesApi.create(tenantId, projectId, input),
        onSuccess: async () => {
            setEditingId(null)
            setForm(defaultForm())
            await invalidate()
        },
    })
    const deleteMutation = useMutation({
        mutationFn: (templateId: string) =>
            taskTemplatesApi.remove(tenantId, projectId, templateId),
        onSuccess: invalidate,
    })
    const instantiateMutation = useMutation({
        mutationFn: (templateId: string) =>
            taskTemplatesApi.instantiate(tenantId, projectId, templateId),
        onSuccess: (result) => setLastCreatedTaskId(result.taskId),
    })

    const submit = () => {
        if (!form.name.trim() || !form.taskTitle.trim()) return
        saveMutation.mutate(toInput(form))
    }

    return (
        <Stack spacing={2}>
            <Stack spacing={0.5}>
                <Typography variant="h6">Task templates</Typography>
                <Typography color="text.secondary" variant="body2">
                    Store reusable task snapshots inside this project and instantiate them through
                    the normal task lifecycle.
                </Typography>
            </Stack>

            {canManage ? (
                <Paper variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={2}>
                        <Typography sx={{ fontWeight: 600 }}>
                            {editingId ? 'Edit task template' : 'New task template'}
                        </Typography>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                            <TextField
                                fullWidth
                                label="Template name"
                                value={form.name}
                                onChange={(event) => setForm({ ...form, name: event.target.value })}
                                slotProps={{ htmlInput: { maxLength: 80 } }}
                                required
                            />
                            <TextField
                                fullWidth
                                label="Task title"
                                value={form.taskTitle}
                                onChange={(event) =>
                                    setForm({ ...form, taskTitle: event.target.value })
                                }
                                slotProps={{ htmlInput: { maxLength: 200 } }}
                                required
                            />
                        </Stack>
                        <TextField
                            label="Task description"
                            value={form.taskDescription}
                            onChange={(event) =>
                                setForm({ ...form, taskDescription: event.target.value })
                            }
                            multiline
                            minRows={2}
                            slotProps={{ htmlInput: { maxLength: 4000 } }}
                        />
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                            <TextField
                                select
                                fullWidth
                                label="Priority"
                                value={form.priority}
                                onChange={(event) =>
                                    setForm({
                                        ...form,
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
                                value={form.dueOffsetMinutes}
                                onChange={(event) =>
                                    setForm({ ...form, dueOffsetMinutes: event.target.value })
                                }
                                slotProps={{ htmlInput: { min: 0, max: 525600 } }}
                            />
                        </Stack>
                        {saveMutation.isError ? (
                            <Alert severity="error">Unable to save task template.</Alert>
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
                <Alert severity="info">You can view task templates but cannot modify them.</Alert>
            )}

            {lastCreatedTaskId ? (
                <Alert severity="success">Created task {lastCreatedTaskId} from template.</Alert>
            ) : null}
            {templatesQuery.isError ? (
                <Alert severity="error">Unable to load task templates.</Alert>
            ) : null}
            {templatesQuery.data?.content.length === 0 ? (
                <Alert severity="info">No task templates exist for this project.</Alert>
            ) : null}

            {templatesQuery.data?.content.map((template) => (
                <Paper key={template.id} variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={1.25}>
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={1}
                            sx={{ justifyContent: 'space-between' }}
                        >
                            <Stack>
                                <Typography sx={{ fontWeight: 600 }}>{template.name}</Typography>
                                <Typography variant="body2">{template.taskTitle}</Typography>
                            </Stack>
                            <Stack direction="row" spacing={1}>
                                <Chip label={template.priority} size="small" />
                                {template.dueOffsetMinutes !== null ? (
                                    <Chip
                                        label={`Due +${template.dueOffsetMinutes} min`}
                                        size="small"
                                        variant="outlined"
                                    />
                                ) : null}
                            </Stack>
                        </Stack>
                        {template.taskDescription ? (
                            <Typography color="text.secondary" variant="body2">
                                {template.taskDescription}
                            </Typography>
                        ) : null}
                        {canManage ? (
                            <Stack
                                direction="row"
                                spacing={1}
                                useFlexGap
                                sx={{ flexWrap: 'wrap' }}
                            >
                                <Button
                                    size="small"
                                    variant="contained"
                                    onClick={() => instantiateMutation.mutate(template.id)}
                                    disabled={instantiateMutation.isPending}
                                >
                                    Create task
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
                        ) : null}
                    </Stack>
                </Paper>
            ))}
        </Stack>
    )
}
