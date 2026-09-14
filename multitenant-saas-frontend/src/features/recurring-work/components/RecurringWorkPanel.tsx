import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
    Alert,
    Button,
    Chip,
    Divider,
    MenuItem,
    Paper,
    Stack,
    TextField,
    Typography,
} from '@mui/material'
import { useMemo, useState } from 'react'

import type { ProjectTaskPriority } from '../../projects/types/projectTasks'
import { recurringWorkApi } from '../api/recurringWorkApi'
import type {
    RecurrenceCadence,
    RecurringWorkInput,
    RecurringWorkRule,
} from '../types/recurringWork'

interface RecurringWorkPanelProps {
    tenantId: string
    projectId: string
    canManage: boolean
}

interface RuleFormState {
    title: string
    description: string
    priority: ProjectTaskPriority
    cadence: RecurrenceCadence
    intervalCount: string
    zoneId: string
    nextOccurrenceAt: string
    dueOffsetMinutes: string
    maxOccurrences: string
}

const defaultForm = (): RuleFormState => ({
    title: '',
    description: '',
    priority: 'MEDIUM',
    cadence: 'WEEKLY',
    intervalCount: '1',
    zoneId: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
    nextOccurrenceAt: '',
    dueOffsetMinutes: '',
    maxOccurrences: '',
})

