import AddRoundedIcon from '@mui/icons-material/AddRounded'
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded'
import EditRoundedIcon from '@mui/icons-material/EditRounded'
import PauseRoundedIcon from '@mui/icons-material/PauseRounded'
import PlayArrowRoundedIcon from '@mui/icons-material/PlayArrowRounded'
import SaveRoundedIcon from '@mui/icons-material/SaveRounded'
import SendRoundedIcon from '@mui/icons-material/SendRounded'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
    Alert,
    Box,
    Button,
    Checkbox,
    Chip,
    Divider,
    FormControlLabel,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useState } from 'react'

import { workflowsApi } from '../../workflow-builder/api/workflowsApi'
import { formsApi } from '../api/formsApi'
import type {
    FormDefinition,
    FormFieldInput,
    FormFieldType,
    FormInput,
    FormStatus,
    TaskPriority,
} from '../types/forms'

interface FormsPanelProps {
    tenantId: string
    projectId: string
    canManage: boolean
}

interface EditorState extends FormInput {
    id: string | null
    status: FormStatus
    definitionVersion: number
}

interface SubmissionDraft {
    formId: string
    values: Record<string, unknown>
}

const fieldTypes: FormFieldType[] = ['TEXT', 'TEXTAREA', 'NUMBER', 'DATE', 'BOOLEAN', 'SELECT']
const priorities: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']

function starterEditor(): EditorState {
    return {
        id: null,
        status: 'DRAFT',
        definitionVersion: 1,
        name: '',
        description: '',
        fields: [
            { key: 'title', label: 'Title', type: 'TEXT', required: true, options: [] },
            {
                key: 'description',
                label: 'Description',
                type: 'TEXTAREA',
                required: false,
                options: [],
            },
        ],
        taskTitleFieldKey: 'title',
        taskDescriptionFieldKey: 'description',
        taskDueDateFieldKey: null,
        taskPriority: 'MEDIUM',
        workflowId: null,
    }
}

function editorFromDefinition(form: FormDefinition): EditorState {
    return {
        id: form.id,
        status: form.status,
        definitionVersion: form.definitionVersion,
        name: form.name,
        description: form.description ?? '',
        fields: form.fields.map(({ key, label, type, required, options }) => ({
            key,
            label,
            type,
            required,
            options,
        })),
        taskTitleFieldKey: form.taskTitleFieldKey,
        taskDescriptionFieldKey: form.taskDescriptionFieldKey,
        taskDueDateFieldKey: form.taskDueDateFieldKey,
        taskPriority: form.taskPriority,
        workflowId: form.workflowId,
    }
}

function toInput(editor: EditorState): FormInput {
    return {
        name: editor.name.trim(),
        description: editor.description?.trim() || null,
        fields: editor.fields.map((field) => ({
            ...field,
            key: field.key.trim(),
            label: field.label.trim(),
            options:
                field.type === 'SELECT'
                    ? field.options.map((option) => option.trim()).filter(Boolean)
                    : [],
        })),
        taskTitleFieldKey: editor.taskTitleFieldKey,
        taskDescriptionFieldKey: editor.taskDescriptionFieldKey || null,
        taskDueDateFieldKey: editor.taskDueDateFieldKey || null,
        taskPriority: editor.taskPriority,
        workflowId: editor.workflowId || null,
    }
}

function statusColor(status: FormStatus): 'default' | 'success' | 'warning' {
    if (status === 'ACTIVE') return 'success'
    if (status === 'PAUSED') return 'warning'
    return 'default'
}

function submissionValue(field: FormFieldInput, raw: unknown): unknown {
    if (field.type === 'BOOLEAN') return Boolean(raw)
    if (field.type === 'NUMBER') return raw === null || raw === undefined ? '' : String(raw)
    return raw ?? ''
}

export function FormsPanel({ tenantId, projectId, canManage }: FormsPanelProps) {
    const queryClient = useQueryClient()
    const [selectedId, setSelectedId] = useState('')
    const [editing, setEditing] = useState(false)
    const [editor, setEditor] = useState<EditorState>(starterEditor)
    const [submissionDraft, setSubmissionDraft] = useState<SubmissionDraft>({
        formId: '',
        values: {},
    })

    const listQuery = useQuery({
        queryKey: ['forms', tenantId, projectId],
        queryFn: () => formsApi.list(tenantId, projectId),
        enabled: Boolean(tenantId && projectId),
    })
    const effectiveSelectedId =
        selectedId && listQuery.data?.content.some((form) => form.id === selectedId)
            ? selectedId
            : (listQuery.data?.content[0]?.id ?? '')
    const detailQuery = useQuery({
        queryKey: ['form', tenantId, projectId, effectiveSelectedId],
        queryFn: () => formsApi.get(tenantId, projectId, effectiveSelectedId),
        enabled: Boolean(effectiveSelectedId),
    })
    const historyQuery = useQuery({
        queryKey: ['form-submissions', tenantId, projectId, effectiveSelectedId],
        queryFn: () => formsApi.history(tenantId, projectId, effectiveSelectedId),
        enabled: Boolean(effectiveSelectedId),
    })
    const workflowsQuery = useQuery({
        queryKey: ['workflows', tenantId],
        queryFn: () => workflowsApi.list(tenantId),
        enabled: Boolean(tenantId),
    })

    const formWorkflows =
        workflowsQuery.data?.content.filter((workflow) =>
            workflow.nodes.some((node) => node.operation === 'TRIGGER_FORM_SUBMITTED'),
        ) ?? []
    const submissionValues =
        submissionDraft.formId === effectiveSelectedId ? submissionDraft.values : {}

    const updateSubmissionValue = (key: string, value: unknown) => {
        setSubmissionDraft((current) => ({
            formId: effectiveSelectedId,
            values: {
                ...(current.formId === effectiveSelectedId ? current.values : {}),
                [key]: value,
            },
        }))
    }

    const clearSubmissionDraft = (formId = effectiveSelectedId) => {
        setSubmissionDraft({ formId, values: {} })
    }

    const refresh = async (formId?: string) => {
        await queryClient.invalidateQueries({ queryKey: ['forms', tenantId, projectId] })
        if (formId) {
            await queryClient.invalidateQueries({ queryKey: ['form', tenantId, projectId, formId] })
        }
    }

    const saveMutation = useMutation({
        mutationFn: async () => {
            const input = toInput(editor)
            return editor.id
                ? formsApi.update(tenantId, projectId, editor.id, input)
                : formsApi.create(tenantId, projectId, input)
        },
        onSuccess: async (saved) => {
            setSelectedId(saved.id)
            setEditing(false)
            setEditor(editorFromDefinition(saved))
            clearSubmissionDraft(saved.id)
            await refresh(saved.id)
        },
    })
    const lifecycleMutation = useMutation({
        mutationFn: async ({ id, action }: { id: string; action: 'activate' | 'pause' }) =>
            action === 'activate'
                ? formsApi.activate(tenantId, projectId, id)
                : formsApi.pause(tenantId, projectId, id),
        onSuccess: async (updated) => {
            setSelectedId(updated.id)
            clearSubmissionDraft(updated.id)
            await refresh(updated.id)
        },
    })
    const submitMutation = useMutation({
        mutationFn: async () => {
            if (!detailQuery.data) throw new Error('Select a form first')
            const values: Record<string, unknown> = {}
            detailQuery.data.fields.forEach((field) => {
                const raw = submissionValues[field.key]
                if (raw !== '' && raw !== null && raw !== undefined) {
                    values[field.key] = submissionValue(field, raw)
                } else if (field.type === 'BOOLEAN' && field.required) {
                    values[field.key] = false
                }
            })
            return formsApi.submit(tenantId, projectId, detailQuery.data.id, values)
        },
        onSuccess: async () => {
            clearSubmissionDraft()
            await queryClient.invalidateQueries({
                queryKey: ['form-submissions', tenantId, projectId, effectiveSelectedId],
            })
        },
    })

    const beginCreate = () => {
        setEditor(starterEditor())
        setEditing(true)
    }
    const beginEdit = () => {
        if (!detailQuery.data) return
        setEditor(editorFromDefinition(detailQuery.data))
        setEditing(true)
    }
    const updateField = (index: number, patch: Partial<FormFieldInput>) => {
        setEditor((current) => ({
            ...current,
            fields: current.fields.map((field, fieldIndex) =>
                fieldIndex === index ? { ...field, ...patch } : field,
            ),
        }))
    }
    const addField = () => {
        setEditor((current) => {
            const sequence = current.fields.length + 1
            return {
                ...current,
                fields: [
                    ...current.fields,
                    {
                        key: `field_${sequence}`,
                        label: `Field ${sequence}`,
                        type: 'TEXT',
                        required: false,
                        options: [],
                    },
                ],
            }
        })
    }
    const removeField = (index: number) => {
        setEditor((current) => ({
            ...current,
            fields: current.fields.filter((_, fieldIndex) => fieldIndex !== index),
        }))
    }

    if (listQuery.isError) {
        return <Alert severity="error">Unable to load forms for this project.</Alert>
    }

    return (
        <Stack spacing={2.5}>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} sx={{ alignItems: 'start' }}>
                <TextField
                    select
                    label="Form"
                    value={effectiveSelectedId}
                    onChange={(event) => {
                        setSelectedId(event.target.value)
                        clearSubmissionDraft(event.target.value)
                    }}
                    disabled={!listQuery.data?.content.length}
                    sx={{ minWidth: 280 }}
                >
                    {listQuery.data?.content.map((form) => (
                        <MenuItem key={form.id} value={form.id}>
                            {form.name} · {form.status.toLowerCase()}
                        </MenuItem>
                    ))}
                </TextField>
                {canManage ? (
                    <Button startIcon={<AddRoundedIcon />} variant="outlined" onClick={beginCreate}>
                        New form
                    </Button>
                ) : null}
            </Stack>

            {!listQuery.isLoading && !listQuery.data?.content.length && !editing ? (
                <Alert severity="info">
                    No forms yet. Create an internal intake form to turn validated submissions into
                    normal project tasks.
                </Alert>
            ) : null}

            {editing ? (
                <Paper variant="outlined" sx={{ p: 2.5 }}>
                    <Stack spacing={2}>
                        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                            <Typography variant="h6">
                                {editor.id ? `Edit ${editor.name || 'form'}` : 'Create form'}
                            </Typography>
                            {editor.id ? (
                                <Chip
                                    size="small"
                                    label={`v${editor.definitionVersion} · ${editor.status}`}
                                />
                            ) : null}
                        </Stack>
                        <TextField
                            label="Name"
                            value={editor.name}
                            onChange={(event) =>
                                setEditor((current) => ({ ...current, name: event.target.value }))
                            }
                            slotProps={{ htmlInput: { maxLength: 100 } }}
                        />
                        <TextField
                            label="Description"
                            value={editor.description ?? ''}
                            multiline
                            minRows={2}
                            onChange={(event) =>
                                setEditor((current) => ({
                                    ...current,
                                    description: event.target.value,
                                }))
                            }
                            slotProps={{ htmlInput: { maxLength: 1000 } }}
                        />

                        <Divider />
                        <Stack spacing={1.5}>
                            <Stack
                                direction="row"
                                sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                            >
                                <Typography variant="subtitle1">
                                    Fields ({editor.fields.length}/30)
                                </Typography>
                                <Button
                                    size="small"
                                    startIcon={<AddRoundedIcon />}
                                    onClick={addField}
                                    disabled={editor.fields.length >= 30}
                                >
                                    Add field
                                </Button>
                            </Stack>
                            {editor.fields.map((field, index) => (
                                <Paper
                                    key={`${index}-${field.key}`}
                                    variant="outlined"
                                    sx={{ p: 1.5 }}
                                >
                                    <Stack spacing={1.5}>
                                        <Stack
                                            direction={{ xs: 'column', md: 'row' }}
                                            spacing={1.5}
                                        >
                                            <TextField
                                                label="Key"
                                                value={field.key}
                                                onChange={(event) =>
                                                    updateField(index, { key: event.target.value })
                                                }
                                                sx={{ flex: 1 }}
                                            />
                                            <TextField
                                                label="Label"
                                                value={field.label}
                                                onChange={(event) =>
                                                    updateField(index, {
                                                        label: event.target.value,
                                                    })
                                                }
                                                sx={{ flex: 1 }}
                                            />
                                            <TextField
                                                select
                                                label="Type"
                                                value={field.type}
                                                onChange={(event) =>
                                                    updateField(index, {
                                                        type: event.target.value as FormFieldType,
                                                        options:
                                                            event.target.value === 'SELECT'
                                                                ? field.options
                                                                : [],
                                                    })
                                                }
                                                sx={{ minWidth: 150 }}
                                            >
                                                {fieldTypes.map((type) => (
                                                    <MenuItem key={type} value={type}>
                                                        {type.toLowerCase()}
                                                    </MenuItem>
                                                ))}
                                            </TextField>
                                        </Stack>
                                        <Stack
                                            direction="row"
                                            spacing={2}
                                            sx={{ alignItems: 'center' }}
                                        >
                                            <FormControlLabel
                                                control={
                                                    <Checkbox
                                                        checked={field.required}
                                                        onChange={(event) =>
                                                            updateField(index, {
                                                                required: event.target.checked,
                                                            })
                                                        }
                                                    />
                                                }
                                                label="Required"
                                            />
                                            <Button
                                                color="error"
                                                size="small"
                                                startIcon={<DeleteOutlineRoundedIcon />}
                                                onClick={() => removeField(index)}
                                                disabled={editor.fields.length <= 1}
                                            >
                                                Remove
                                            </Button>
                                        </Stack>
                                        {field.type === 'SELECT' ? (
                                            <TextField
                                                label="Options (comma separated)"
                                                value={field.options.join(', ')}
                                                onChange={(event) =>
                                                    updateField(index, {
                                                        options: event.target.value
                                                            .split(',')
                                                            .map((value) => value.trim()),
                                                    })
                                                }
                                                helperText="1–50 unique options; each option is limited to 100 characters."
                                            />
                                        ) : null}
                                    </Stack>
                                </Paper>
                            ))}
                        </Stack>

                        <Divider />
                        <Typography variant="subtitle1">Created task mapping</Typography>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                            <TextField
                                select
                                label="Task title field"
                                value={editor.taskTitleFieldKey}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        taskTitleFieldKey: event.target.value,
                                    }))
                                }
                                sx={{ flex: 1 }}
                            >
                                {editor.fields
                                    .filter(
                                        (field) => field.type === 'TEXT' || field.type === 'SELECT',
                                    )
                                    .map((field) => (
                                        <MenuItem key={field.key} value={field.key}>
                                            {field.label || field.key}
                                        </MenuItem>
                                    ))}
                            </TextField>
                            <TextField
                                select
                                label="Task description field"
                                value={editor.taskDescriptionFieldKey ?? ''}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        taskDescriptionFieldKey: event.target.value || null,
                                    }))
                                }
                                sx={{ flex: 1 }}
                            >
                                <MenuItem value="">None</MenuItem>
                                {editor.fields
                                    .filter(
                                        (field) =>
                                            field.type === 'TEXT' || field.type === 'TEXTAREA',
                                    )
                                    .map((field) => (
                                        <MenuItem key={field.key} value={field.key}>
                                            {field.label || field.key}
                                        </MenuItem>
                                    ))}
                            </TextField>
                            <TextField
                                select
                                label="Task due-date field"
                                value={editor.taskDueDateFieldKey ?? ''}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        taskDueDateFieldKey: event.target.value || null,
                                    }))
                                }
                                sx={{ flex: 1 }}
                            >
                                <MenuItem value="">None</MenuItem>
                                {editor.fields
                                    .filter((field) => field.type === 'DATE')
                                    .map((field) => (
                                        <MenuItem key={field.key} value={field.key}>
                                            {field.label || field.key}
                                        </MenuItem>
                                    ))}
                            </TextField>
                        </Stack>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5}>
                            <TextField
                                select
                                label="Task priority"
                                value={editor.taskPriority}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        taskPriority: event.target.value as TaskPriority,
                                    }))
                                }
                                sx={{ minWidth: 220 }}
                            >
                                {priorities.map((priority) => (
                                    <MenuItem key={priority} value={priority}>
                                        {priority}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <TextField
                                select
                                label="Optional workflow"
                                value={editor.workflowId ?? ''}
                                onChange={(event) =>
                                    setEditor((current) => ({
                                        ...current,
                                        workflowId: event.target.value || null,
                                    }))
                                }
                                sx={{ flex: 1 }}
                                helperText="Only workflows whose trigger is Form submitted are eligible."
                            >
                                <MenuItem value="">None</MenuItem>
                                {formWorkflows.map((workflow) => (
                                    <MenuItem key={workflow.id} value={workflow.id}>
                                        {workflow.name} · {workflow.status.toLowerCase()}
                                    </MenuItem>
                                ))}
                            </TextField>
                        </Stack>
                        {saveMutation.isError ? (
                            <Alert severity="error">Unable to save this form definition.</Alert>
                        ) : null}
                        <Stack direction="row" spacing={1}>
                            <Button
                                variant="contained"
                                startIcon={<SaveRoundedIcon />}
                                onClick={() => saveMutation.mutate()}
                                disabled={
                                    !canManage || saveMutation.isPending || !editor.name.trim()
                                }
                            >
                                Save definition
                            </Button>
                            <Button onClick={() => setEditing(false)}>Cancel</Button>
                        </Stack>
                    </Stack>
                </Paper>
            ) : null}

            {detailQuery.data && !editing ? (
                <Paper variant="outlined" sx={{ p: 2.5 }}>
                    <Stack spacing={2}>
                        <Stack
                            direction={{ xs: 'column', md: 'row' }}
                            spacing={1}
                            sx={{ alignItems: { md: 'center' }, justifyContent: 'space-between' }}
                        >
                            <Box>
                                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                                    <Typography variant="h6">{detailQuery.data.name}</Typography>
                                    <Chip
                                        size="small"
                                        color={statusColor(detailQuery.data.status)}
                                        label={`${detailQuery.data.status} · v${detailQuery.data.definitionVersion}`}
                                    />
                                </Stack>
                                {detailQuery.data.description ? (
                                    <Typography color="text.secondary">
                                        {detailQuery.data.description}
                                    </Typography>
                                ) : null}
                            </Box>
                            {canManage ? (
                                <Stack direction="row" spacing={1}>
                                    {detailQuery.data.status !== 'ACTIVE' ? (
                                        <Button
                                            startIcon={<EditRoundedIcon />}
                                            onClick={beginEdit}
                                            variant="outlined"
                                        >
                                            Edit
                                        </Button>
                                    ) : null}
                                    {detailQuery.data.status === 'ACTIVE' ? (
                                        <Button
                                            startIcon={<PauseRoundedIcon />}
                                            onClick={() =>
                                                lifecycleMutation.mutate({
                                                    id: detailQuery.data.id,
                                                    action: 'pause',
                                                })
                                            }
                                        >
                                            Pause
                                        </Button>
                                    ) : (
                                        <Button
                                            startIcon={<PlayArrowRoundedIcon />}
                                            onClick={() =>
                                                lifecycleMutation.mutate({
                                                    id: detailQuery.data.id,
                                                    action: 'activate',
                                                })
                                            }
                                        >
                                            Activate
                                        </Button>
                                    )}
                                </Stack>
                            ) : null}
                        </Stack>

                        <Divider />
                        <Typography variant="subtitle1">Internal submission</Typography>
                        {detailQuery.data.status !== 'ACTIVE' ? (
                            <Alert severity="info">
                                Activate the form before accepting submissions.
                            </Alert>
                        ) : (
                            <Stack spacing={1.5}>
                                {detailQuery.data.fields.map((field) =>
                                    field.type === 'BOOLEAN' ? (
                                        <FormControlLabel
                                            key={field.key}
                                            control={
                                                <Checkbox
                                                    checked={Boolean(submissionValues[field.key])}
                                                    onChange={(event) =>
                                                        updateSubmissionValue(
                                                            field.key,
                                                            event.target.checked,
                                                        )
                                                    }
                                                />
                                            }
                                            label={`${field.label}${field.required ? ' *' : ''}`}
                                        />
                                    ) : (
                                        <TextField
                                            key={field.key}
                                            select={field.type === 'SELECT'}
                                            type={
                                                field.type === 'NUMBER'
                                                    ? 'number'
                                                    : field.type === 'DATE'
                                                      ? 'date'
                                                      : 'text'
                                            }
                                            label={`${field.label}${field.required ? ' *' : ''}`}
                                            value={
                                                (submissionValues[field.key] as
                                                    string | number | undefined) ?? ''
                                            }
                                            onChange={(event) =>
                                                updateSubmissionValue(field.key, event.target.value)
                                            }
                                            multiline={field.type === 'TEXTAREA'}
                                            minRows={field.type === 'TEXTAREA' ? 3 : undefined}
                                            slotProps={
                                                field.type === 'DATE'
                                                    ? { inputLabel: { shrink: true } }
                                                    : undefined
                                            }
                                        >
                                            {field.type === 'SELECT'
                                                ? field.options.map((option) => (
                                                      <MenuItem key={option} value={option}>
                                                          {option}
                                                      </MenuItem>
                                                  ))
                                                : undefined}
                                        </TextField>
                                    ),
                                )}
                                {submitMutation.isError ? (
                                    <Alert severity="error">
                                        Submission failed validation or task creation.
                                    </Alert>
                                ) : null}
                                {submitMutation.isSuccess ? (
                                    <Alert severity="success">
                                        Submission accepted and created task{' '}
                                        {submitMutation.data.createdTaskId.slice(0, 8)}.
                                    </Alert>
                                ) : null}
                                <Button
                                    variant="contained"
                                    startIcon={<SendRoundedIcon />}
                                    onClick={() => submitMutation.mutate()}
                                    disabled={!canManage || submitMutation.isPending}
                                    sx={{ alignSelf: 'start' }}
                                >
                                    Submit and create task
                                </Button>
                            </Stack>
                        )}

                        <Divider />
                        <Typography variant="subtitle1">Recent submissions</Typography>
                        {historyQuery.data?.content.length ? (
                            <Stack spacing={1}>
                                {historyQuery.data.content.map((submission) => (
                                    <Paper key={submission.id} variant="outlined" sx={{ p: 1.5 }}>
                                        <Typography variant="body2">
                                            {new Date(submission.submittedAt).toLocaleString()} ·
                                            form v{submission.definitionVersion} · task{' '}
                                            {submission.createdTaskId.slice(0, 8)}
                                        </Typography>
                                        <Typography variant="caption" color="text.secondary">
                                            {submission.validationContext}
                                        </Typography>
                                    </Paper>
                                ))}
                            </Stack>
                        ) : (
                            <Typography color="text.secondary">
                                No submissions recorded yet.
                            </Typography>
                        )}
                    </Stack>
                </Paper>
            ) : null}
        </Stack>
    )
}