function toLocalInputValue(instant: string): string {
    const date = new Date(instant)
    const offset = date.getTimezoneOffset() * 60_000
    return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

function toInput(form: RuleFormState): RecurringWorkInput {
    return {
        title: form.title.trim(),
        description: form.description.trim() || null,
        priority: form.priority,
        assigneeUserId: null,
        cadence: form.cadence,
        intervalCount: Number(form.intervalCount),
        zoneId: form.zoneId.trim(),
        nextOccurrenceAt: new Date(form.nextOccurrenceAt).toISOString(),
        dueOffsetMinutes: form.dueOffsetMinutes ? Number(form.dueOffsetMinutes) : null,
        endAt: null,
        maxOccurrences: form.maxOccurrences ? Number(form.maxOccurrences) : null,
    }
}

function formFromRule(rule: RecurringWorkRule): RuleFormState {
    return {
        title: rule.title,
        description: rule.description ?? '',
        priority: rule.priority,
        cadence: rule.cadence,
        intervalCount: String(rule.intervalCount),
        zoneId: rule.zoneId,
        nextOccurrenceAt: toLocalInputValue(rule.nextOccurrenceAt),
        dueOffsetMinutes: rule.dueOffsetMinutes === null ? '' : String(rule.dueOffsetMinutes),
        maxOccurrences: rule.maxOccurrences === null ? '' : String(rule.maxOccurrences),
    }
}

export function RecurringWorkPanel({ tenantId, projectId, canManage }: RecurringWorkPanelProps) {
    const queryClient = useQueryClient()
    const [form, setForm] = useState<RuleFormState>(defaultForm)
    const [editingId, setEditingId] = useState<string | null>(null)
    const [historyId, setHistoryId] = useState<string | null>(null)

    const rulesKey = useMemo(
        () => ['recurring-work', tenantId, projectId] as const,
        [tenantId, projectId],
    )
    const rulesQuery = useQuery({
        queryKey: rulesKey,
        queryFn: () => recurringWorkApi.list(tenantId, projectId),
        enabled: projectId.length > 0,
    })
    const historyQuery = useQuery({
        queryKey: ['recurring-work-occurrences', tenantId, projectId, historyId],
        queryFn: () => recurringWorkApi.occurrences(tenantId, projectId, historyId ?? ''),
        enabled: historyId !== null,
    })

    const invalidate = async () => queryClient.invalidateQueries({ queryKey: rulesKey })
    const saveMutation = useMutation({
        mutationFn: (input: RecurringWorkInput) =>
            editingId
                ? recurringWorkApi.update(tenantId, projectId, editingId, input)
                : recurringWorkApi.create(tenantId, projectId, input),
        onSuccess: async () => {
            setEditingId(null)
            setForm(defaultForm())
            await invalidate()
        },
    })
    const pauseMutation = useMutation({
        mutationFn: (id: string) => recurringWorkApi.pause(tenantId, projectId, id),
        onSuccess: invalidate,
    })
    const resumeMutation = useMutation({
        mutationFn: (id: string) => recurringWorkApi.resume(tenantId, projectId, id),
        onSuccess: invalidate,
    })

    const submit = () => {
        if (!form.title.trim() || !form.zoneId.trim() || !form.nextOccurrenceAt) return
        saveMutation.mutate(toInput(form))
    }

    return (
        <Stack spacing={2}>
            <Stack spacing={0.5}>
                <Typography variant="h6">Recurring work</Typography>
                <Typography color="text.secondary" variant="body2">
                    Materialize project tasks on a timezone-aware daily, weekly, or monthly cadence.
                </Typography>
            </Stack>

            {canManage ? (
                <Paper variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={2}>
                        <Typography sx={{ fontWeight: 600 }}>
                            {editingId ? 'Edit recurring rule' : 'New recurring rule'}
                        </Typography>
                        <TextField
                            label="Task title"
                            value={form.title}
                            onChange={(event) => setForm({ ...form, title: event.target.value })}
                            slotProps={{ htmlInput: { maxLength: 200 } }}
                            required
                        />
                        <TextField
                            label="Description"
                            value={form.description}
                            onChange={(event) =>
                                setForm({ ...form, description: event.target.value })
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
                                select
                                fullWidth
                                label="Cadence"
                                value={form.cadence}
                                onChange={(event) =>
                                    setForm({
                                        ...form,
                                        cadence: event.target.value as RecurrenceCadence,
                                    })
                                }
                            >
                                {['DAILY', 'WEEKLY', 'MONTHLY'].map((cadence) => (
                                    <MenuItem key={cadence} value={cadence}>
                                        {cadence}
                                    </MenuItem>
                                ))}
                            </TextField>
                            <TextField
                                fullWidth
                                label="Every"
                                type="number"
                                value={form.intervalCount}
                                onChange={(event) =>
                                    setForm({ ...form, intervalCount: event.target.value })
                                }
                                slotProps={{ htmlInput: { min: 1, max: 52 } }}
                            />
                        </Stack>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                            <TextField
                                fullWidth
                                label="Timezone"
                                value={form.zoneId}
                                onChange={(event) =>
                                    setForm({ ...form, zoneId: event.target.value })
                                }
                            />
                            <TextField
                                fullWidth
                                label="Next occurrence"
                                type="datetime-local"
                                value={form.nextOccurrenceAt}
                                onChange={(event) =>
                                    setForm({ ...form, nextOccurrenceAt: event.target.value })
                                }
                                slotProps={{ inputLabel: { shrink: true } }}
                            />
                        </Stack>
                        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
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
                            <TextField
                                fullWidth
                                label="Maximum occurrences"
                                type="number"
                                value={form.maxOccurrences}
                                onChange={(event) =>
                                    setForm({ ...form, maxOccurrences: event.target.value })
                                }
                                slotProps={{ htmlInput: { min: 1, max: 10000 } }}
                            />
                        </Stack>
                        {saveMutation.isError ? (
                            <Alert severity="error">Unable to save recurring work.</Alert>
                        ) : null}
                        <Stack direction="row" spacing={1}>
                            <Button
                                variant="contained"
                                onClick={submit}
                                disabled={saveMutation.isPending}
                            >
                                {editingId ? 'Save changes' : 'Create rule'}
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
                <Alert severity="info">You can view recurring work but cannot modify it.</Alert>
            )}

            {rulesQuery.isError ? (
                <Alert severity="error">Unable to load recurring work.</Alert>
            ) : null}
            {rulesQuery.data?.content.length === 0 ? (
                <Alert severity="info">No recurring rules exist for this project.</Alert>
            ) : null}
            {rulesQuery.data?.content.map((rule) => (
                <Paper key={rule.id} variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={1.5}>
                        <Stack
                            direction={{ xs: 'column', sm: 'row' }}
                            spacing={1}
                            sx={{ justifyContent: 'space-between' }}
                        >
                            <Stack>
                                <Typography sx={{ fontWeight: 600 }}>{rule.title}</Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Every {rule.intervalCount} {rule.cadence.toLowerCase()} · next{' '}
                                    {new Date(rule.nextOccurrenceAt).toLocaleString()}
                                </Typography>
                            </Stack>
                            <Stack direction="row" spacing={1}>
                                <Chip label={rule.status} size="small" />
                                <Chip
                                    label={`${rule.generatedCount} generated`}
                                    size="small"
                                    variant="outlined"
                                />
                            </Stack>
                        </Stack>
                        {rule.description ? (
                            <Typography variant="body2">{rule.description}</Typography>
                        ) : null}
                        {rule.lastError ? <Alert severity="warning">{rule.lastError}</Alert> : null}
                        <Stack direction="row" spacing={1} useFlexGap sx={{ flexWrap: 'wrap' }}>
                            <Button size="small" onClick={() => setHistoryId(rule.id)}>
                                History
                            </Button>
                            {canManage ? (
                                <Button
                                    size="small"
                                    onClick={() => {
                                        setEditingId(rule.id)
                                        setForm(formFromRule(rule))
                                    }}
                                >
                                    Edit
                                </Button>
                            ) : null}
                            {canManage && rule.status === 'ACTIVE' ? (
                                <Button size="small" onClick={() => pauseMutation.mutate(rule.id)}>
                                    Pause
                                </Button>
                            ) : null}
                            {canManage && rule.status === 'PAUSED' ? (
                                <Button size="small" onClick={() => resumeMutation.mutate(rule.id)}>
                                    Resume
                                </Button>
                            ) : null}
                        </Stack>
                    </Stack>
                </Paper>
            ))}

            {historyId ? (
                <Paper variant="outlined" sx={{ p: 2 }}>
                    <Stack spacing={1.5}>
                        <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
                            <Typography sx={{ fontWeight: 600 }}>Occurrence history</Typography>
                            <Button size="small" onClick={() => setHistoryId(null)}>
                                Close
                            </Button>
                        </Stack>
                        <Divider />
                        {historyQuery.data?.content.length === 0 ? (
                            <Typography color="text.secondary" variant="body2">
                                No materialized occurrences yet.
                            </Typography>
                        ) : null}
                        {historyQuery.data?.content.map((occurrence) => (
                            <Stack key={occurrence.id} direction="row" spacing={2}>
                                <Typography variant="body2">
                                    {new Date(occurrence.scheduledFor).toLocaleString()}
                                </Typography>
                                <Typography color="text.secondary" variant="body2">
                                    Task {occurrence.taskId}
                                </Typography>
                            </Stack>
                        ))}
                    </Stack>
                </Paper>
            ) : null}
        </Stack>
    )
}
